#!/usr/bin/env python3
import os
import re
import json
import time
import logging
from collections import deque
from urllib.parse import urlparse, urljoin, urlsplit, urlunsplit

import requests
from bs4 import BeautifulSoup

try:
    from playwright.sync_api import sync_playwright
    HAS_PLAYWRIGHT = True
except Exception:
    HAS_PLAYWRIGHT = False


ROOT_URL = "https://dev-docs.marketplace.cake.com/"
EXTRA_SEEDS = [
    "/clockify/learn/",
    "/clockify/build/",
    "/clockify/publish/",
]
ALLOWED_HOST = "dev-docs.marketplace.cake.com"
OUT_DIR = os.path.dirname(os.path.abspath(__file__))
HTML_DIR = os.path.join(OUT_DIR, "html")
URLS_TXT = os.path.join(OUT_DIR, "urls.txt")
URL_MAP_JSON = os.path.join(OUT_DIR, "url_map.json")
CRAWL_LOG = os.path.join(OUT_DIR, "crawl_log.txt")


BLOCKED_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".pdf",
    ".zip", ".tar", ".gz", ".tgz", ".css", ".js", ".woff",
    ".woff2", ".ttf", ".eot", ".map"
}


def is_html_response(resp: requests.Response) -> bool:
    ctype = resp.headers.get("Content-Type", "").lower()
    return resp.status_code == 200 and "text/html" in ctype


def strip_fragment(u: str) -> str:
    parts = urlsplit(u)
    return urlunsplit((parts.scheme, parts.netloc, parts.path, parts.query, ""))


def has_blocked_extension(path: str) -> bool:
    path = path.lower()
    for ext in BLOCKED_EXTENSIONS:
        if path.endswith(ext):
            return True
    return False


def absolutize_links(base_url: str, soup: BeautifulSoup) -> None:
    for a in soup.find_all("a", href=True):
        href = a.get("href")
        if href is None:
            continue
        abs_url = urljoin(base_url, href)
        a["href"] = strip_fragment(abs_url)


def make_filename_for_url(url: str, used_names: set) -> str:
    parts = urlsplit(url)
    path = parts.path
    query = parts.query
    if not path or path == "/":
        slug = "index"
    else:
        slug = path.strip("/").replace("/", "_")
        if not slug:
            slug = "index"
    if query:
        qslug = re.sub(r"[^A-Za-z0-9]+", "_", query)[:120].strip("_")
        if qslug:
            slug = f"{slug}_{qslug}"
    # ensure uniqueness
    fname = f"{slug}.html"
    i = 2
    while fname in used_names:
        fname = f"{slug}_{i}.html"
        i += 1
    used_names.add(fname)
    return fname


def setup_logging():
    logger = logging.getLogger("crawler")
    logger.setLevel(logging.INFO)
    handler = logging.FileHandler(CRAWL_LOG, mode="a", encoding="utf-8")
    fmt = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
    handler.setFormatter(fmt)
    logger.addHandler(handler)
    # also log to stdout minimally
    sh = logging.StreamHandler()
    sh.setLevel(logging.INFO)
    sh.setFormatter(fmt)
    logger.addHandler(sh)
    return logger


def crawl_with_requests(logger):
    os.makedirs(HTML_DIR, exist_ok=True)

    session = requests.Session()
    session.headers.update({
        "User-Agent": (
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/118.0.0.0 Safari/537.36"
        )
    })

    seeds = [ROOT_URL] + [urljoin(ROOT_URL, p) for p in EXTRA_SEEDS]
    # unique and normalized
    seeds = [strip_fragment(s) for s in seeds]
    queue = deque()
    seen = set()
    for s in seeds:
        if urlsplit(s).netloc == ALLOWED_HOST:
            queue.append(s)
            seen.add(s)
    visited = []  # list preserves order
    url_to_file = {}
    used_names = set()

    while queue:
        current = queue.popleft()
        current = strip_fragment(current)
        logger.info(f"FETCH START {current}")
        try:
            resp = session.get(current, allow_redirects=True, timeout=20)
        except Exception as e:
            logger.error(f"FETCH ERROR {current} {e}")
            continue

        final_url = strip_fragment(resp.url)
        final_parts = urlsplit(final_url)
        if final_parts.netloc != ALLOWED_HOST:
            logger.info(f"FETCH SKIP REDIRECTED_OUT {current} -> {final_url}")
            continue

        if not is_html_response(resp):
            logger.info(
                f"FETCH SKIP NON-HTML {final_url} status={resp.status_code} ctype={resp.headers.get('Content-Type')}"
            )
            continue

        # Parse and discover links
        soup = BeautifulSoup(resp.text, "lxml")
        absolutize_links(final_url, soup)

        # Save HTML snapshot (pretty printed original text to keep close to fetched content)
        if final_url not in url_to_file:
            fname = make_filename_for_url(final_url, used_names)
            fpath = os.path.join(HTML_DIR, fname)
            try:
                with open(fpath, "w", encoding="utf-8") as f:
                    f.write(resp.text)
                url_to_file[final_url] = os.path.relpath(fpath, OUT_DIR)
                visited.append(final_url)
                logger.info(f"FETCH OK {final_url} -> {url_to_file[final_url]}")
            except Exception as e:
                logger.error(f"WRITE ERROR {final_url} {e}")
                # don't abort; continue crawling links

        # Discover links
        for a in soup.find_all("a", href=True):
            href = a.get("href")
            if not href:
                continue
            abs_url = strip_fragment(href)
            parts = urlsplit(abs_url)
            if parts.scheme not in ("http", "https"):
                continue
            if parts.netloc != ALLOWED_HOST:
                continue
            if has_blocked_extension(parts.path):
                continue
            if abs_url not in seen:
                seen.add(abs_url)
                queue.append(abs_url)
                logger.info(f"DISCOVERED {final_url} -> {abs_url}")

        # be polite
        time.sleep(0.3)

    # Write outputs
    try:
        with open(URLS_TXT, "w", encoding="utf-8") as f:
            for u in sorted(visited):
                f.write(u + "\n")
    except Exception as e:
        logger.error(f"WRITE ERROR urls.txt {e}")

    try:
        with open(URL_MAP_JSON, "w", encoding="utf-8") as f:
            json.dump(url_to_file, f, indent=2, ensure_ascii=False)
    except Exception as e:
        logger.error(f"WRITE ERROR url_map.json {e}")

    # Print coverage summary
    print("Total unique HTML URLs:", len(visited))
    print("Collected URLs:")
    for u in sorted(visited):
        print(u)


