#!/usr/bin/env bash
# Build submission PDFs from the milestone Markdown documents.
#
#   ./build-pdf.sh            build both M1 documents
#   ./build-pdf.sh se423      build one of them
#
# Needs: pandoc, xelatex (MiKTeX), node, and either curl (renders Mermaid via
# mermaid.ink) or a local mermaid-cli: set MMDC=/path/to/mmdc to render offline.
# Mermaid code blocks are replaced by pre-rendered PNGs in assets/ before pandoc
# runs; delete assets/<prefix>-fig*.png to force a diagram to re-render.

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
  local src="$1" prefix="$2"
  # Each diagram gets the house theme (mermaid-init.txt) prepended, so the
  # colours match the PDF whichever renderer is used.
  node --no-warnings -e "
    const fs=require('fs');
    const init=fs.readFileSync('mermaid-init.txt','utf8').trim();
    const blocks=[...fs.readFileSync('$src','utf8').matchAll(/\`\`\`mermaid\r?\n([\s\S]*?)\`\`\`/g)].map(m=>m[1]);
    blocks.forEach((b,i)=>fs.writeFileSync('assets/$prefix-fig'+(i+1)+'.mmd', init+'\n'+b));
  "
  for f in assets/"$prefix"-fig*.mmd; do
    [ -e "$f" ] || continue
    local n; n="$(basename "$f" .mmd)"
    if [ ! -f "assets/$n.png" ]; then
      if [ -n "${MMDC:-}" ]; then
        "$MMDC" -i "$f" -o "assets/$n.png" -s 3 -b white -q
      else
        local b64; b64="$(node --no-warnings -e "process.stdout.write(Buffer.from(require('fs').readFileSync('$f')).toString('base64url'))")"
        curl -s -o "assets/$n.png" "https://mermaid.ink/img/$b64?type=png&bgColor=FFFFFF&width=1400"
      fi
    fi
    rm -f "$f"
  done
}

make_titlepage() {    # $1 course line, $2 doc line, $3 course code, $4 output file
  cat > "$4" <<EOF
\\gdef\\coursecode{$3}
\\begin{titlepage}
\\thispagestyle{empty}
\\begin{tikzpicture}[remember picture,overlay]
  \\fill[brandnavy] (current page.north west) rectangle ([yshift=-1.6cm]current page.north east);
  \\fill[brandgold] ([yshift=-1.6cm]current page.north west) rectangle ([yshift=-1.75cm]current page.north east);
  \\fill[brandnavy] (current page.south west) rectangle ([yshift=1.1cm]current page.south east);
  \\node[anchor=west, text=white, font=\\sffamily\\small] at ([xshift=2cm,yshift=-0.8cm]current page.north west) {Ghulam Ishaq Khan Institute of Engineering Sciences and Technology};
  \\node[anchor=east, text=brandgold, font=\\sffamily\\small\\bfseries] at ([xshift=-2cm,yshift=-0.8cm]current page.north east) {Fall 2026};
  \\node[text=white, font=\\sffamily\\footnotesize] at ([yshift=0.55cm]current page.south) {Faculty of Computer Science \\& Engineering \\textperiodcentered\\ Team Fable};
\\end{tikzpicture}
\\centering
\\vspace*{1.4cm}
\\includegraphics[width=0.24\\textwidth]{assets/giki-logo.jpg}\\\\[1.4cm]
{\\sffamily\\fontsize{40}{44}\\selectfont\\bfseries\\color{brandnavy} $TITLE_423}\\\\[0.35cm]
{\\large\\itshape\\color{brandslate} $SUB_423}\\\\[1.1cm]
{\\color{brandgold}\\rule{0.18\\textwidth}{2pt}}\\\\[0.9cm]
{\\sffamily\\Large\\bfseries\\color{brandmaroon} $1}\\\\[0.35cm]
{\\large $2}\\\\[1.6cm]
{\\sffamily\\small\\bfseries\\color{brandslate} SUBMITTED BY}\\\\[0.4cm]
\\renewcommand{\\arraystretch}{1.5}
\\begin{tabular}{@{}l@{\\hspace{1.4cm}}r@{}}
\\large Muhammad Ibrahim \\textcolor{brandgold}{\\small\\sffamily\\bfseries\\ \\ TEAM LEAD} & \\large\\sffamily\\color{brandslate} 2023446 \\\\
\\large Hassan Khalid    & \\large\\sffamily\\color{brandslate} 2023242 \\\\
\\large Tughral Hussain  & \\large\\sffamily\\color{brandslate} 2023532 \\\\
\\end{tabular}\\\\[1.6cm]
{\\sffamily\\small\\bfseries\\color{brandslate} INSTRUCTOR}\\\\[0.25cm]
{\\large Dr.\\ Mian Muaz Razaq}
\\vfill
\\end{titlepage}
EOF
}

build() {             # $1 source md, $2 asset prefix, $3 course line, $4 doc line, $5 course code, $6 output pdf
  render_diagrams "$1" "$2"
  local body="build-$2.md"
  node --no-warnings -e "
    const fs=require('fs');
    let md=fs.readFileSync('$1','utf8'), i=0;
    md=md.replace(/\`\`\`mermaid\r?\n[\s\S]*?\`\`\`/g, () => '![](assets/$2-fig'+(++i)+'.png)');
    md=md.replace(/^# .*\r?\n/, '');                    // title is on the title page
    const log = fs.readFileSync('../qa/m1-ai-usage-log.md','utf8')
      .replace(/^# .*\r?\n/, '# Annex A — AI Usage Log\n');
    md = md + '\n\n\\\\newpage\n\n' + log;
    fs.writeFileSync('$body', md);
  "
  make_titlepage "$3" "$4" "$5" "titlepage-$2.tex"
  pandoc "$body" \
    --from=markdown+pipe_tables+yaml_metadata_block \
    --pdf-engine=xelatex \
    --lua-filter=pdf-style.lua \
    --include-in-header=header.tex \
    --include-before-body="titlepage-$2.tex" \
    --toc --toc-depth=2 \
    -V subparagraph=yes \
    -V geometry:a4paper,margin=2cm,top=2.4cm,bottom=2.4cm \
    -V fontsize=10pt \
    -V mainfont="Cambria" \
    -V sansfont="Segoe UI" \
    -V monofont="Consolas" \
    -V colorlinks=true -V linkcolor=black -V urlcolor=blue \
    -o "$6"
  rm -f "$body" "titlepage-$2.tex"
  echo "built $6"
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
\usepackage{etoolbox}
\usepackage{tikz}
\usepackage[most]{tcolorbox}
\usepackage{titlesec}

% House palette, taken from the GIKI crest
\definecolor{brandnavy}{HTML}{1F2A5A}
\definecolor{brandmaroon}{HTML}{7A1F2B}
\definecolor{brandgold}{HTML}{B8913A}
\definecolor{brandslate}{HTML}{5A6178}
\definecolor{rowtint}{HTML}{F2F4F9}
\definecolor{codebg}{HTML}{F5F6FA}
\providecommand{\coursecode}{}

% Headings: sans-serif in navy; top-level sections carry a gold rule
\titleformat{\section}{\sffamily\LARGE\bfseries\color{brandnavy}}{}{0pt}{}[{\color{brandgold}\titlerule[1.2pt]}]
\titleformat{\subsection}{\sffamily\Large\bfseries\color{brandnavy}}{}{0pt}{}[{\color{brandgold}\titlerule[0.8pt]}]
\titleformat{\subsubsection}{\sffamily\large\bfseries\color{brandmaroon}}{}{0pt}{}
\titlespacing*{\subsection}{0pt}{18pt}{8pt}
\titlespacing*{\subsubsection}{0pt}{12pt}{4pt}

% Running header and footer
\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\small\sffamily\textcolor{brandnavy}{\textbf{MicroLend}}\ \textcolor{brandgold}{|}\ \textcolor{brandslate}{\coursecode\ \textperiodcentered\ Milestone 1}}
\fancyhead[R]{\small\sffamily\textcolor{brandslate}{Team Fable}}
\fancyfoot[C]{\small\sffamily\textcolor{brandnavy}{\thepage}}
\renewcommand{\headrulewidth}{0.6pt}
\renewcommand{\headrule}{{\color{brandgold}\hrule width\headwidth height\headrulewidth}\vskip-\headrulewidth}
\fancypagestyle{plain}{\fancyhf{}\fancyfoot[C]{\small\sffamily\textcolor{brandnavy}{\thepage}}\renewcommand{\headrulewidth}{0pt}}

% Tables: navy header row (white text is set by pdf-style.lua), zebra rows,
% navy rules with no white gaps around the coloured rows
\setlength{\LTleft}{0pt}\setlength{\LTright}{0pt}
\setlength{\aboverulesep}{0pt}\setlength{\belowrulesep}{0pt}
\setlength{\heavyrulewidth}{0.9pt}\setlength{\lightrulewidth}{0.5pt}
\arrayrulecolor{brandnavy}
\let\oldtoprule\toprule
\renewcommand{\toprule}{\oldtoprule\rowcolor{brandnavy}}
\AtBeginEnvironment{longtable}{\small\RaggedRight\renewcommand{\arraystretch}{1.35}\rowcolors{2}{rowtint}{white}}

% Code blocks: tinted panel with a navy rule on the left
\tcbset{codebox/.style={enhanced, colback=codebg, colframe=brandnavy, boxrule=0pt,
  leftrule=2.5pt, sharp corners, left=7pt, right=7pt, top=5pt, bottom=5pt, before skip=8pt, after skip=8pt}}
\BeforeBeginEnvironment{verbatim}{\begin{tcolorbox}[codebox]\small}
\AfterEndEnvironment{verbatim}{\end{tcolorbox}}
\AtBeginDocument{\ifcsname Shaded\endcsname\renewenvironment{Shaded}{\begin{tcolorbox}[codebox]\small}{\end{tcolorbox}}\fi}

% Block quotes (the declaration): gold rule on the left
\renewenvironment{quote}{\begin{tcolorbox}[enhanced, colback=white, colframe=brandgold, boxrule=0pt, leftrule=2pt, sharp corners, left=8pt, top=3pt, bottom=3pt]\itshape}{\end{tcolorbox}}

% images never wider than the text block
\setkeys{Gin}{width=\linewidth,keepaspectratio}
EOF

case "${1:-both}" in
  se423) build "$SRC_423" se423 "$COURSE_423" "$DOC_423" "SE423" "M1-SE423-MicroLend.pdf" ;;
  se431) build "$SRC_431" se431 "$COURSE_431" "$DOC_431" "SE431" "M1-SE431-MicroLend.pdf" ;;
  both)
    build "$SRC_423" se423 "$COURSE_423" "$DOC_423" "SE423" "M1-SE423-MicroLend.pdf"
    build "$SRC_431" se431 "$COURSE_431" "$DOC_431" "SE431" "M1-SE431-MicroLend.pdf"
    ;;
  *) echo "usage: $0 [se423|se431|both]" >&2; exit 1 ;;
esac
