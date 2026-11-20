"""Developer-assisted real FIT archive; all ride artifacts remain private."""
import argparse
import io
import json
from datetime import datetime
from pathlib import Path
import fitdecode
from export_fit_replay import decode, PRIVATE


def read_summaries(data):
    sessions, activities = [], []
    with fitdecode.FitReader(io.BytesIO(data), check_crc=fitdecode.CrcCheck.RAISE,
                            error_handling=fitdecode.ErrorHandling.RAISE) as reader:
        for message in reader:
            if not isinstance(message, fitdecode.FitDataMessage):
                continue
            if message.name == "session":
                sessions.append({k: message.get_value(k, fallback=None) for k in
                                 ("start_time", "timestamp", "sport", "total_elapsed_time", "total_timer_time")})
            if message.name == "activity":
                activities.append({k: message.get_value(k, fallback=None) for k in
                                   ("timestamp", "event", "event_type", "total_timer_time")})
    return sessions, activities


def qualify(manifest, sessions, activities):
    if len(sessions) != 1 or len(activities) != 1:
        raise ValueError("Only one session/activity is supported")
    session, activity = sessions[0], activities[0]
    start, end = session["start_time"], session["timestamp"]
    if not all(isinstance(t, datetime) and t.tzinfo is not None for t in (start, end, activity["timestamp"])):
        raise ValueError("Missing qualified file boundaries")
    if session["sport"] != "cycling" or activity["event"] != "activity" or activity["event_type"] != "stop":
        raise ValueError("Unsupported activity completion")
    origin = datetime.fromisoformat(manifest["file_origin"])
    last = origin.timestamp() + manifest["end_offset_nanos"] / 1e9
    if not (0 < (end - start).total_seconds() <= 86400 and start <= origin and end.timestamp() >= last
            and activity["timestamp"] == end):
        raise ValueError("Inconsistent file boundaries")
    active = None
    intervals = []
    timers = manifest["timer_events"]
    for event in timers:
        at = datetime.fromisoformat(event["file_timestamp"])
        if event["event_type"] == "start":
            if active is not None:
                raise ValueError("Nested recording start")
            active = at
        else:
            if active is None:
                raise ValueError("Unpaired recording stop")
            intervals.append((active, at))
            active = None
    if not intervals or active is not None or "initial_timer_start_not_observed" in manifest["diagnostics"]:
        raise ValueError("Incomplete timer boundaries")
    result = dict(manifest)
    result.update(schema="fit-history-archive-v1", session_start=start.isoformat(), session_end=end.isoformat(),
                  completion="file-session-activity-stop-v1", timer_sum_seconds=sum((b-a).total_seconds() for a,b in intervals),
                  declared_session_timer_seconds=session["total_timer_time"],
                  declared_session_elapsed_seconds=session["total_elapsed_time"],
                  declared_activity_timer_seconds=activity["total_timer_time"])
    return result


def export_history(data, directory):
    directory = directory.resolve()
    if not directory.is_relative_to(PRIVATE.resolve()):
        raise ValueError("Private directory required")
    if not 0 < len(data) <= 16 * 1024 * 1024:
        raise ValueError("FIT input exceeds bounded scope")
    projection, manifest = decode(data, "private-owner-input")
    if len(projection.encode()) > 16 * 1024 * 1024 or not 0 < manifest["projected_rows"] <= 131072:
        raise ValueError("Projection exceeds bounded scope")
    result = qualify(manifest, *read_summaries(data))
    directory.mkdir(parents=True, exist_ok=True)
    for name, content in (("observations.tsv", projection), ("manifest.json", json.dumps(result, sort_keys=True, indent=2) + "\n")):
        temporary = directory / (name + ".tmp")
        temporary.write_bytes(content.encode())
        temporary.replace(directory / name)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    try:
        export_history(args.input.read_bytes(), args.output_dir)
    except (ValueError, OSError, KeyError, fitdecode.FitError):
        raise SystemExit("FIT history archive rejected; inspect private input qualification") from None
    print("Verified private real FIT history archive; no production import performed")


if __name__ == "__main__":
    main()
