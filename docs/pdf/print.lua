-- Pandoc filter for the print layout (used by build-pdf.mjs).

-- A table whose separator row has equal dashes (|---|---|) was never sized by hand,
-- so let the browser size its columns from their content. Tuned tables keep their widths.
function Table(t)
  local first = t.colspecs[1][2]
  for _, cs in ipairs(t.colspecs) do
    if cs[2] ~= first then return nil end
  end
  for _, cs in ipairs(t.colspecs) do cs[2] = nil end
  return t
end

-- IDs such as FR-13, TC-LED-01 or DEF-001, and dates such as 2026-11-14, never break at their hyphens.
local ID = '^(.-)([%u%d][%u%d]*%-[%u%d%-]*%d)(.*)$'

local function ids(text)
  local pre, id, post = text:match(ID)
  if not id then return { pandoc.Str(text) } end
  local out = { pandoc.Str(pre), pandoc.Span(pandoc.Str(id), { class = 'id' }) }
  for _, el in ipairs(ids(post)) do table.insert(out, el) end
  return out
end

function Str(s)
  if s.text:match(ID) then return ids(s.text) end
end

-- Long identifiers in inline code may break after '.', '#' or '/', never mid-name.
function Code(c)
  local html = c.text:gsub('&', '&amp;'):gsub('<', '&lt;'):gsub('>', '&gt;')
  return pandoc.RawInline('html', '<code>' .. html:gsub('([%./#])', '%1<wbr>') .. '</code>')
end
