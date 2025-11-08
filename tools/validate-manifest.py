#!/usr/bin/env python3
"""
Validate Clockify add-on manifests under addons/*/manifest.json.

Behavior:
- Loads a local JSON schema if available at tools/manifest.schema.json,
  else falls back to dev-docs snapshot if present.
- Enforces no "$schema", requires schemaVersion=="1.3", and required keys.
- Uses jsonschema if installed; otherwise performs minimal checks only.
"""

import glob
import json
import os
import sys

try:
    from jsonschema import Draft7Validator  # type: ignore
    _HAS_JSONSCHEMA = True
except Exception:
    Draft7Validator = None  # type: ignore
    _HAS_JSONSCHEMA = False

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOCAL_SCHEMA = os.path.join(ROOT, 'tools', 'manifest.schema.json')
SNAPSHOT_SCHEMA = os.path.join(ROOT, 'dev-docs-marketplace-cake-snapshot', 'extras', 'manifest-schema-latest.json')
REQUIRED_SCHEMA_VERSION = "1.3"

def load_schema():
    path = None
    if os.path.isfile(LOCAL_SCHEMA):
        path = LOCAL_SCHEMA
    elif os.path.isfile(SNAPSHOT_SCHEMA):
        path = SNAPSHOT_SCHEMA
    if not path:
        return None
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return None

def validate_manifest_dict(path, data, schema):
    errs = []
    # Local hard checks
    if "$schema" in data:
        errs.append(f"{path}: contains $schema (Clockify will reject this manifest)")
    sv = data.get("schemaVersion")
    if sv != REQUIRED_SCHEMA_VERSION:
        errs.append(f"{path}: schemaVersion must be \"{REQUIRED_SCHEMA_VERSION}\" (found {sv!r})")
    for k in ("key", "name", "schemaVersion", "baseUrl"):
        if k not in data:
            errs.append(f"{path}: missing required field: {k}")

    # JSON schema validation (optional)
    if _HAS_JSONSCHEMA and schema is not None:
        try:
            v = Draft7Validator(schema)  # type: ignore
            for e in sorted(v.iter_errors(data), key=lambda e: list(e.path)):
                loc = "/".join([str(x) for x in e.path])
                errs.append(f"{path}: {loc or '<root>'}: {e.message}")
        except Exception as e:
            errs.append(f"{path}: schema validation failed: {e}")

    return errs

def main():
    schema = load_schema()

    targets = sys.argv[1:]
    if not targets:
        manifests = sorted(glob.glob(os.path.join(ROOT, 'addons', '*', 'manifest.json')))
        if not manifests:
            print("No manifests found under addons/. Nothing to validate.")
            return 0
        targets = [os.path.relpath(p, ROOT) for p in manifests]

    failed = 0
    for rel in targets:
        path = os.path.join(ROOT, rel)
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except Exception as e:
            print(f"ERROR: {rel} is not valid JSON: {e}")
            failed += 1
            continue

        errs = validate_manifest_dict(rel, data, schema)
        if errs:
            print(f"Invalid: {rel}")
            for msg in errs:
                print(f"  - {msg}")
            failed += 1
        else:
            print(f"OK: {rel}")

    return 1 if failed else 0

if __name__ == '__main__':
    sys.exit(main())
