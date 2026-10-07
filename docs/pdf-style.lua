-- Each "View N — ..." diagram heading starts a new page, so it is never
-- stranded at the foot of a page with its diagram on the next one.
function Para(p)
  if p.content[1] and p.content[1].t == "Strong"
     and pandoc.utils.stringify(p.content[1]):match("^View %d") then
    return { pandoc.RawBlock("latex", "\\clearpage"), p }
  end
end

-- Header cells are printed white and bold; header.tex paints the header row navy.
function Table(t)
  for _, row in ipairs(t.head.rows) do
    for _, cell in ipairs(row.cells) do
      for _, block in ipairs(cell.contents) do
        if block.t == "Plain" or block.t == "Para" then
          table.insert(block.content, 1, pandoc.RawInline("latex", "\\color{white}\\bfseries\\sffamily "))
        end
      end
    end
  end
  return t
end
