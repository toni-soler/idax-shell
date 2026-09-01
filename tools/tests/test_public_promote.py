import importlib.util, json, subprocess, tempfile, unittest
from pathlib import Path

SPEC=importlib.util.spec_from_file_location("public_promote",Path(__file__).parents[1]/"public_promote.py")
PROMOTE=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(PROMOTE)

class PromotionTests(unittest.TestCase):
    def test_transform_is_deterministic(self): self.assertEqual(PROMOTE.transform(b"a\r\n\r\n",[{"id":"normalize-text","version":1}]),b"a\n")
    def test_scan_rejects_private_key(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); (root/"bad.txt").write_text("-----BEGIN PRIVATE KEY-----")
            manifest={"forbidden":{"patterns":[],"imports":[],"dependencies":[]},"generatedFiles":{"policy":"allow-declared","patterns":[]}}
            with self.assertRaisesRegex(ValueError,"forbidden content"): PROMOTE.scan(root,manifest)
    def test_scan_rejects_generated_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); (root/"x.generated.js").write_text("safe")
            manifest={"forbidden":{"patterns":[],"imports":[],"dependencies":[]},"generatedFiles":{"policy":"reject","patterns":["*.generated.js"]}}
            with self.assertRaisesRegex(ValueError,"generated files rejected"): PROMOTE.scan(root,manifest)
    def test_closed_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/"m.json"; p.write_text(json.dumps({"schemaVersion":1,"extra":True}))
            with self.assertRaisesRegex(ValueError,"closed v1"): PROMOTE.load(p)

if __name__ == "__main__": unittest.main()
