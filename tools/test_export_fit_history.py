import copy
import json
import tempfile
import struct
import unittest
from datetime import datetime, timedelta
from pathlib import Path
from export_fit_replay import decode, PRIVATE
from export_fit_history import export_history, qualify, read_summaries
from verify_fit_replay import synthetic_fit, definition, crc16


class HistoryExportTest(unittest.TestCase):
    def facts(self):
        original = synthetic_fit()
        start_seconds = 1_136_073_600
        body = original[12:-2] + struct.pack("<BIBB", 0, start_seconds + 10, 0, 4)
        body += definition(3, 18, [(253, 4, 0x86), (2, 4, 0x86), (5, 1, 0), (7, 4, 0x86), (8, 4, 0x86)])
        body += struct.pack("<BIIBII", 3, start_seconds + 10, start_seconds, 2, 10000, 1000)
        body += definition(4, 34, [(253, 4, 0x86), (0, 4, 0x86), (3, 1, 0), (4, 1, 0)])
        body += struct.pack("<BIIBB", 4, start_seconds + 10, 10000, 26, 1)
        header = struct.pack("<BBHI4s", 12, 0x10, 2171, len(body), b".FIT")
        data = header + body + struct.pack("<H", crc16(header + body))
        _, manifest = decode(data, "private-owner-input")
        start = datetime.fromisoformat(manifest["file_origin"])
        end = start + timedelta(seconds=manifest["end_offset_nanos"] / 1e9)
        sessions, activities = read_summaries(data)
        session, activity = sessions[0], activities[0]
        return data, manifest, session, activity

    def test_qualified_boundaries_preserve_conflicting_summary_without_padding(self):
        _, manifest, session, activity = self.facts()
        result = qualify(manifest, [session], [activity])
        self.assertEqual("fit-history-archive-v1", result["schema"])
        self.assertEqual(manifest["end_offset_nanos"], result["end_offset_nanos"])
        self.assertEqual(1.0, result["declared_session_timer_seconds"])
        self.assertEqual(session["start_time"].isoformat(), result["session_start"])

    def test_incomplete_multiple_and_unsupported_activity_refuse(self):
        _, manifest, session, activity = self.facts()
        for sessions, activities in (([], [activity]), ([session, session], [activity]), ([session], [])):
            with self.assertRaises(ValueError): qualify(manifest, sessions, activities)
        bad = dict(activity, event_type="start")
        with self.assertRaises(ValueError): qualify(manifest, [session], [bad])
        bad_manifest = copy.deepcopy(manifest)
        bad_manifest["timer_events"].pop()
        with self.assertRaises(ValueError): qualify(bad_manifest, [session], [activity])

    def test_export_is_private_filtered_and_repeatable(self):
        data, _, session, activity = self.facts()
        PRIVATE.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=PRIVATE) as tmp:
            root = Path(tmp)
            first = export_history(data, root)
            projection = (root / "observations.tsv").read_bytes()
            self.assertEqual(first, export_history(data, root))
            self.assertEqual(projection, (root / "observations.tsv").read_bytes())
            self.assertEqual(first, json.loads((root / "manifest.json").read_text()))
            self.assertNotIn("position_lat", (root / "manifest.json").read_text())
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError): export_history(data, Path(tmp))


if __name__ == "__main__": unittest.main()
