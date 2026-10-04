#!/usr/bin/env bash
# Build submission PDFs from the milestone Markdown documents.
#
#   ./build-pdf.sh            build both M1 documents
#   ./build-pdf.sh se423      build one of them
#
# Needs: pandoc, xelatex (MiKTeX), curl (to render Mermaid diagrams via mermaid.ink).
# Mermaid code blocks are replaced by pre-rendered PNGs in assets/ before pandoc runs.

set -euo pipefail
cd "$(dirname "$0")"

TITLE_423="MicroLend"
SUB_423="Community Microfinance Loan Servicing and Delinquency Engine"
COURSE_423="SE423 — Software Construction and Development"
DOC_423="Milestone 1 — Proposal, Object-Oriented Design and Pattern Plan"
SRC_423="M1-SE423-Proposal-OOD-Pattern-Plan.md"

COURSE_431="SE431 — Software Quality Assurance"
DOC_431="Milestone 1 — Quality Requirements, RTM and SQA Plan"
SRC_431="M1-SE431-Quality-Requirements-RTM-SQAP.md"

render_diagrams() {   # $1 = source markdown, $2 = asset prefix
  local src="$1" prefix="$2" i=1
  node --no-warnings -e "
    const fs=require('fs');
    const blocks=[...fs.readFileSync('$src','utf8').matchAll(/\`\`\`mermaid\n([\s\S]*?)\`\`\`/g)].map(m=>m[1]);
    blocks.forEach((b,i)=>fs.writeFileSync('assets/$prefix-fig'+(i+1)+'.b64', Buffer.from(b).toString('base64url')));
  "
  for f in assets/"$prefix"-fig*.b64; do
    [ -e "$f" ] || continue
    local n; n="$(basename "$f" .b64)"
    [ -f "assets/$n.png" ] || curl -s -o "assets/$n.png" \
      "https://mermaid.ink/img/$(cat "$f")?type=png&bgColor=FFFFFF&width=1400"
    rm -f "$f"
  done
}

make_titlepage() {    # $1 course line, $2 doc line, $3 output file
  cat > "$3" <<EOF
\\begin{titlepage}
\\centering
\\vspace*{1.2cm}
\\includegraphics[width=0.30\\textwidth]{assets/giki-logo.jpg}\\\\[0.9cm]
{\\large Ghulam Ishaq Khan Institute of Engineering Sciences and Technology}\\\\[0.25cm]
{\\normalsize Faculty of Computer Science \& Engineering}\\\\[1.6cm]
{\\Huge\\bfseries $TITLE_423}\\\\[0.5cm]
{\\large $SUB_423}\\\\[1.8cm]
\\rule{0.8\\textwidth}{0.4pt}\\\\[0.7cm]
{\\Large\\bfseries $1}\\\\[0.5cm]
{\\large $2}\\\\[0.5cm]
\\rule{0.8\\textwidth}{0.4pt}\\\\[1.8cm]
{\\large\\bfseries Submitted by}\\\\[0.7cm]
\\begin{tabular}{ll}
\\large Muhammad Ibrahim & \\large 2023446 \\\\[0.25cm]
\\large Hassan Khalid    & \\large 2023242 \\\\[0.25cm]
\\large Tughral Hussain  & \\large 2023532 \\\\
\\end{tabular}\\\\[1.8cm]
{\\large Instructor: Dr. Mian Muaz Razaq}\\\\[0.4cm]
{\\large Fall 2026}
\\vfill
\\end{titlepage}
EOF
}

build() {             # $1 source md, $2 asset prefix, $3 course line, $4 doc line, $5 output pdf
  render_diagrams "$1" "$2"
  local body="build-$2.md"
  node --no-warnings -e "
    const fs=require('fs');
    let md=fs.readFileSync('$1','utf8'), i=0;
    md=md.replace(/\`\`\`mermaid\n[\s\S]*?\`\`\`/g, () => '![](assets/$2-fig'+(++i)+'.png)');
    md=md.replace(/^# .*\n/, '');                       // title is on the title page
    const log = fs.readFileSync('../qa/ai-usage-log.md','utf8')
      .replace(/^# .*\n/, '# Annex A — AI Usage Log\n');
    md = md + '\n\n\\\\newpage\n\n' + log;
    fs.writeFileSync('$body', md);
  "
  make_titlepage "$3" "$4" "titlepage-$2.tex"
  pandoc "$body" \
    --from=markdown+pipe_tables+yaml_metadata_block \
    --pdf-engine=xelatex \
    --include-in-header=header.tex \
    --include-before-body="titlepage-$2.tex" \
    --toc --toc-depth=2 \
    -V geometry:a4paper,margin=2cm \
    -V fontsize=10pt \
    -V mainfont="Times New Roman" \
    -V monofont="Consolas" \
    -V colorlinks=true -V linkcolor=black -V urlcolor=blue \
    -o "$5"
  rm -f "$body" "titlepage-$2.tex"
  echo "built $5"
}

cat > header.tex <<'EOF'
\usepackage{graphicx}
\usepackage{amssymb}
\usepackage{newunicodechar}
\newunicodechar{✔}{\ensuremath{\checkmark}}
\newunicodechar{→}{\ensuremath{\rightarrow}}
\newunicodechar{←}{\ensuremath{\leftarrow}}
\newunicodechar{≤}{\ensuremath{\leq}}
\newunicodechar{≥}{\ensuremath{\geq}}
\newunicodechar{≈}{\ensuremath{\approx}}
\newunicodechar{×}{\ensuremath{\times}}
\newunicodechar{Σ}{\ensuremath{\Sigma}}
\usepackage{longtable,booktabs,array}
\usepackage{ragged2e}
\usepackage{fancyhdr}
\usepackage[table]{xcolor}
\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\small MicroLend --- Milestone 1}
\fancyhead[R]{\small\thepage}
\renewcommand{\headrulewidth}{0.4pt}
% keep wide tables inside the margins
\setlength{\LTleft}{0pt}\setlength{\LTright}{0pt}
\AtBeginEnvironment{longtable}{\small\RaggedRight}
\usepackage{etoolbox}
% images never wider than the text block
\setkeys{Gin}{width=\linewidth,keepaspectratio}
EOF

case "${1:-both}" in
  se423) build "$SRC_423" se423 "$COURSE_423" "$DOC_423" "M1-SE423-MicroLend.pdf" ;;
  se431) build "$SRC_431" se431 "$COURSE_431" "$DOC_431" "M1-SE431-MicroLend.pdf" ;;
  both)
    build "$SRC_423" se423 "$COURSE_423" "$DOC_423" "M1-SE423-MicroLend.pdf"
    build "$SRC_431" se431 "$COURSE_431" "$DOC_431" "M1-SE431-MicroLend.pdf"
    ;;
  *) echo "usage: $0 [se423|se431|both]" >&2; exit 1 ;;
esac
