// Builds the milestone submission PDFs.
//
//   npm install          (once, inside docs/)
//   npm run build                build the current milestone's two documents (M2)
//   npm run build -- m2-se431    build one document; M1 keys are se423 and se431
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

const M1 = { milestone: 'Milestone 1 · Week 6', short: 'Milestone 1', log: '../qa/m1-ai-usage-log.md' };
const M2 = { milestone: 'Milestone 2 · Week 10', short: 'Milestone 2', log: '../qa/m2-ai-usage-log.md' };

const DOCS = {
  se423: {
    ...M1,
    src: 'M1-SE423-Proposal-OOD-Pattern-Plan.md',
    out: 'M1-SE423-MicroLend.pdf',
    code: 'SE423',
    course: 'Software Construction and Development',
    title: 'Proposal, Object-Oriented Design & Pattern Plan',
    roles: ['origination · delinquency', 'products · reporting', 'ledger · web layer'],
  },
  se431: {
    ...M1,
    src: 'M1-SE431-Quality-Requirements-RTM-SQAP.md',
    out: 'M1-SE431-MicroLend.pdf',
    code: 'SE431',
    course: 'Software Quality Assurance',
    title: 'Quality Requirements, RTM & SQA Plan',
    roles: ['SQAP outline · risk register', 'Quality requirements · RTM', 'Standards · Cost of Quality'],
  },
  'm2-se423': {
    ...M2,
    src: 'M2-SE423-Architecture-Implementation.md',
    out: 'M2-SE423-MicroLend.pdf',
    code: 'SE423',
    course: 'Software Construction and Development',
    title: 'Working Architecture, Patterns, Concurrency & Test Baseline',
    roles: ['origination · delinquency · importing', 'products · reporting', 'ledger · persistence · web'],
  },
  'm2-se431': {
    ...M2,
    src: 'M2-SE431-Verification-Baseline.md',
    out: 'M2-SE431-MicroLend.pdf',
    code: 'SE431',
    course: 'Software Quality Assurance',
    title: 'Inspection, Static Analysis, V&V Plan, Test Design & SCM Baseline',
    roles: ['M2 document lead', 'products · reporting', 'ledger · persistence · web'],
  },
};

// `npm run build` with no argument builds the current milestone only, so the
// submitted M1 PDFs are not regenerated. Name a key to build any document.
const CURRENT = ['m2-se423', 'm2-se431'];

