// Builds the milestone submission PDFs.
//
//   npm install          (once, inside docs/)
//   npm run build        build both documents
//   npm run build se423  build one of them
//
// Pipeline: markdown → HTML (pandoc) → paged PDF (Paged.js on headless Chromium).
// Mermaid diagrams are rendered to SVG with mermaid-cli and cached by content hash.
// Needs pandoc on PATH; everything else comes from node_modules. To print with an
// installed Chrome instead of the downloaded one, set PUPPETEER_EXECUTABLE_PATH.

import { readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

process.chdir(path.dirname(fileURLToPath(import.meta.url)));

const TEAM = [
  { name: 'Muhammad Ibrahim', reg: '2023446', lead: true },
  { name: 'Hassan Khalid', reg: '2023242' },
  { name: 'Tughral Hussain', reg: '2023532' },
];

const DOCS = {
  se423: {
    src: 'M1-SE423-Proposal-OOD-Pattern-Plan.md',
    out: 'M1-SE423-MicroLend.pdf',
    code: 'SE423',
    course: 'Software Construction and Development',
    title: 'Proposal, Object-Oriented Design & Pattern Plan',
    roles: ['origination · delinquency', 'products · reporting', 'ledger · web layer'],
  },
  se431: {
    src: 'M1-SE431-Quality-Requirements-RTM-SQAP.md',
    out: 'M1-SE431-MicroLend.pdf',
    code: 'SE431',
    course: 'Software Quality Assurance',
    title: 'Quality Requirements, RTM & SQA Plan',
    roles: ['SQAP outline · risk register', 'Quality requirements · RTM', 'Standards · Cost of Quality'],
  },
};

// Run a package's CLI through node itself, so no shell is involved on any OS.
function cli(pkg, args) {
  const pj = JSON.parse(readFileSync(`node_modules/${pkg}/package.json`, 'utf8'));
  const bin = typeof pj.bin === 'string' ? pj.bin : Object.values(pj.bin)[0];
  execFileSync(process.execPath, [path.join('node_modules', pkg, bin), ...args], { stdio: 'inherit' });
}

const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function renderDiagrams(md, key) {
  const config = readFileSync('mermaid-config.json', 'utf8');
  let i = 0;
  return md.replace(/```mermaid\r?\n([\s\S]*?)```/g, (_, src) => {
    const file = `assets/${key}-fig${++i}.svg`;
    const hash = createHash('sha1').update(config + src).digest('hex').slice(0, 12);
    const stamp = `${file}.sha1`;
    if (!existsSync(file) || !existsSync(stamp) || readFileSync(stamp, 'utf8') !== hash) {
      writeFileSync('diagram.tmp.mmd', src);
      cli('@mermaid-js/mermaid-cli', ['-i', 'diagram.tmp.mmd', '-o', file, '-c', 'mermaid-config.json', '-b', 'white', '-q']);
      rmSync('diagram.tmp.mmd');
      writeFileSync(stamp, hash);
    }
    return `\n<figure class="diagram"><img src="${file}" alt="Diagram ${i}"></figure>\n`;
  });
}

function cover(doc) {
  const members = TEAM.map((m, i) => `
      <div class="member">
        <div class="member-name">${m.name}${m.lead ? '<span class="badge">Team Lead</span>' : ''}</div>
        <div class="member-meta">${m.reg} · ${esc(doc.roles[i])}</div>
      </div>`).join('');
  return `
<section class="cover">
  <div class="cover-glow"></div>
  <header class="cover-top">
    <img class="crest" src="assets/giki-logo.jpg" alt="GIKI crest">
    <div class="institute">
      <div>Ghulam Ishaq Khan Institute of Engineering Sciences and Technology</div>
      <div class="faculty">Faculty of Computer Science &amp; Engineering</div>
    </div>
    <span class="pill">Fall 2026</span>
  </header>
  <div class="cover-main">
    <div class="eyebrow"><span>${doc.code}</span> ${doc.course}</div>
    <h1 class="cover-title">MicroLend</h1>
    <p class="cover-sub">Community Microfinance Loan Servicing and Delinquency Engine</p>
    <div class="cover-doc"><span class="ms">Milestone 1 · Week 6</span>${esc(doc.title)}</div>
  </div>
  <div class="cover-team">
    <div class="label">Team Fable</div>
    <div class="members">${members}
    </div>
  </div>
  <footer class="cover-foot">
    <div><span class="label">Instructor</span>Dr. Mian Muaz Razaq</div>
    <div><span class="label">Repository</span>github.com/tughral1/microlend</div>
  </footer>
</section>`;
}

function build(key) {
  const doc = DOCS[key];
  let md = readFileSync(doc.src, 'utf8').replace(/\r\n/g, '\n');
  md = md.replace(/^# .*\n/, '');                                   // the cover carries the title
  md = renderDiagrams(md, key);
  md = md.replace(/^\*\*(View \d+ — .*)\*\*\s*$/gm, '#### $1 {.view}'); // diagram titles start a page
  md = md.replace(/^(#{2,3}) (\d+(?:\.\d+)*)\.? (.+)$/gm, '$1 [$2]{.num} $3'); // number badges
  const log = readFileSync('../qa/m1-ai-usage-log.md', 'utf8').replace(/\r\n/g, '\n')
    .replace(/^# .*\n/, '# Annex A — AI Usage Log {.annex}\n');
  writeFileSync(`${key}.tmp.md`, `${md}\n\n${log}`);
  writeFileSync(`${key}.cover.tmp.html`, cover(doc));

  execFileSync('pandoc', [
    `${key}.tmp.md`, '-f', 'markdown+pipe_tables+bracketed_spans+header_attributes',
    '-t', 'html5', '--standalone', '--template', 'pdf/template.html',
    '--toc', '--toc-depth=2', '--include-before-body', `${key}.cover.tmp.html`,
    '-M', `pagetitle=MicroLend — ${doc.code} Milestone 1`, '-V', `code=${doc.code}`,
    '-o', `${key}.tmp.html`,
  ], { stdio: 'inherit' });

  cli('pagedjs-cli', [`${key}.tmp.html`, '-o', doc.out, '--timeout', '120000']);
  for (const f of [`${key}.tmp.md`, `${key}.cover.tmp.html`, `${key}.tmp.html`]) rmSync(f);
  console.log(`built ${doc.out}`);
}

const which = process.argv[2];
for (const key of which ? [which] : Object.keys(DOCS)) {
  if (!DOCS[key]) { console.error(`usage: npm run build [-- ${Object.keys(DOCS).join('|')}]`); process.exit(1); }
  build(key);
}
