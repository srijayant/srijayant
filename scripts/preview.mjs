import { createServer } from "node:http";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { Marked } from "marked";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const README = join(ROOT, "README.md");
const GH_CSS = join(ROOT, "node_modules", "github-markdown-css", "github-markdown.css");

const marked = new Marked({ gfm: true, breaks: false });

async function renderPage() {
  const [md, css] = await Promise.all([
    readFile(README, "utf8"),
    readFile(GH_CSS, "utf8").catch(() => ""),
  ]);
  const body = marked.parse(md);
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>README preview</title>
<style>${css}
body { box-sizing: border-box; margin: 0 auto; max-width: 980px; padding: 24px; }
</style>
</head>
<body>
<article class="markdown-body">
${body}
</article>
</body>
</html>`;
}

async function build() {
  const html = await renderPage();
  const outDir = join(ROOT, "dist");
  await mkdir(outDir, { recursive: true });
  const outFile = join(outDir, "index.html");
  await writeFile(outFile, html, "utf8");
  console.log(`Rendered README.md -> ${outFile}`);
}

async function serve() {
  const port = Number(process.env.PORT) || 3000;
  const host = process.env.HOST || "0.0.0.0";
  const server = createServer(async (req, res) => {
    try {
      // Re-read and re-render on every request for a live preview.
      const html = await renderPage();
      res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
      res.end(html);
    } catch (err) {
      res.writeHead(500, { "content-type": "text/plain; charset=utf-8" });
      res.end(`Failed to render README: ${err.message}`);
    }
  });
  server.listen(port, host, () => {
    console.log(`README preview running at http://${host}:${port}`);
  });
}

if (process.argv.includes("--build")) {
  await build();
} else {
  await serve();
}