// A line `<!-- include N path -->` is replaced by that markdown file, so an artifact
// kept under qa/ is submitted from its single source instead of a pasted copy.
// Its title becomes section N and its headings move down one level, with their
// numbers prefixed by N (`## 3. Scope` becomes `### N.3 Scope`).
function expandIncludes(md) {
  return md.replace(/^<!-- include (\d+) (\S+) -->$/gm, (_, n, file) => {
    const body = readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
    const lines = body.split('\n');
    const title = lines[0].replace(/^# (MicroLend\s*[-—]\s*)?/, '').trim();
    let inFence = false;
    const rest = lines.slice(1).map(line => {
      if (/^```/.test(line)) inFence = !inFence;
      if (inFence) return line;
      const m = line.match(/^(#{2,5}) (?:(\d+(?:\.\d+)*)\.? )?(.+)$/);
      if (!m) return line;
      return `#${m[1]} ${m[2] ? `${n}.${m[2]} ` : ''}${m[3]}`;
    });
    return `## ${n}. ${title}\n${rest.join('\n')}`;
  });
}

// Run a package's CLI through node itself, so no shell is involved on any OS.
function cli(pkg, args) {
  const pj = JSON.parse(readFileSync(`node_modules/${pkg}/package.json`, 'utf8'));
  const bin = typeof pj.bin === 'string' ? pj.bin : Object.values(pj.bin)[0];
  execFileSync(process.execPath, [path.join('node_modules', pkg, bin), ...args], { stdio: 'inherit' });
}

const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function renderDiagrams(md, key) {
  // Line endings are normalised so a Windows checkout (CRLF) hits the same cache as Linux.
  const config = readFileSync('mermaid-config.json', 'utf8').replace(/\r\n/g, '\n');
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
    <div class="cover-doc"><span class="ms">${esc(doc.milestone)}</span>${esc(doc.title)}</div>
  </div>
  <div class="cover-team">
    <div class="label">Team Fable</div>
    <div class="members">${members}
    </div>
  </div>
  <footer class="cover-foot">
    <div><span class="label">Instructor</span>Dr. Mian Muaz Razaq</div>
    <div><span class="label">Repository</span>github.com/MicroLend-SE423/microlend</div>
  </footer>
</section>`;
}

// Pandoc only starts a list after a blank line, so "text:\n- item" would print the
// items inline. Insert that blank line, outside code fences, before the first item.
// Likewise a "**Label:** value" line directly under a text line (a metadata block)
// would join the line above, so end that line with a hard break.
function separateLists(md) {
  let inFence = false;
  const out = [];
  for (const line of md.split('\n')) {
    if (/^```/.test(line)) inFence = !inFence;
    const prev = out.length ? out[out.length - 1] : '';
    const isItem = /^(?:- |\* |\d+\. )/.test(line);
    const prevIsText = prev.trim() !== '' && !/^(?:- |\* |\d+\. |\s|\||>|#|```)/.test(prev);
    if (!inFence && isItem && prevIsText) out.push('');
    if (!inFence && /^\*\*[^*]+:\*\*/.test(line) && prevIsText && !prev.endsWith('\\')) out[out.length - 1] += '\\';
    out.push(line);
  }
  return out.join('\n');
}

function build(key) {
  const doc = DOCS[key];
  let md = readFileSync(doc.src, 'utf8').replace(/\r\n/g, '\n');
  md = md.replace(/^# .*\n/, '');                                   // the cover carries the title
  md = expandIncludes(md);
  md = separateLists(md);
  md = renderDiagrams(md, key);
  md = md.replace(/^\*\*(View \d+ — .*)\*\*\s*$/gm, '#### $1 {.view}'); // diagram titles start a page
  md = md.replace(/^(#{2,3}) (\d+(?:\.\d+)*)\.? (.+)$/gm, '$1 [$2]{.num} $3'); // number badges
  const log = readFileSync(doc.log, 'utf8').replace(/\r\n/g, '\n')
    .replace(/^# .*\n/, '# Annex A — AI Usage Log {.annex}\n');
  writeFileSync(`${key}.tmp.md`, `${md}\n\n${log}`);
  writeFileSync(`${key}.cover.tmp.html`, cover(doc));

  execFileSync('pandoc', [
    `${key}.tmp.md`, '-f', 'markdown+pipe_tables+bracketed_spans+header_attributes',
    '-t', 'html5', '--standalone', '--template', 'pdf/template.html', '--lua-filter', 'pdf/print.lua',
    '--toc', '--toc-depth=2', '--include-before-body', `${key}.cover.tmp.html`,
    '-M', `pagetitle=MicroLend — ${doc.code} ${doc.short}`, '-V', `code=${doc.code}`,
    '-V', `milestone=${doc.short}`,
    '-o', `${key}.tmp.html`,
  ], { stdio: 'inherit' });

  cli('pagedjs-cli', [`${key}.tmp.html`, '-o', doc.out, '--timeout', '120000']);
  for (const f of [`${key}.tmp.md`, `${key}.cover.tmp.html`, `${key}.tmp.html`]) rmSync(f);
  console.log(`built ${doc.out}`);
}

const which = process.argv[2];
for (const key of which ? [which] : CURRENT) {
  if (!DOCS[key]) { console.error(`usage: npm run build [-- ${Object.keys(DOCS).join('|')}]`); process.exit(1); }
  build(key);
}
