#!/usr/bin/env python3
import json, sys, os
from jsonschema import validate, Draft7Validator

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
schema_path = os.path.join(root, 'dev-docs-marketplace-cake-snapshot', 'extras', 'manifest-schema-latest.json')
with open(schema_path, 'r', encoding='utf-8') as f:
    schema = json.load(f)

errors = 0
targets = sys.argv[1:] or [
    'addons/_template-addon/manifest.json'
]

for rel in targets:
    path = os.path.join(root, rel)
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    v = Draft7Validator(schema)
    errs = sorted(v.iter_errors(data), key=lambda e: e.path)
    if errs:
        print(f"Invalid: {rel}")
        for e in errs:
            loc = "/".join([str(x) for x in e.path])
            print(f"  - {loc}: {e.message}")
        errors += 1
    else:
        print(f"OK: {rel}")

sys.exit(1 if errors else 0)
