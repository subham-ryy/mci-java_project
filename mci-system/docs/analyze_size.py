import docx

doc = docx.Document(r"D:\java_project\mci-system\docs\MCI_System_Project_Report.docx")
print("Total paragraphs:", len(doc.paragraphs))
print("Total tables:", len(doc.tables))

# Count words
word_count = sum(len(p.text.split()) for p in doc.paragraphs)
print("Total paragraph words:", word_count)

# Let's see the lengths of tables
table_cells = sum(len(t.rows) * len(t.columns) for t in doc.tables)
print("Total table cells:", table_cells)

# Let's inspect headings
headings = [p.text for p in doc.paragraphs if p.text.startswith("CHAPTER") or p.text.startswith("APPENDIX") or p.text == "REFERENCES"]
print(f"Headings count: {len(headings)}")
