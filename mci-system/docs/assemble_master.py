# -*- coding: utf-8 -*-
import sys
import os

sys.path.append(r"D:\java_project\mci-system\docs")
import chapters_part1
import chapters_part2
import chapters_part3

def main():
    parts = []
    
    # Part 1: Front Matter + Chapters 1-4
    parts.append(chapters_part1.get_front_matter())
    parts.append(chapters_part1.get_chapter_1())
    parts.append(chapters_part1.get_chapter_2())
    parts.append(chapters_part1.get_chapter_3())
    parts.append(chapters_part1.get_chapter_4())

    # Part 2: Chapters 5-9
    parts.append(chapters_part2.get_chapter_5())
    parts.append(chapters_part2.get_chapter_6())
    parts.append(chapters_part2.get_chapter_7())
    parts.append(chapters_part2.get_chapter_8())
    parts.append(chapters_part2.get_chapter_9())

    # Part 3: Chapters 10-14 + References + Appendices
    parts.append(chapters_part3.get_chapter_10())
    parts.append(chapters_part3.get_chapter_11())
    parts.append(chapters_part3.get_chapter_12())
    parts.append(chapters_part3.get_chapter_13())
    parts.append(chapters_part3.get_chapter_14())
    parts.append(chapters_part3.get_references())
    parts.append(chapters_part3.get_appendices())

    full_text = "\n\n".join(parts)
    out_file = r"D:\java_project\mci-system\docs\project_report.txt"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(full_text)

    words = len(full_text.split())
    lines = len(full_text.splitlines())
    print(f"Master report written to: {out_file}")
    print(f"Total Words: {words:,}, Total Lines: {lines:,}")

if __name__ == "__main__":
    main()
