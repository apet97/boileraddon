#!/usr/bin/env python3
import os
import json
from urllib.parse import urljoin, urlsplit, urlunsplit

from bs4 import BeautifulSoup

try:
    from markdownify import markdownify as md
    HAS_MD = True
except Exception:
    HAS_MD = False

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
HTML_DIR = os.path.join(OUT_DIR, "html")
URL_MAP_JSON = os.path.join(OUT_DIR, "url_map.json")
AGG_MD = os.path.join(OUT_DIR, "cake_marketplace_dev_docs.md")


def strip_fragment(u: str) -> str:
    parts = urlsplit(u)
    return urlunsplit((parts.scheme, parts.netloc, parts.path, parts.query, ""))


def convert_images_to_nothing(attrs, is_self_closing):
    return ""


def main():
    if not os.path.exists(URL_MAP_JSON):
        raise SystemExit("url_map.json not found. Run crawler first.")

    with open(URL_MAP_JSON, "r", encoding="utf-8") as f:
        url_map = json.load(f)

    # Ensure stable order by URL
    items = sorted(url_map.items(), key=lambda kv: kv[0])

    with open(AGG_MD, "w", encoding="utf-8") as out:
        for url, relpath in items:
            fpath = os.path.join(OUT_DIR, relpath)
            if not os.path.exists(fpath):
                continue
            with open(fpath, "r", encoding="utf-8") as f:
                html = f.read()

            soup = BeautifulSoup(html, "lxml")

            # pick main content
            main = soup.find("main") or soup.find("article") or soup.find("body")
            if main is None:
                main = soup

            # strip chrome elements inside the chosen container
            for tag in main.find_all(["nav", "header", "footer", "aside", "script", "style"]):
                tag.decompose()

            # absolutize links to keep them valid/visible
            for a in main.find_all("a", href=True):
                a["href"] = strip_fragment(urljoin(url, a.get("href")))

            # title
            title = None
            h1 = main.find("h1")
            if h1 and h1.get_text(strip=True):
                title = h1.get_text(strip=True)
            if not title:
                title_tag = soup.find("title")
                if title_tag and title_tag.get_text(strip=True):
                    title = title_tag.get_text(strip=True)
            if not title:
                title = url

            # convert to markdown
            if not HAS_MD:
                raise SystemExit("markdownify is required. Run: pip install markdownify bs4 lxml")

            md_body = md(
                str(main),
                heading_style="ATX",
                strip=['img'],
            )

            out.write(f"# {title}\n")
            out.write(f"> Source: {url}\n\n")
            out.write(md_body.strip() + "\n\n")
            out.write("---\n\n")

    print("Wrote:", AGG_MD)


if __name__ == "__main__":
    main()
