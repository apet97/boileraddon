#!/usr/bin/env python3
import sys, os
import xml.etree.ElementTree as ET

def gen_badge(pct: str, color: str):
    label = "coverage"
    value = pct
    # Simple static SVG badge (no external service)
    # width tuned for label/value lengths
    label_w = 62
    value_w = 58 if len(value) <= 4 else 70
    total_w = label_w + value_w
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{total_w}" height="20" role="img" aria-label="{label}: {value}">
  <linearGradient id="s" x2="0" y2="100%">
    <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
    <stop offset="1" stop-opacity=".1"/>
  </linearGradient>
  <mask id="m"><rect width="{total_w}" height="20" rx="3" fill="#fff"/></mask>
  <g mask="url(#m)">
    <rect width="{label_w}" height="20" fill="#555"/>
    <rect x="{label_w}" width="{value_w}" height="20" fill="{color}"/>
    <rect width="{total_w}" height="20" fill="url(#s)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="11">
    <text x="{label_w/2:.0f}" y="15">{label}</text>
    <text x="{label_w + value_w/2:.0f}" y="15">{value}</text>
  </g>
</svg>'''

def compute_coverage(xml_path: str):
    try:
        root = ET.parse(xml_path).getroot()
    except Exception:
        return None
    covered = missed = 0
    for c in root.findall('.//counter'):
        if c.get('type') == 'INSTRUCTION':
            covered += int(c.get('covered') or 0)
            missed += int(c.get('missed') or 0)
    if covered + missed == 0:
        return 0.0
    return covered / (covered + missed)

def main():
    if len(sys.argv) < 3:
        print("usage: coverage_badge.py <jacoco.xml> <outdir>")
        sys.exit(2)
    xml_path, outdir = sys.argv[1], sys.argv[2]
    os.makedirs(outdir, exist_ok=True)
    ratio = compute_coverage(xml_path)
    summary_path = os.path.join(outdir, 'summary.json')
    badge_path = os.path.join(outdir, 'badge.svg')
    if ratio is None:
        # No coverage file found; write N/A
        with open(summary_path, 'w') as f:
            f.write('{"coverage":"N/A"}')
        with open(badge_path, 'w') as f:
            f.write(gen_badge('N/A', '#9f9f9f'))
        return
    pct = int(round(ratio * 100))
    color = '#e05d44' if pct < 50 else ('#dfb317' if pct < 70 else '#4c1')
    with open(summary_path, 'w') as f:
        f.write('{"coverage":"%d"}' % pct)
    with open(badge_path, 'w') as f:
        f.write(gen_badge(f"{pct}%", color))

if __name__ == '__main__':
    main()

