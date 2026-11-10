"""Generate and verify the synthetic FIT projection used by the JVM replay tests."""

import argparse
import io
import struct
from importlib.metadata import version
from pathlib import Path

import fitdecode

OUTPUT = Path(__file__).resolve().parents[1] / "core/src/test/resources/fit-replay.tsv"


def definition(local, global_number, fields):
    return struct.pack("<BBBHB", 0x40 | local, 0, 0, global_number, len(fields)) + bytes(
        component for field in fields for component in field
    )


def crc16(data):
    crc = 0
    for byte in data:
        crc ^= byte
        for _ in range(8):
            crc = (crc >> 1) ^ (0xA001 if crc & 1 else 0)
    return crc


def synthetic_fit():
    # FIT seconds since 1989-12-31. No location, device or rider fields.
    start = 1_136_073_600
    body = definition(0, 21, [(253, 4, 0x86), (0, 1, 0), (1, 1, 0)])
    body += definition(1, 20, [(253, 4, 0x86), (7, 2, 0x84), (3, 1, 2), (4, 1, 2)])
    body += definition(2, 20, [(253, 4, 0x86), (7, 2, 0x84)])
    body += struct.pack("<BIBB", 0, start, 0, 0)  # Timer start
    body += struct.pack("<BIHBB", 1, start, 200, 145, 90)
    body += struct.pack("<BIHBB", 1, start + 2, 0, 0xFF, 0)
    body += struct.pack("<BIBB", 0, start + 3, 0, 1)  # Timer stop
    body += struct.pack("<BIBB", 0, start + 8, 0, 0)
    body += struct.pack("<BIH", 2, start + 8, 210)
    body += struct.pack("<BIHBB", 1, start + 9, 0xFFFF, 0, 0xFF)
    header = struct.pack("<BBHI4s", 12, 0x10, 2171, len(body), b".FIT")
    return header + body + struct.pack("<H", crc16(header + body))


def project(data):
    rows = []
    with fitdecode.FitReader(
        io.BytesIO(data), check_crc=fitdecode.CrcCheck.RAISE,
        error_handling=fitdecode.ErrorHandling.RAISE,
    ) as reader:
        for message in reader:
            if not isinstance(message, fitdecode.FitDataMessage):
                continue
            if message.name == "event" and message.get_value("event") == "timer":
                state = {"start": "RECORDING", "stop": "PAUSED"}.get(
                    message.get_value("event_type")
                )
                if state:
                    rows.append((message.get_value("timestamp").isoformat(), "STATE", state, "-"))
            elif message.name == "record":
                stamp = message.get_value("timestamp").isoformat()
                for field, metric in (
                    ("power", "POWER_W"), ("heart_rate", "HEART_RATE_BPM"),
                    ("cadence", "CADENCE_RPM"),
                ):
                    if not message.has_field(field):
                        rows.append((stamp, metric, "-", "UNAVAILABLE"))
                        continue
                    value = message.get_value(field)
                    if value is None or (field == "heart_rate" and value == 0):
                        rows.append((stamp, metric, "-", "INVALID_SOURCE_VALUE"))
                    else:
                        rows.append((stamp, metric, str(value), "-"))
    return "".join("\t".join(row) + "\n" for row in rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    if version("fitdecode") != "0.11.0":
        raise SystemExit("Install fitdecode==0.11.0 to verify this fixture")
    data = synthetic_fit()
    actual = project(data)
    try:
        project(data[:-1] + bytes([data[-1] ^ 1]))
    except fitdecode.FitCRCError:
        pass
    else:
        raise SystemExit("Corrupted FIT passed CRC validation")
    if args.write:
        OUTPUT.write_bytes(actual.encode("ascii"))
    elif OUTPUT.read_text(encoding="ascii") != actual:
        raise SystemExit("FIT replay fixture differs from the decoded projection")
    print(f"Verified {len(actual.splitlines())} projected events and FIT CRC rejection")


if __name__ == "__main__":
    main()
