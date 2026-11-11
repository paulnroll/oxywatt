"""Synthetic-only decoder edge cases; never reads a personal ride."""
import hashlib
import json
import struct
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

import fitdecode
from export_fit_replay import decode, export
from verify_fit_replay import crc16, definition, synthetic_fit


def stream(body):
    header = struct.pack("<BBHI4s", 12, 0x10, 2171, len(body), b".FIT")
    return header + body + struct.pack("<H", crc16(header + body))


def timer(stamp, event_type):
    return struct.pack("<BIBB", 0, stamp if stamp == 0xFFFFFFFF else 1_136_073_600 + stamp, 0, event_type)


class ExportTest(unittest.TestCase):
    def decode(self, data):
        return decode(data, "generated-synthetic-v1")

    def test_deterministic_mapping_and_provenance(self):
        tsv, manifest = self.decode(synthetic_fit())
        self.assertEqual((tsv, manifest), self.decode(synthetic_fit()))
        self.assertEqual(15, manifest["projected_rows"])
        self.assertIn("\tHEART_RATE_BPM\t-\tUNAVAILABLE\t-\n", tsv)
        self.assertIn("\tHEART_RATE_BPM\t-\tINVALID_SOURCE_VALUE\t-\n", tsv)
        self.assertIn("\tPOWER_W\t0\t-\t-\n", tsv)
        self.assertEqual(["start", "stop", "start"], [t["event_type"] for t in manifest["timer_events"]])
        self.assertIn("final_timer_stop_not_observed", manifest["diagnostics"])

    def test_corrupt_truncated_trailing_and_concatenated(self):
        data = synthetic_fit()
        corrupt = data[:-1] + bytes([data[-1] ^ 1])
        with self.assertRaises(fitdecode.FitCRCError):
            self.decode(corrupt)
        for invalid in (data[:-1], data + b"x", data + data, b""):
            with self.assertRaises(ValueError): self.decode(invalid)

    def test_stop_all_is_pause_and_original_timer_is_retained(self):
        body = definition(0, 21, [(253, 4, 0x86), (0, 1, 0), (1, 1, 0)])
        tsv, manifest = self.decode(stream(body + timer(100, 0) + timer(105, 4)))
        self.assertIn("5000000000\tSTATE\tPAUSED\t-\tstop_all", tsv)
        self.assertEqual("stop_all", manifest["timer_events"][-1]["event_type"])
        self.assertEqual(["native_saved_or_discarded_unknown"], manifest["diagnostics"])

    def test_timer_trigger_remains_in_manifest_without_changing_canonical_pause(self):
        body = definition(0, 21, [(253, 4, 0x86), (0, 1, 0), (1, 1, 0), (3, 4, 0x86)])
        data = stream(body + struct.pack("<BIBBI", 0, 1_136_073_600, 0, 4, 1))
        tsv, manifest = self.decode(data)
        self.assertIn("STATE\tPAUSED", tsv)
        self.assertEqual("auto", manifest["timer_events"][0]["timer_trigger"])

    def test_missing_regressing_and_unsupported_timer_are_rejected(self):
        body = definition(0, 21, [(253, 4, 0x86), (0, 1, 0), (1, 1, 0)])
        for events, reason in ((timer(0xFFFFFFFF, 0), "Missing/invalid"), (timer(105, 0) + timer(100, 1), "Regressing"), (timer(100, 2), "Unsupported")):
            with self.assertRaisesRegex(ValueError, reason): self.decode(stream(body + events))

    def test_record_only_has_explicit_lifecycle_diagnostics(self):
        body = definition(0, 20, [(253, 4, 0x86), (7, 2, 0x84)])
        tsv, manifest = self.decode(stream(body + struct.pack("<BIH", 0, 1_136_073_700, 250)))
        self.assertNotIn("STATE", tsv)
        self.assertIn("initial_timer_start_not_observed", manifest["diagnostics"])
        self.assertIn("final_timer_stop_not_observed", manifest["diagnostics"])

    def test_checked_in_synthetic_export_matches_pinned_decoder(self):
        directory = Path(__file__).resolve().parents[1] / "core/src/test/resources/fit-scenario"
        tsv, manifest = self.decode(synthetic_fit())
        self.assertEqual(tsv, directory.joinpath("observations.tsv").read_text())
        self.assertEqual(manifest, json.loads(directory.joinpath("manifest.json").read_text()))

    def test_export_hash_survives_windows_text_newline_translation(self):
        original_open = Path.open

        def windows_open(path, mode="r", *args, **kwargs):
            if "w" in mode and "b" not in mode:
                kwargs["newline"] = "\r\n"
            return original_open(path, mode, *args, **kwargs)

        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            with patch.object(Path, "open", windows_open):
                manifest = export(synthetic_fit(), "generated-synthetic-v1", directory)
            raw = directory.joinpath("observations.tsv").read_bytes()
            self.assertEqual(manifest["projection_sha256"], hashlib.sha256(raw).hexdigest())
            self.assertNotIn(b"\r\n", raw)

    def test_private_export_cannot_escape_ignored_directory(self):
        with tempfile.TemporaryDirectory() as name:
            with self.assertRaises(ValueError):
                export(synthetic_fit(), "private-owner-input", Path(name))
            self.assertEqual([], list(Path(name).iterdir()))


if __name__ == "__main__":
    unittest.main()
