#!/usr/bin/env python3
"""Generate a JWT compatible with TestForge/Auth (jjwt HS256 with base64-decoded secret)."""
import base64
import json
import os
import sys
import time

import jwt as pyjwt


def main() -> int:
    secret_b64 = os.environ.get("JWT_SECRET")
    if not secret_b64:
        print("error: JWT_SECRET env var not set", file=sys.stderr)
        return 2

    try:
        key_bytes = base64.b64decode(secret_b64, validate=True)
    except Exception as e:
        print(f"error: JWT_SECRET is not valid base64: {e}", file=sys.stderr)
        return 2

    subject = os.environ.get("JWT_SUB", "test@healthmine.com")
    roles = json.loads(os.environ.get("JWT_ROLES", '["ROLE_ADMIN"]'))
    ttl = int(os.environ.get("JWT_TTL", "3600"))

    now = int(time.time())
    payload = {
        "sub": subject,
        "roles": roles,
        "iat": now,
        "exp": now + ttl,
    }

    token = pyjwt.encode(payload, key_bytes, algorithm="HS256")
    print(token)
    return 0


if __name__ == "__main__":
    sys.exit(main())
