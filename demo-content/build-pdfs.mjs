// Renders the Markdown demo documents to PDF with a headless Chromium browser (Edge or Chrome).
//
// Usage:  node demo-content/build-pdfs.mjs [--browser "<path to msedge/chrome>"]
//
// Every *.md file in a sub-folder of demo-content/ is converted. The front matter `pdf` field is the output
// file name; PDFs are written to knowgauge-service-rest-api/src/main/resources/devdocs/.
// Only a small Markdown subset is supported: headings, paragraphs, lists, tables, blockquotes, bold and italics.

import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '..');
const outDir = join(repoRoot, 'knowgauge-service-rest-api', 'src', 'main', 'resources', 'devdocs');

const browserCandidates = [
	process.env.KG_PDF_BROWSER,
	'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
	'C:/Program Files/Google/Chrome/Application/chrome.exe',
	'/usr/bin/chromium',
	'/usr/bin/google-chrome',
	'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
].filter(Boolean);

function findBrowser() {
	const idx = process.argv.indexOf('--browser');
	if (idx > -1 && process.argv[idx + 1]) return process.argv[idx + 1];
	const found = browserCandidates.find((p) => existsSync(p));
	if (!found) throw new Error('No Chromium-based browser found. Pass --browser <path> or set KG_PDF_BROWSER.');
	return found;
}

function escapeHtml(s) {
	return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function inline(s) {
	return escapeHtml(s)
		.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
		.replace(/(^|[^*])\*(?!\s)(.+?)\*/g, '$1<em>$2</em>');
}

function parseFrontMatter(text) {
	const meta = {};
	if (!text.startsWith('---')) return { meta, body: text };
	const end = text.indexOf('\n---', 3);
	for (const line of text.slice(3, end).split('\n')) {
		const m = line.match(/^(\w+):\s*(.*)$/);
		if (m) meta[m[1]] = m[2].trim();
	}
	return { meta, body: text.slice(end + 4) };
}

function markdownToHtml(md) {
	const lines = md.replace(/\r\n/g, '\n').split('\n');
	const out = [];
	let i = 0;
	while (i < lines.length) {
		const line = lines[i];
		if (!line.trim()) { i++; continue; }

		const h = line.match(/^(#{1,3})\s+(.*)$/);
		if (h) { out.push(`<h${h[1].length}>${inline(h[2])}</h${h[1].length}>`); i++; continue; }

		if (/^\s*[-*]\s+/.test(line) || /^\s*\d+\.\s+/.test(line)) {
			const ordered = /^\s*\d+\.\s+/.test(line);
			const items = [];
			while (i < lines.length && (ordered ? /^\s*\d+\.\s+/ : /^\s*[-*]\s+/).test(lines[i])) {
				let item = lines[i].replace(/^\s*([-*]|\d+\.)\s+/, '');
				i++;
				while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*([-*]|\d+\.)\s+/.test(lines[i])) {
					item += ' ' + lines[i].trim();
					i++;
				}
				items.push(`<li>${inline(item)}</li>`);
			}
			const tag = ordered ? 'ol' : 'ul';
			out.push(`<${tag}>${items.join('')}</${tag}>`);
			continue;
		}

		if (line.startsWith('|')) {
			const rows = [];
			while (i < lines.length && lines[i].startsWith('|')) { rows.push(lines[i]); i++; }
			const cells = (r) => r.trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());
			const head = cells(rows[0]);
			const body = rows.slice(2).map(cells);
			out.push('<table><thead><tr>' + head.map((c) => `<th>${inline(c)}</th>`).join('') + '</tr></thead><tbody>' +
				body.map((r) => '<tr>' + r.map((c) => `<td>${inline(c)}</td>`).join('') + '</tr>').join('') +
				'</tbody></table>');
			continue;
		}

		if (line.startsWith('>')) {
			const parts = [];
			while (i < lines.length && lines[i].startsWith('>')) { parts.push(lines[i].replace(/^>\s?/, '')); i++; }
			out.push(`<blockquote>${inline(parts.join(' '))}</blockquote>`);
			continue;
		}

		const para = [];
		while (i < lines.length && lines[i].trim() && !/^(#|>|\||\s*[-*]\s|\s*\d+\.\s)/.test(lines[i])) {
			para.push(lines[i].trim());
			i++;
		}
		out.push(`<p>${inline(para.join(' '))}</p>`);
	}
	return out.join('\n');
}

const css = readFileSync(join(here, 'style.css'), 'utf8');

function page(meta, bodyHtml) {
	const rows = [
		['Document ID', meta.docid],
		['Version', meta.version],
		['Effective date', meta.effective],
		['Owner', meta.owner],
		['Applies to', meta.appliesTo],
	].filter(([, v]) => v);
	return `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>${escapeHtml(meta.title)}</title>
<style>${css}</style></head><body>
<div class="brand">Corvane Systems</div>
<table class="meta">${rows.map(([k, v]) => `<tr><th>${k}</th><td>${escapeHtml(v)}</td></tr>`).join('')}</table>
${bodyHtml}
<p class="disclaimer">Fictional sample document created for the KnowGauge demo. Corvane Systems is not a real
company, and this document is not legal or HR advice.</p>
</body></html>`;
}

const browser = findBrowser();
mkdirSync(outDir, { recursive: true });
const workDir = join(tmpdir(), 'kg-demo-pdfs');
rmSync(workDir, { recursive: true, force: true });
mkdirSync(workDir, { recursive: true });

const sets = readdirSync(here).filter((d) => statSync(join(here, d)).isDirectory());
let count = 0;
for (const set of sets) {
	for (const file of readdirSync(join(here, set)).filter((f) => f.endsWith('.md')).sort()) {
		const { meta, body } = parseFrontMatter(readFileSync(join(here, set, file), 'utf8'));
		if (!meta.pdf) { console.warn(`skip ${set}/${file}: no 'pdf' in front matter`); continue; }
		const htmlPath = join(workDir, file.replace(/\.md$/, '.html'));
		writeFileSync(htmlPath, page(meta, markdownToHtml(body)), 'utf8');
		const pdfPath = join(outDir, meta.pdf);
		execFileSync(browser, ['--headless=new', '--disable-gpu', '--no-pdf-header-footer',
			`--user-data-dir=${join(workDir, 'profile')}`, `--print-to-pdf=${pdfPath}`, pathToFileURL(htmlPath).href],
			{ stdio: 'ignore' });
		console.log(`${set}/${file} -> ${meta.pdf}`);
		count++;
	}
}
console.log(`Rendered ${count} PDF(s) into ${outDir}`);
