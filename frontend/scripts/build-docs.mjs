#!/usr/bin/env node
/**
 * Converts docs/*.md and CONTRIBUTING.md to styled HTML pages
 * in frontend/public/ for GitHub Pages deployment.
 *
 * Usage: node scripts/build-docs.mjs
 */
import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "fs";
import { join } from "path";
import { marked } from "marked";

const ROOT = join(import.meta.dirname, "../..");
const PUBLIC_DOCS = join(import.meta.dirname, "../public/docs");

mkdirSync(PUBLIC_DOCS, { recursive: true });

function htmlTemplate(title, content) {
  return `<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title} — URLShort</title>
  <style>
    :root {
      --bg: #ffffff;
      --fg: #1a1a2e;
      --muted: #6b7280;
      --border: #e5e7eb;
      --code-bg: #f3f4f6;
      --link: #2563eb;
      --header-bg: #fafafa;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --bg: #0a0a0a;
        --fg: #e5e5e5;
        --muted: #9ca3af;
        --border: #27272a;
        --code-bg: #1a1a1a;
        --link: #60a5fa;
        --header-bg: #111111;
      }
    }
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--fg);
      line-height: 1.7;
    }
    header {
      border-bottom: 1px solid var(--border);
      background: var(--header-bg);
      padding: 0.75rem 1.5rem;
      display: flex;
      align-items: center;
      gap: 1rem;
      position: sticky;
      top: 0;
      z-index: 10;
    }
    header a {
      color: var(--fg);
      text-decoration: none;
      font-weight: 600;
      font-size: 0.95rem;
    }
    header span { color: var(--muted); font-size: 0.85rem; }
    header .sep { color: var(--border); }
    .container {
      max-width: 52rem;
      margin: 0 auto;
      padding: 2.5rem 1.5rem 4rem;
    }
    h1 { font-size: 2rem; font-weight: 700; margin-bottom: 1rem; letter-spacing: -0.02em; }
    h2 { font-size: 1.5rem; font-weight: 600; margin-top: 2.5rem; margin-bottom: 0.75rem; padding-bottom: 0.4rem; border-bottom: 1px solid var(--border); }
    h3 { font-size: 1.2rem; font-weight: 600; margin-top: 2rem; margin-bottom: 0.5rem; }
    h4 { font-size: 1.05rem; font-weight: 600; margin-top: 1.5rem; margin-bottom: 0.5rem; }
    p { margin-bottom: 1rem; }
    a { color: var(--link); text-decoration: none; }
    a:hover { text-decoration: underline; }
    ul, ol { margin-bottom: 1rem; padding-left: 1.5rem; }
    li { margin-bottom: 0.35rem; }
    code {
      font-family: "SF Mono", "Fira Code", monospace;
      font-size: 0.875em;
      background: var(--code-bg);
      padding: 0.15em 0.4em;
      border-radius: 4px;
    }
    pre {
      background: var(--code-bg);
      padding: 1rem 1.25rem;
      border-radius: 8px;
      overflow-x: auto;
      margin-bottom: 1rem;
      border: 1px solid var(--border);
    }
    pre code { background: none; padding: 0; font-size: 0.85rem; }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 1rem;
      font-size: 0.9rem;
    }
    th, td {
      padding: 0.6rem 0.8rem;
      border: 1px solid var(--border);
      text-align: left;
    }
    th { background: var(--code-bg); font-weight: 600; }
    blockquote {
      border-left: 3px solid var(--border);
      padding-left: 1rem;
      color: var(--muted);
      margin-bottom: 1rem;
    }
    hr { border: none; border-top: 1px solid var(--border); margin: 2rem 0; }
    img { max-width: 100%; border-radius: 8px; }
  </style>
</head>
<body>
  <header>
    <a href="/url-short/">URLShort</a>
    <span class="sep">|</span>
    <span>${title}</span>
  </header>
  <div class="container">
    ${content}
  </div>
</body>
</html>`;
}

// Convert all docs/*.md files
const docsDir = join(ROOT, "docs");
const mdFiles = readdirSync(docsDir).filter((f) => f.endsWith(".md"));

let converted = 0;

for (const file of mdFiles) {
  const md = readFileSync(join(docsDir, file), "utf-8");
  const html = marked.parse(md);
  const title = file.replace(".md", "").replace(/_/g, " ");
  const outName = file.replace(".md", ".html");
  writeFileSync(join(PUBLIC_DOCS, outName), htmlTemplate(title, html));
  converted++;
}

// Convert CONTRIBUTING.md (project root → public/CONTRIBUTING.html)
const contributingPath = join(ROOT, "CONTRIBUTING.md");
try {
  const md = readFileSync(contributingPath, "utf-8");
  const html = marked.parse(md);
  writeFileSync(
    join(ROOT, "frontend/public/CONTRIBUTING.html"),
    htmlTemplate("Contributing", html)
  );
  converted++;
} catch {
  console.warn("CONTRIBUTING.md not found, skipping");
}

// Convert docs/FRONTEND.md (docs dir → HTML)
const frontendMdPath = join(docsDir, "FRONTEND.md");
try {
  const md = readFileSync(frontendMdPath, "utf-8");
  const html = marked.parse(md);
  writeFileSync(join(PUBLIC_DOCS, "FRONTEND.html"), htmlTemplate("Frontend", html));
  converted++;
} catch {
  console.warn("FRONTEND.md not found, skipping");
}

console.log(`Converted ${converted} markdown files to HTML`);
