import os
import re
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def add_styled_paragraph(doc, text="", style='Normal', space_before=0, space_after=4, line_spacing=1.15, align=WD_ALIGN_PARAGRAPH.LEFT):
    p = doc.add_paragraph(text, style=style)
    p.alignment = align
    p.paragraph_format.space_before = Pt(space_before)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.line_spacing = line_spacing
    return p

def main():
    import sys
    if len(sys.argv) > 1 and os.path.exists(sys.argv[1]):
        report_txt_path = sys.argv[1]
    else:
        report_txt_path = r"D:\java_project\mci-system\docs\project_report.txt"
    logo_path = r"D:\java_project\mci-system\docs\srm_logo.png"
    asset_path = r"D:\java_project\mci-system\frontend\asset.png"
    dashboard_path = r"D:\java_project\mci-system\frontend\screenshot_matched_bg.png"
    output_docx_path = r"D:\java_project\mci-system\docs\MCI_System_Project_Report.docx"

    with open(report_txt_path, "r", encoding="utf-8") as f:
        full_text = f.read()

    doc = Document()

    # Page setup - A4 with 1-inch margins
    sections = doc.sections
    for section in sections:
        section.page_width = Inches(8.27)
        section.page_height = Inches(11.69)
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        section.different_first_page_header_footer = True

        # Header for pages 2+
        header = section.header
        p_hdr = header.paragraphs[0]
        p_hdr.text = "Multi-Agent Computer Intelligence (MCI) System  |  CSE 213: Advanced Java Programming"
        p_hdr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        if p_hdr.runs:
            p_hdr.runs[0].font.name = 'Times New Roman'
            p_hdr.runs[0].font.size = Pt(8.5)
            p_hdr.runs[0].font.italic = True
            p_hdr.runs[0].font.color.rgb = RGBColor(0x88, 0x88, 0x88)

        # Footer for pages 2+
        footer = section.footer
        p_ftr = footer.paragraphs[0]
        p_ftr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        r_ftr = p_ftr.add_run("SRM University, AP  —  Department of CSE                           Page ")
        r_ftr.font.name = 'Times New Roman'
        r_ftr.font.size = Pt(8.5)
        r_ftr.font.color.rgb = RGBColor(0x77, 0x77, 0x77)

        fldSimple = OxmlElement('w:fldSimple')
        fldSimple.set(qn('w:instr'), 'PAGE')
        p_ftr._p.append(fldSimple)

    # Base font setup
    style_normal = doc.styles['Normal']
    font_normal = style_normal.font
    font_normal.name = 'Times New Roman'
    font_normal.size = Pt(11)
    font_normal.color.rgb = RGBColor(0x22, 0x22, 0x22)

    # -------------------------------------------------------------
    # 1. COVER PAGE (Matching Template Exactly)
    # -------------------------------------------------------------
    p_top = add_styled_paragraph(doc, "A REPORT ON", space_before=10, space_after=12, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_top.runs[0].font.bold = True
    p_top.runs[0].font.size = Pt(14)
    p_top.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    p_title = add_styled_paragraph(doc, "MULTI-AGENT COMPUTER INTELLIGENCE (MCI) SYSTEM:\nAN AUTONOMOUS ZERO-TRUST MULTI-AGENT TASK ORCHESTRATION PLATFORM IN JAVA 21", space_before=4, space_after=16, line_spacing=1.2, align=WD_ALIGN_PARAGRAPH.CENTER)
    for r in p_title.runs:
        r.font.bold = True
        r.font.size = Pt(16)
        r.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)

    p_by = add_styled_paragraph(doc, "BY", space_before=4, space_after=8, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_by.runs[0].font.bold = True
    p_by.runs[0].font.size = Pt(12)

    team_members = [
        "SUBHAM ROY – AP25110080092",
        "SHREYAS POONDOTA – AP25110080064",
        "MUHAMMAD ISMAIL – AP25110080094",
        "UTKARSH NAYAN – AP25110080089"
    ]
    for tm in team_members:
        p_tm = add_styled_paragraph(doc, tm, space_before=0, space_after=2, align=WD_ALIGN_PARAGRAPH.CENTER)
        p_tm.runs[0].font.bold = True
        p_tm.runs[0].font.size = Pt(11.5)
        p_tm.runs[0].font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)

    p_guidance = add_styled_paragraph(doc, "Under the esteemed guidance of Course Instructor", space_before=12, space_after=2, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_guidance.runs[0].font.italic = True
    p_guidance.runs[0].font.size = Pt(11)

    p_inst = add_styled_paragraph(doc, "Praneetha Surapaneni,", space_before=0, space_after=1, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_inst.runs[0].font.bold = True
    p_inst.runs[0].font.size = Pt(12)

    p_desig = add_styled_paragraph(doc, "Assistant Professor\nDepartment of Computer Science and Engineering", space_before=0, space_after=12, align=WD_ALIGN_PARAGRAPH.CENTER)
    for r in p_desig.runs:
        r.font.size = Pt(10.5)

    # SRM Logo Centered
    if os.path.exists(logo_path):
        p_logo = add_styled_paragraph(doc, space_before=6, space_after=12, align=WD_ALIGN_PARAGRAPH.CENTER)
        p_logo.add_run().add_picture(logo_path, width=Inches(1.85))

    p_dept = add_styled_paragraph(doc, "DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING", space_before=6, space_after=2, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_dept.runs[0].font.bold = True
    p_dept.runs[0].font.size = Pt(12.5)
    p_dept.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    p_univ = add_styled_paragraph(doc, "SRM UNIVERSITY, AP", space_before=0, space_after=2, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_univ.runs[0].font.bold = True
    p_univ.runs[0].font.size = Pt(13)

    p_loc = add_styled_paragraph(doc, "AMARAVATI – 522 240, ANDHRA PRADESH, INDIA.", space_before=0, space_after=6, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_loc.runs[0].font.size = Pt(10.5)

    p_date = add_styled_paragraph(doc, "NOVEMBER-2026", space_before=4, space_after=0, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_date.runs[0].font.bold = True
    p_date.runs[0].font.size = Pt(11.5)

    doc.add_page_break()

    # -------------------------------------------------------------
    # 2. ACKNOWLEDGEMENT PAGE
    # -------------------------------------------------------------
    p_ack_title = add_styled_paragraph(doc, "ACKNOWLEDGEMENT", space_before=12, space_after=18, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_ack_title.runs[0].font.bold = True
    p_ack_title.runs[0].font.size = Pt(16)
    p_ack_title.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    ack_paras = [
        "We would like to express our sincere and heartfelt gratitude to all those who have contributed to the conceptualization, development, and successful completion of this project on the Multi-Agent Computer Intelligence (MCI) System.",
        "First and foremost, we are deeply grateful to our Honourable Vice Chancellor Prof. Ch. Satish Kumar sir, Dean of the School of Engineering and Sciences Prof. C V Tomy sir, and Head of the Department for Computer Science and Engineering Dr. Manikandan sir, Associate Professor, for providing us with the opportunity, computational infrastructure, and institutional facilities necessary to undertake this advanced software engineering project. Their constant encouragement, visionary leadership, and unwavering commitment to academic and technical excellence have been a great source of inspiration throughout this work.",
        "We wish to extend our profound gratitude and thanks to our esteemed Course Instructor and Mentor, Praneetha Surapaneni, Assistant Professor, Department of Computer Science and Engineering, for her invaluable guidance, rigorous critiques, insightful suggestions, and continuous encouragement throughout the semester. Her technical depth in Advanced Java Programming, distributed systems, and software architecture played a pivotal role in shaping our understanding of enterprise modularity, zero-trust security patterns, and robust concurrency.",
        "We also convey our sincere appreciation to all faculty members, technical laboratory staff, and administrative members of the Department of Computer Science and Engineering for their academic guidance, constructive feedback, and logistical support throughout this project.",
        "Finally, we express our warmest appreciation to our fellow classmates, colleagues, and families for their continuous patience, moral support, and cooperation during the long design, coding, and benchmarking sessions that brought this project to fruition."
    ]
    for p_text in ack_paras:
        p = add_styled_paragraph(doc, p_text, space_before=0, space_after=10, line_spacing=1.2, align=WD_ALIGN_PARAGRAPH.JUSTIFY)

    p_reg = add_styled_paragraph(doc, "With Regards,", space_before=16, space_after=6, align=WD_ALIGN_PARAGRAPH.RIGHT)
    p_reg.runs[0].font.bold = True

    for tm in team_members:
        p_mem = add_styled_paragraph(doc, tm, space_before=0, space_after=2, align=WD_ALIGN_PARAGRAPH.RIGHT)
        p_mem.runs[0].font.size = Pt(10.5)

    doc.add_page_break()

    # -------------------------------------------------------------
    # 3. CERTIFICATE PAGE
    # -------------------------------------------------------------
    p_cert_title = add_styled_paragraph(doc, "CERTIFICATE", space_before=12, space_after=24, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_cert_title.runs[0].font.bold = True
    p_cert_title.runs[0].font.size = Pt(16)
    p_cert_title.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    cert_text = (
        "This is to certify that the project entitled \"Multi-Agent Computer Intelligence (MCI) System: "
        "An Autonomous Zero-Trust Multi-Agent Task Orchestration Platform in Java 21\" has been successfully "
        "completed and submitted by Subham Roy (AP25110080092), Shreyas Poondota (AP25110080064), "
        "Muhammad Ismail (AP25110080094), and Utkarsh Nayan (AP25110080089) as a required academic "
        "component of the course CSE 213: Advanced Java Programming during Semester 3 of the Academic Year "
        "2026-27 in the Department of Computer Science and Engineering at SRM University, AP.\n\n"
        "This work was executed under the supervision and guidance of the undersigned and is deemed to have "
        "successfully satisfied all academic standards, technical specifications, and laboratory evaluation "
        "benchmarks as of November 2026. This is also to certify that the project work represents the original "
        "and collaborative teamwork of the candidates."
    )
    p_c = add_styled_paragraph(doc, cert_text, space_before=0, space_after=40, line_spacing=1.3, align=WD_ALIGN_PARAGRAPH.JUSTIFY)

    p_sig = add_styled_paragraph(doc, "(Signature of the Course Instructor)", space_before=30, space_after=4, align=WD_ALIGN_PARAGRAPH.RIGHT)
    p_sig.runs[0].font.bold = True
    p_sig.runs[0].font.size = Pt(11)

    inst_info = [
        "Praneetha Surapaneni,",
        "Assistant Professor,",
        "Department of Computer Science and Engineering,",
        "SRM University, AP"
    ]
    for inf in inst_info:
        p_inf = add_styled_paragraph(doc, inf, space_before=0, space_after=2, align=WD_ALIGN_PARAGRAPH.RIGHT)
        p_inf.runs[0].font.size = Pt(10.5)

    doc.add_page_break()

    # -------------------------------------------------------------
    # 4. ABSTRACT PAGE
    # -------------------------------------------------------------
    p_abs_title = add_styled_paragraph(doc, "ABSTRACT", space_before=12, space_after=18, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_abs_title.runs[0].font.bold = True
    p_abs_title.runs[0].font.size = Pt(16)
    p_abs_title.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    abs_paras = [
        "As Large Language Models (LLMs) transition from conversational interfaces into autonomous computer agents, existing open-source frameworks—predominantly constructed in Python (e.g., AutoGPT, CrewAI)—suffer from critical architectural liabilities: non-deterministic conversational looping, lack of hardware-level isolation, and absence of verifiable pre-execution audit trails. This report presents the Multi-Agent Computer Intelligence (MCI) System, an enterprise-grade, zero-trust autonomous multi-agent task execution platform built natively in Java 21, Spring Boot 3, and Google Gemini AI via LangChain4j.",
        "The architecture is physically partitioned into eight Maven submodules enforcing strict compile-time boundaries: domain records (`mci-core`), lightweight JDBC relational persistence (`mci-persistence`), physical actuators (`mci-tools`), role-based security gating (`mci-gateway`), persona dispatching (`mci-agents`), dynamic planning and DAG scheduling (`mci-orchestrator`), Spring Boot REST and Server-Sent Events streaming (`mci-web`), and an ANSI CLI runner (`mci-app`). At the core of the system sits the ToolGateway, a zero-trust mediator that evaluates every tool invocation against an immutable permission matrix, records intent into an ACID SQLite ledger before hardware execution, and intercepts unauthorized operations. Autonomous code computation is executed via an air-gapped Docker sandbox (`--network none`) equipped with a self-healing write-run-fix repair loop, with automatic cross-platform fallback to a monitored Java ProcessBuilder.",
        "In empirical evaluations spanning over 15 end-to-end missions, the system achieved a 100% task completion rate with an average mission latency of 8 to 14 seconds. The Zero-Trust ToolGateway introduced a microscopic 3.20 milliseconds of overhead per invocation (<0.04% relative penalty), while successfully neutralizing 100% of simulated directory traversal (../../etc/passwd), loopback SSRF (http://localhost:8080), and privilege escalation attacks. Real-time telemetry is broadcast to an editorial architectural web interface (\"Nirikshak\") via native Server-Sent Events, demonstrating that autonomous multi-agent intelligence can be made deterministic, high-performing, and rigorously secure within pure enterprise Java infrastructure."
    ]
    for ap in abs_paras:
        p = add_styled_paragraph(doc, ap, space_before=0, space_after=10, line_spacing=1.2, align=WD_ALIGN_PARAGRAPH.JUSTIFY)

    doc.add_page_break()

    # -------------------------------------------------------------
    # 5. TABLE OF CONTENTS
    # -------------------------------------------------------------
    p_toc_title = add_styled_paragraph(doc, "TABLE OF CONTENTS", space_before=12, space_after=18, align=WD_ALIGN_PARAGRAPH.CENTER)
    p_toc_title.runs[0].font.bold = True
    p_toc_title.runs[0].font.size = Pt(16)
    p_toc_title.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)

    toc_entries = [
        ("Acknowledgement", "2"),
        ("Certificate", "3"),
        ("Abstract", "4"),
        ("CHAPTER 1: INTRODUCTION, PROBLEM STATEMENT & OBJECTIVES", "6"),
        ("CHAPTER 2: LITERATURE REVIEW & TECHNOLOGICAL FOUNDATIONS", "8"),
        ("CHAPTER 3: HIGH-LEVEL SYSTEM ARCHITECTURE & MODULAR DECOMPOSITION", "11"),
        ("CHAPTER 4: CORE DOMAIN MODELS & DATABASE PERSISTENCE", "14"),
        ("CHAPTER 5: THE ZERO-TRUST SECURITY TOOL GATEWAY", "17"),
        ("CHAPTER 6: TOOL IMPLEMENTATIONS & SANDBOX SECURITY", "19"),
        ("CHAPTER 7: AUTONOMOUS MULTI-AGENT SUBSYSTEM (SWARM ENGINE)", "22"),
        ("CHAPTER 8: ORCHESTRATION ENGINE: DYNAMIC PLANNING & REFLECTION", "24"),
        ("CHAPTER 9: WEB LAYER & FULL-STACK INTEGRATION", "27"),
        ("CHAPTER 10: FRONTEND ARCHITECTURE & USER EXPERIENCE", "30"),
        ("CHAPTER 11: REAL-WORLD ENGINEERING CHALLENGES & RESOLUTIONS", "32"),
        ("CHAPTER 12: EXPERIMENTAL EVALUATION, BENCHMARKS & RESULTS", "34"),
        ("CHAPTER 13: NOVELTY, ARCHITECTURAL COMPARISON & DISCUSSION", "37"),
        ("CHAPTER 14: CONCLUSION & KEY LEARNINGS", "38"),
        ("REFERENCES", "39"),
        ("APPENDICES", "40"),
        ("    Appendix A: Complete Database Schema & Triggers (schema.sql)", "40"),
        ("    Appendix B: Security Audit Log Sample (Allowed vs. Blocked)", "40"),
        ("    Appendix C: Sample Generated Executive Intelligence Report", "41"),
        ("    Appendix D: Master Maven Project Configuration (pom.xml)", "41"),
        ("    Appendix E: Environment Configuration & Execution Setup (.env)", "42")
    ]

    for title, pg in toc_entries:
        p_t = add_styled_paragraph(doc, space_before=0, space_after=3, line_spacing=1.1)
        r1 = p_t.add_run(title)
        if title.startswith("CHAPTER") or title in ["Acknowledgement", "Certificate", "Abstract", "REFERENCES", "APPENDICES"]:
            r1.font.bold = True
            r1.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        else:
            r1.font.color.rgb = RGBColor(0x44, 0x44, 0x44)

        # Tab stop simulation / dots
        r_dots = p_t.add_run(" " + "." * max(4, (80 - len(title))) + " ")
        r_dots.font.color.rgb = RGBColor(0xAA, 0xAA, 0xAA)

        r2 = p_t.add_run(pg)
        r2.font.bold = True
        r2.font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)

    doc.add_page_break()

    # -------------------------------------------------------------
    # 6. PARSE AND FORMAT BODY CONTENT FROM project_report.txt
    # -------------------------------------------------------------
    lines = full_text.split("\n")
    i = 0
    in_code_block = False
    code_lines = []
    in_ascii_table = False
    table_lines = []

    while i < len(lines):
        line = lines[i]

        # Check for start/end of markdown code fences
        if line.strip().startswith("```"):
            if in_code_block:
                # Flush code block
                render_code_block(doc, code_lines)
                code_lines = []
                in_code_block = False
            else:
                in_code_block = True
                code_lines = []
            i += 1
            continue

        if in_code_block:
            code_lines.append(line)
            i += 1
            continue

        # Check for ASCII tables (lines starting with +--- or containing |)
        if line.strip().startswith("+--") or (line.strip().startswith("|") and line.strip().endswith("|")):
            table_lines.append(line)
            # Peek ahead to see if table continues
            if i + 1 < len(lines) and (lines[i+1].strip().startswith("+--") or lines[i+1].strip().startswith("|")):
                i += 1
                continue
            else:
                # Table ended
                render_ascii_table(doc, table_lines)
                table_lines = []
                i += 1
                continue

        # Check for Chapter Headers
        if line.startswith("CHAPTER ") and i > 0 and lines[i-1].startswith("===="):
            doc.add_page_break()
            p_ch = add_styled_paragraph(doc, line.strip(), space_before=12, space_after=14, align=WD_ALIGN_PARAGRAPH.LEFT)
            p_ch.runs[0].font.bold = True
            p_ch.runs[0].font.size = Pt(17)
            p_ch.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)
            # Skip separator line below
            if i + 1 < len(lines) and lines[i+1].startswith("===="):
                i += 2
                continue
            i += 1
            continue

        if line.strip() in ["REFERENCES", "APPENDICES"] and i > 0 and lines[i-1].startswith("===="):
            doc.add_page_break()
            p_ref = add_styled_paragraph(doc, line.strip(), space_before=12, space_after=14, align=WD_ALIGN_PARAGRAPH.LEFT)
            p_ref.runs[0].font.bold = True
            p_ref.runs[0].font.size = Pt(17)
            p_ref.runs[0].font.color.rgb = RGBColor(0x00, 0x33, 0x66)
            if i + 1 < len(lines) and lines[i+1].startswith("===="):
                i += 2
                continue
            i += 1
            continue

        # Check for Section Headers: e.g. "1.1 THE AUTONOMY..." followed by "----"
        sec_match = re.match(r"^(\d+\.\d+\s+.*)", line.strip())
        app_match = re.match(r"^(APPENDIX\s+[A-Z]:\s+.*)", line.strip())
        if (sec_match or app_match) and i + 1 < len(lines) and lines[i+1].startswith("----"):
            sec_title = sec_match.group(1) if sec_match else app_match.group(1)
            p_sec = add_styled_paragraph(doc, sec_title, space_before=14, space_after=6, align=WD_ALIGN_PARAGRAPH.LEFT)
            p_sec.runs[0].font.bold = True
            p_sec.runs[0].font.size = Pt(13)
            p_sec.runs[0].font.color.rgb = RGBColor(0x1F, 0x4E, 0x79)
            i += 2  # skip the underline
            continue

        # Ignore standalone decorative separator lines
        if line.startswith("====") or line.startswith("----"):
            i += 1
            continue

        # Skip boilerplate header from project_report.txt
        if "Comprehensive System Architecture" in line or "[Note: Front matter" in line or "END OF REPORT" in line:
            i += 1
            continue

        # Check for image insertion cues
        if "Figure 2.1" in line:
            p_b = add_styled_paragraph(doc, line.strip(), space_before=6, space_after=6, align=WD_ALIGN_PARAGRAPH.JUSTIFY)
            if os.path.exists(asset_path):
                p_img = add_styled_paragraph(doc, space_before=8, space_after=4, align=WD_ALIGN_PARAGRAPH.CENTER)
                p_img.add_run().add_picture(asset_path, width=Inches(4.8))
                p_cap = add_styled_paragraph(doc, "Figure 2.1: Multi-Agent System Architecture & Inter-Module Communication Topology.", space_before=2, space_after=10, align=WD_ALIGN_PARAGRAPH.CENTER)
                p_cap.runs[0].font.italic = True
                p_cap.runs[0].font.size = Pt(9.5)
                p_cap.runs[0].font.color.rgb = RGBColor(0x55, 0x55, 0x55)
            i += 1
            continue

        if "Figure 10.2" in line:
            p_b = add_styled_paragraph(doc, line.strip(), space_before=6, space_after=6, align=WD_ALIGN_PARAGRAPH.JUSTIFY)
            if os.path.exists(dashboard_path):
                p_img = add_styled_paragraph(doc, space_before=8, space_after=4, align=WD_ALIGN_PARAGRAPH.CENTER)
                p_img.add_run().add_picture(dashboard_path, width=Inches(5.2))
                p_cap = add_styled_paragraph(doc, "Figure 10.2: Rendered live Nirikshak web dashboard showing hero layout, ecosystem cards, sparkline telemetry, and live mission console.", space_before=2, space_after=10, align=WD_ALIGN_PARAGRAPH.CENTER)
                p_cap.runs[0].font.italic = True
                p_cap.runs[0].font.size = Pt(9.5)
                p_cap.runs[0].font.color.rgb = RGBColor(0x55, 0x55, 0x55)
            i += 1
            continue

        # Sub-sub headers: e.g. "1. Hallucination and Compounding Errors:" or "Phase 1: ..."
        sub_heading_match = re.match(r"^(\d+\.\s+[A-Za-z\s]+:|Phase\s+\d+:|Pillar\s+\d+:|Attack Test\s+\d+:|Case Study\s+\d+:|View\s+\d+:|Direction\s+\d+:)", line.strip())
        if sub_heading_match:
            p_sub = add_styled_paragraph(doc, line.strip(), space_before=8, space_after=2, align=WD_ALIGN_PARAGRAPH.LEFT)
            p_sub.runs[0].font.bold = True
            p_sub.runs[0].font.size = Pt(11)
            p_sub.runs[0].font.color.rgb = RGBColor(0x22, 0x22, 0x22)
            i += 1
            continue

        # Bullet list items
        if line.strip().startswith("- ") or line.strip().startswith("* "):
            bullet_text = line.strip()[2:]
            p_bullet = add_styled_paragraph(doc, bullet_text, space_before=0, space_after=3, line_spacing=1.15, align=WD_ALIGN_PARAGRAPH.LEFT)
            p_bullet.paragraph_format.left_indent = Inches(0.25)
            # Add bullet bullet bullet symbol
            p_bullet.runs[0].text = "•  " + p_bullet.runs[0].text
            i += 1
            continue

        # Standard Paragraph
        if line.strip():
            p = add_styled_paragraph(doc, line.strip(), space_before=0, space_after=5, line_spacing=1.15, align=WD_ALIGN_PARAGRAPH.JUSTIFY)
        else:
            # Empty line
            pass

        i += 1

    # Save final document
    doc.save(output_docx_path)
    print(f"Successfully generated DOCX report at: {output_docx_path}")

def render_code_block(doc, code_lines):
    if not code_lines:
        return
    text = "\n".join(code_lines)
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.cell(0, 0)
    set_cell_background(cell, "F4F5F7")
    set_cell_margins(cell, top=80, bottom=80, left=140, right=140)

    # Thin border
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'<w:left w:val="single" w:sz="12" w:space="0" w:color="003366"/>'
        f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'<w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(tcBorders)

    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.05
    run = p.add_run(text)
    run.font.name = 'Consolas'
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)

    # Empty spacing after table
    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

def render_ascii_table(doc, table_lines):
    rows_data = []
    for line in table_lines:
        if line.strip().startswith("+"):
            continue
        if "|" in line:
            parts = [c.strip() for c in line.split("|")]
            # remove leading/trailing empty elements from split
            if parts and parts[0] == "":
                parts = parts[1:]
            if parts and parts[-1] == "":
                parts = parts[:-1]
            if parts:
                rows_data.append(parts)

    if not rows_data:
        return

    num_cols = max(len(r) for r in rows_data)
    tbl = doc.add_table(rows=len(rows_data), cols=num_cols)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER

    for r_idx, row in enumerate(rows_data):
        for c_idx in range(num_cols):
            cell = tbl.cell(r_idx, c_idx)
            val = row[c_idx] if c_idx < len(row) else ""
            cell.text = val
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.line_spacing = 1.05

            set_cell_margins(cell, top=60, bottom=60, left=100, right=100)

            if r_idx == 0:
                # Header row
                set_cell_background(cell, "1F4E79")
                for run in p.runs:
                    run.font.bold = True
                    run.font.size = Pt(9.5)
                    run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
            else:
                # Body row
                bg_color = "F8FAFC" if r_idx % 2 == 1 else "FFFFFF"
                set_cell_background(cell, bg_color)
                for run in p.runs:
                    run.font.size = Pt(9)
                    run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)

    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

if __name__ == "__main__":
    main()
