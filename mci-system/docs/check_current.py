# -*- coding: utf-8 -*-
"""
Enriches chapters_part1.py, chapters_part2.py, and chapters_part3.py to reach
~13,000 words, calibrated so that Microsoft Word produces ~42 pages on this host
and exactly ~50 pages on the user's Word environment.
"""

import sys
sys.path.append(r"D:\java_project\mci-system\docs")
import assemble_master

# Let's inspect where additional rich content can be injected
print("Current words:", len(open(r"D:\java_project\mci-system\docs\project_report.txt", "r", encoding="utf-8").read().split()))
