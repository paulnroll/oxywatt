"""Strict developer FIT projection. Real ride exports must remain under fixtures/private."""

import argparse
import hashlib
import io
import json
import math
import struct
from datetime import datetime
from importlib.metadata import version
from pathlib import Path

import fitdecode
from verify_fit_replay import synthetic_fit

ROOT = Path(__file__).resolve().parents[1]
PRIVATE = ROOT / "fixtures/private"
COLUMNS = "message_index\tfile_timestamp\toffset_nanos\tkind\tvalue\treason\ttimer_type\n"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def decode(data, provenance):
    """No sorting, deduplication, timestamp repair, GPS or unrelated profile export."""
    if version("fitdecode") != "0.11.0":
        raise ValueError("fitdecode==0.11.0 is required")
    if provenance not in ("private-owner-input", "generated-synthetic-v1"):
        raise ValueError("Explicit provenance is required")
    if len(data) < 14 or data[0] not in (12, 14) or data[8:12] != b".FIT":
        raise ValueError("Invalid FIT header")
    if len(data) != data[0] + struct.unpack_from("<I", data, 4)[0] + 2:
        raise ValueError("Truncated, trailing or concatenated FIT data")
    rows = []
    timers = []
    origin = previous = None
    first_kind = None
    records = 0
    with fitdecode.FitReader(
        io.BytesIO(data), check_crc=fitdecode.CrcCheck.RAISE,
        error_handling=fitdecode.ErrorHandling.RAISE,
    ) as reader:
        for message_index, message in enumerate(reader):
            if not isinstance(message, fitdecode.FitDataMessage):
                continue
            timer = message.name == "event" and message.get_value("event", fallback=None) == "timer"
            if message.name != "record" and not timer:
                continue
            stamp = message.get_value("timestamp", fallback=None)
            if not isinstance(stamp, datetime) or stamp.tzinfo is None:
                raise ValueError(f"Missing/invalid timestamp at message {message_index}")
            if previous is not None and stamp < previous:
                raise ValueError(f"Regressing timestamp at message {message_index}")
            if origin is None:
                origin = stamp
                first_kind = "timer" if timer else "record"
            previous = stamp
            delta = stamp - origin
            offset = ((delta.days * 86400 + delta.seconds) * 1_000_000 + delta.microseconds) * 1000
            prefix = (str(message_index), stamp.isoformat(), str(offset))
            if timer:
                event_type = message.get_value("event_type", fallback=None)
                state = {"start": "RECORDING", "stop": "PAUSED", "stop_all": "PAUSED"}.get(event_type)
                if state is None:
                    raise ValueError(f"Unsupported timer type at message {message_index}")
                rows.append(prefix + ("STATE", state, "-", event_type))
                timers.append({"message_index": message_index, "file_timestamp": stamp.isoformat(), "offset_nanos": offset, "event_type": event_type, "timer_trigger": message.get_value("timer_trigger", fallback=None)})
            else:
                records += 1
                for field, metric in (("power", "POWER_W"), ("heart_rate", "HEART_RATE_BPM"), ("cadence", "CADENCE_RPM")):
                    value = message.get_value(field, fallback=None)
                    reason = "-"
                    if not message.has_field(field):
                        value, reason = None, "UNAVAILABLE"
                    elif not isinstance(value, (int, float)) or not math.isfinite(value) or value < 0 or (field == "heart_rate" and value == 0):
                        value, reason = None, "INVALID_SOURCE_VALUE"
                    rows.append(prefix + (metric, "-" if value is None else str(value), reason, "-"))
    if not rows:
        raise ValueError("No timestamped record/timer input")
    projection = COLUMNS + "".join("\t".join(row) + "\n" for row in rows)
    diagnostics = ["native_saved_or_discarded_unknown"]
    if not timers or first_kind != "timer" or timers[0]["event_type"] != "start":
        diagnostics.append("initial_timer_start_not_observed")
    if not timers or timers[-1]["event_type"] == "start":
        diagnostics.append("final_timer_stop_not_observed")
    manifest = {
        "schema": "fit-replay-v1", "decoder": "fitdecode", "decoder_version": "0.11.0",
        "normalization_version": "fit-normalization-v1", "provenance": provenance,
        "input_sha256": sha256(data), "projection_sha256": sha256(projection.encode("utf-8")),
        "timestamp_policy": "ordered-original-file-time-v1", "scheduler_policy": "timers-before-input-v1",
        "file_origin": origin.isoformat(), "end_offset_nanos": int(rows[-1][2]),
        "projected_rows": len(rows), "record_messages": records, "timer_events": timers,
        "diagnostics": diagnostics,
    }
    return projection, manifest


def export(data, provenance, directory):
    directory = directory.resolve()
    if provenance == "private-owner-input" and not directory.is_relative_to(PRIVATE.resolve()):
        raise ValueError("Private FIT projections must stay under fixtures/private")
    projection, manifest = decode(data, provenance)
    directory.mkdir(parents=True, exist_ok=True)
    # Decode fully before publishing either file; manifest hash also detects mismatched pairs.
    for name, content in (("observations.tsv", projection), ("manifest.json", json.dumps(manifest, sort_keys=True, indent=2) + "\n")):
        temporary = directory / (name + ".tmp")
        temporary.write_bytes(content.encode("utf-8"))
        temporary.replace(directory / name)
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--input", type=Path)
    source.add_argument("--synthetic", action="store_true")
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    try:
        data = args.input.read_bytes() if args.input else synthetic_fit()
        manifest = export(data, "private-owner-input" if args.input else "generated-synthetic-v1", args.output_dir)
    except (ValueError, OSError, fitdecode.FitError):
        # Decoder exceptions may contain private values: keep CLI errors sanitized.
        raise SystemExit("FIT export rejected; verify integrity, timestamp policy and private output directory") from None
    print(f"Verified {manifest['record_messages']} records, {len(manifest['timer_events'])} timer events and {manifest['projected_rows']} projected rows")


if __name__ == "__main__":
    main()