def crawl_with_playwright(logger):
    os.makedirs(HTML_DIR, exist_ok=True)

    seeds = [ROOT_URL] + [urljoin(ROOT_URL, p) for p in EXTRA_SEEDS]
    seeds = [strip_fragment(s) for s in seeds]
    queue = deque()
    seen = set()
    for s in seeds:
        if urlsplit(s).netloc == ALLOWED_HOST:
            queue.append(s)
            seen.add(s)

    visited = []
    url_to_file = {}
    used_names = set()

    user_agent = (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/118.0.0.0 Safari/537.36"
    )

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(user_agent=user_agent, java_script_enabled=True)
        while queue:
            current = queue.popleft()
            current = strip_fragment(current)
            logger.info(f"FETCH START {current}")
            page = context.new_page()
            try:
                page.goto(current, wait_until="domcontentloaded", timeout=30000)
                # give some time for SPA to render nav/content
                page.wait_for_timeout(800)
            except Exception as e:
                logger.error(f"FETCH ERROR {current} {e}")
                try:
                    page.close()
                except Exception:
                    pass
                continue

            final_url = strip_fragment(page.url)
            final_parts = urlsplit(final_url)
            if final_parts.netloc != ALLOWED_HOST:
                logger.info(f"FETCH SKIP REDIRECTED_OUT {current} -> {final_url}")
                page.close()
                continue

            # get rendered HTML
            try:
                html = page.content()
            except Exception as e:
                logger.error(f"READ ERROR {final_url} {e}")
                html = ""

            # Save HTML snapshot
            if final_url not in url_to_file:
                fname = make_filename_for_url(final_url, used_names)
                fpath = os.path.join(HTML_DIR, fname)
                try:
                    with open(fpath, "w", encoding="utf-8") as f:
                        f.write(html)
                    url_to_file[final_url] = os.path.relpath(fpath, OUT_DIR)
                    visited.append(final_url)
                    logger.info(f"FETCH OK {final_url} -> {url_to_file[final_url]}")
                except Exception as e:
                    logger.error(f"WRITE ERROR {final_url} {e}")

            # Discover links from rendered DOM
            try:
                anchors = page.eval_on_selector_all("a[href]", "els => els.map(e => e.getAttribute('href'))")
            except Exception:
                anchors = []

            for href in anchors:
                if not href:
                    continue
                abs_url = strip_fragment(urljoin(final_url, href))
                parts = urlsplit(abs_url)
                if parts.scheme not in ("http", "https"):
                    continue
                if parts.netloc != ALLOWED_HOST:
                    continue
                if has_blocked_extension(parts.path):
                    continue
                if abs_url not in seen:
                    seen.add(abs_url)
                    queue.append(abs_url)
                    logger.info(f"DISCOVERED {final_url} -> {abs_url}")

            try:
                page.close()
            except Exception:
                pass
            time.sleep(0.2)

        try:
            context.close()
        except Exception:
            pass
        try:
            browser.close()
        except Exception:
            pass

    # Write outputs
    try:
        with open(URLS_TXT, "w", encoding="utf-8") as f:
            for u in sorted(visited):
                f.write(u + "\n")
    except Exception as e:
        logger.error(f"WRITE ERROR urls.txt {e}")

    try:
        with open(URL_MAP_JSON, "w", encoding="utf-8") as f:
            json.dump(url_to_file, f, indent=2, ensure_ascii=False)
    except Exception as e:
        logger.error(f"WRITE ERROR url_map.json {e}")

    print("Total unique HTML URLs:", len(visited))
    print("Collected URLs:")
    for u in sorted(visited):
        print(u)


def main():
    logger = setup_logging()
    if HAS_PLAYWRIGHT:
        logger.info("Using Playwright-rendered crawling for dynamic links")
        crawl_with_playwright(logger)
    else:
        logger.info("Playwright not available; using requests-based crawling")
        crawl_with_requests(logger)


if __name__ == "__main__":
    main()
