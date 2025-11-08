#!/usr/bin/env python3

import sys, re, pathlib, requests

def main():
    if len(sys.argv) != 2:
        print("usage: check_briefing_links.py <briefings_dir>", file=sys.stderr)
        sys.exit(2)
    base = pathlib.Path(sys.argv[1])
    if not base.exists():
        print(f"not found: {base}", file=sys.stderr)
        sys.exit(2)
    bad = []
    for md in base.glob("*.md"):
        text = md.read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r"https://github\.com/[^)\s]+", text):
            url = m.group(0)
            if "/blob/main/" in url or "/blob/master/" in url:
                bad.append((md.name, url, "not pinned to a SHA"))
                continue
            try:
                r = requests.get(url, timeout=15)
                if r.status_code != 200:
                    bad.append((md.name, url, f"HTTP {r.status_code}"))
            except Exception as e:
                bad.append((md.name, url, f"ERR {type(e).__name__}: {e}"))
    if bad:
        for f,u,msg in bad:
            print(f"[{f}] {u} -> {msg}")
        sys.exit(1)
    print("OK: all pinned links resolved.")

if __name__ == "__main__":
    main()
