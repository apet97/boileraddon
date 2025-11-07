#!/usr/bin/env python3
import sys, json, os
import jwt

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
key_path = os.path.join(root, 'dev-docs-marketplace-cake-snapshot', 'extras', 'clockify-public-key.pem')
public_key = open(key_path,'r',encoding='utf-8').read()

token = sys.argv[1] if len(sys.argv) > 1 else None
if not token:
    print('Usage: verify-jwt-example.py <JWT>')
    sys.exit(2)

try:
    claims = jwt.decode(token, public_key, algorithms=['RS256'], options={"verify_aud": False})
    print(json.dumps(claims, indent=2))
except Exception as e:
    print('JWT verify failed:', e)
    sys.exit(1)
