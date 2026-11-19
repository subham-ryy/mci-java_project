# -*- coding: utf-8 -*-
import sys
import os

sys.path.append(r"D:\java_project\mci-system\docs")
import assemble_report
import expand_ch8_14

def main():
    parts = []
    
    # 0. Front matter
    parts.append("""MULTI-AGENT COMPUTER INTELLIGENCE (MCI) SYSTEM:
AN AUTONOMOUS ZERO-TRUST MULTI-AGENT TASK ORCHESTRATION PLATFORM IN JAVA 21

SUBMITTED BY:
1. SUBHAM ROY – AP25110080092
2. SHREYAS POONDOTA – AP25110080064
3. MUHAMMAD ISMAIL – AP25110080094
4. UTKARSH NAYAN – AP25110080089

COURSE: ADVANCED JAVA PROGRAMMING (CSE 213)
COURSE INSTRUCTOR: PRANEETHA SURAPANENI, ASSISTANT PROFESSOR
DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING
SRM UNIVERSITY, AP, AMARAVATI – 522 240, ANDHRA PRADESH, INDIA
NOVEMBER 2026
""")

    # Chapters 1 to 7 from assemble_report
    # Let's extract chapters 1-7 from assemble_report
    old_parts = assemble_report.build_full_report().split("\n\nCHAPTER ")
    parts.append(old_parts[1]) # Chapter 1
    parts.append("CHAPTER " + old_parts[2]) # Chapter 2
    parts.append("CHAPTER " + old_parts[3]) # Chapter 3
    parts.append("CHAPTER " + old_parts[4]) # Chapter 4
    parts.append("CHAPTER " + old_parts[5]) # Chapter 5
    parts.append("CHAPTER " + old_parts[6]) # Chapter 6
    parts.append("CHAPTER " + old_parts[7]) # Chapter 7

    # Chapters 8 to 14 from expand_ch8_14
    parts.append(expand_ch8_14.get_chapter_8())
    parts.append(expand_ch8_14.get_chapter_9())
    parts.append(expand_ch8_14.get_chapter_10())
    parts.append(expand_ch8_14.get_chapter_11())
    parts.append(expand_ch8_14.get_chapter_12())
    parts.append(expand_ch8_14.get_chapter_13())
    parts.append(expand_ch8_14.get_chapter_14())

    # References & Appendices
    # In old_parts, index 14 had Chapter 14 + References + Appendices
    tail = old_parts[14]
    ref_idx = tail.find("REFERENCES\n\n")
    if ref_idx != -1:
        tail_refs_and_app = tail[ref_idx:]
        parts.append(tail_refs_and_app)

    full_text = "\n\n".join(parts)
    out_file = r"D:\java_project\mci-system\docs\project_report.txt"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(full_text)

    words = len(full_text.split())
    lines = len(full_text.splitlines())
    print(f"Generated complete report: {words} words, {lines} lines")

if __name__ == "__main__":
    main()
