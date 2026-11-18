package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dedicated tool for generating structured, presentable PDF reports and research results.
 *
 * Implements pure Java PDF 1.4 generation with precise typography, margin isolation,
 * and robust text-wrapping to prevent overlapping headings, lines, or paragraphs.
 */
public class PdfTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(PdfTool.class);

    private final String baseDir; // e.g. "./workspace"

    public PdfTool(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String name() {
        return "pdf.generate";
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String path = request.getString("path");
        String content = request.getString("content");
        String title = request.getString("title");

        if (path == null || path.isBlank()) {
            path = request.runId() + "/results.pdf";
        }
        if (content == null || content.isBlank()) {
            return ToolResponse.error("Missing required parameter: 'content'");
        }
        if (title == null || title.isBlank()) {
            title = "Multi-Agent System — Intelligence & Research Results";
        }

        File targetFile = new File(baseDir, path);

        // Security check: verify the target file stays inside workspace sandbox
        if (!isInsideWorkspace(targetFile)) {
            log.warn("Security violation: Path escapes workspace: {}", path);
            return ToolResponse.denied("Path escapes workspace sandbox: " + path);
        }

        try {
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            byte[] pdfBytes = buildPdf(title, content, request.runId());
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(pdfBytes);
            }

            long bytes = targetFile.length();
            log.info("Successfully generated PDF: {} ({} bytes)", path, bytes);
            return ToolResponse.success("PDF generated successfully: " + path + " (" + bytes + " bytes)");

        } catch (Exception e) {
            log.error("Failed generating PDF {}: {}", path, e.getMessage(), e);
            return ToolResponse.error("Failed to generate PDF: " + e.getMessage());
        }
    }

    /**
     * Builds a structured multi-page PDF document with non-overlapping typography.
     */
    private byte[] buildPdf(String title, String content, String runId) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();
        offsets.add(0L); // 0th object is dummy

        // Parse content into layout blocks
        List<PdfBlock> blocks = parseBlocks(content);

        // Paginate blocks cleanly (A4: 595 x 842 points)
        List<List<PdfBlock>> pages = paginate(blocks);
        if (pages.isEmpty()) {
            pages.add(List.of(new PdfBlock(BlockType.H1, List.of("Research Results"), 10, 18, 10)));
        }

        int totalPages = pages.size();

        int nextObj = 1;
        int catalogId = nextObj++;
        int pagesId = nextObj++;
        int fontBoldId = nextObj++;
        int fontRegularId = nextObj++;
        int fontItalicId = nextObj++;

        List<Integer> pageObjIds = new ArrayList<>();
        List<Integer> contentObjIds = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageObjIds.add(nextObj++);
            contentObjIds.add(nextObj++);
        }

        // PDF Header
        write(pdf, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

        // Catalog
        offsets.add((long) pdf.size());
        write(pdf, catalogId + " 0 obj\n<< /Type /Catalog /Pages " + pagesId + " 0 R >>\nendobj\n");

        // Pages
        offsets.add((long) pdf.size());
        StringBuilder kids = new StringBuilder("[");
        for (int id : pageObjIds) kids.append(id).append(" 0 R ");
        kids.append("]");
        write(pdf, pagesId + " 0 obj\n<< /Type /Pages /Kids " + kids + " /Count " + totalPages + " >>\nendobj\n");

        // Fonts
        offsets.add((long) pdf.size());
        write(pdf, fontBoldId + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

        offsets.add((long) pdf.size());
        write(pdf, fontRegularId + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        offsets.add((long) pdf.size());
        write(pdf, fontItalicId + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>\nendobj\n");

        // Render each page
        for (int p = 0; p < totalPages; p++) {
            int pageId = pageObjIds.get(p);
            int contentId = contentObjIds.get(p);
            List<PdfBlock> pageBlocks = pages.get(p);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            // Header Banner
            if (p == 0) {
                // Top Header Banner (Deep Navy: RGB 0.10, 0.18, 0.32)
                write(stream, "0.10 0.18 0.32 rg\n");
                write(stream, "40 765 515 54 re f\n");

                // Cyan accent underline
                write(stream, "0.20 0.60 0.88 RG\n2 w\n40 763 m 555 763 l S\n");

                // Main Title (White)
                String displayTitle = sanitizeText(title);
                if (displayTitle.length() > 60) displayTitle = displayTitle.substring(0, 57) + "...";
                write(stream, "BT\n/F1 13.5 Tf\n1 1 1 rg\n52 795 Td\n(" + escapePdf(displayTitle) + ") Tj\nET\n");

                // Subtitle metadata
                write(stream, "BT\n/F3 9.0 Tf\n0.80 0.88 0.98 rg\n52 778 Td\n(MCI System Intelligence Run: " + escapePdf(runId) + " | Official Results Document) Tj\nET\n");
            } else {
                // Continuation Header for page 2+
                write(stream, "0.75 0.78 0.82 RG\n0.75 w\n40 805 m 555 805 l S\n");
                String shortTitle = sanitizeText(title);
                if (shortTitle.length() > 50) shortTitle = shortTitle.substring(0, 47) + "...";
                write(stream, "BT\n/F3 8.5 Tf\n0.35 0.38 0.45 rg\n40 812 Td\n(" + escapePdf(shortTitle) + " — Results Continued) Tj\nET\n");
            }

            // Render content blocks with strictly non-overlapping coordinates
            double y = (p == 0) ? 740 : 785;
            for (PdfBlock block : pageBlocks) {
                y -= block.marginTop;

                switch (block.type) {
                    case H1 -> {
                        for (String line : block.lines) {
                            y -= block.leading;
                            write(stream, "BT\n/F1 13.5 Tf\n0.10 0.20 0.38 rg\n45 " + (int) y + " Td\n(" + escapePdf(line) + ") Tj\nET\n");
                        }
                        // Decorative accent rule under H1
                        int lineY = (int) (y - 5);
                        write(stream, "0.80 0.84 0.90 RG\n1 w\n45 " + lineY + " m 550 " + lineY + " l S\n");
                        y -= block.marginBottom;
                    }
                    case H2 -> {
                        for (String line : block.lines) {
                            y -= block.leading;
                            write(stream, "BT\n/F1 11.5 Tf\n0.15 0.35 0.60 rg\n45 " + (int) y + " Td\n(" + escapePdf(line) + ") Tj\nET\n");
                        }
                        y -= block.marginBottom;
                    }
                    case H3 -> {
                        for (String line : block.lines) {
                            y -= block.leading;
                            write(stream, "BT\n/F1 10.0 Tf\n0.22 0.26 0.32 rg\n45 " + (int) y + " Td\n(" + escapePdf(line) + ") Tj\nET\n");
                        }
                        y -= block.marginBottom;
                    }
                    case BODY -> {
                        for (String line : block.lines) {
                            y -= block.leading;
                            write(stream, "BT\n/F2 9.5 Tf\n0.15 0.15 0.15 rg\n45 " + (int) y + " Td\n(" + escapePdf(line) + ") Tj\nET\n");
                        }
                        y -= block.marginBottom;
                    }
                    case BULLET -> {
                        boolean first = true;
                        for (String line : block.lines) {
                            y -= block.leading;
                            if (first) {
                                // Bullet marker in blue
                                write(stream, "BT\n/F1 9.5 Tf\n0.20 0.45 0.75 rg\n46 " + (int) y + " Td\n(-) Tj\nET\n");
                                first = false;
                            }
                            write(stream, "BT\n/F2 9.5 Tf\n0.15 0.15 0.15 rg\n56 " + (int) y + " Td\n(" + escapePdf(line) + ") Tj\nET\n");
                        }
                        y -= block.marginBottom;
                    }
                    case SPACER -> {
                        // Spacer already consumed marginTop
                    }
                }
            }

            // Footer
            write(stream, "0.80 0.82 0.85 RG\n0.5 w\n40 45 m 555 45 l S\n");
            write(stream, "BT\n/F2 8.0 Tf\n0.45 0.45 0.45 rg\n40 32 Td\n(Multi-Agent Computer Intelligence System \\(MCI\\) | Security & Audit Enforced) Tj\nET\n");
            write(stream, "BT\n/F2 8.0 Tf\n0.45 0.45 0.45 rg\n495 32 Td\n(Page " + (p + 1) + " of " + totalPages + ") Tj\nET\n");

            byte[] streamBytes = stream.toByteArray();

            // Page object
            offsets.add((long) pdf.size());
            write(pdf, pageId + " 0 obj\n");
            write(pdf, "<< /Type /Page /Parent " + pagesId + " 0 R /MediaBox [0 0 595 842]\n");
            write(pdf, "   /Contents " + contentId + " 0 R\n");
            write(pdf, "   /Resources << /Font << /F1 " + fontBoldId + " 0 R /F2 " + fontRegularId + " 0 R /F3 " + fontItalicId + " 0 R >> >>\n");
            write(pdf, ">>\nendobj\n");

            // Content Stream object
            offsets.add((long) pdf.size());
            write(pdf, contentId + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            pdf.write(streamBytes);
            write(pdf, "\nendstream\nendobj\n");
        }

        // Xref Table
        long xrefOffset = pdf.size();
        write(pdf, "xref\n0 " + nextObj + "\n");
        write(pdf, "0000000000 65535 f \n");
        for (int i = 1; i < nextObj; i++) {
            write(pdf, String.format(Locale.US, "%010d 00000 n \n", offsets.get(i)));
        }

        // Trailer
        write(pdf, "trailer\n<< /Size " + nextObj + " /Root " + catalogId + " 0 R >>\n");
        write(pdf, "startxref\n" + xrefOffset + "\n%%EOF\n");

        return pdf.toByteArray();
    }

    /**
     * Parses raw markdown/text into structured typographic blocks with strict margin separation.
     */
    private static List<PdfBlock> parseBlocks(String content) {
        List<PdfBlock> blocks = new ArrayList<>();
        String[] rawLines = content.split("\n");

        boolean lastWasSpacer = true;

        for (String raw : rawLines) {
            String trimmed = raw.trim();

            if (trimmed.isEmpty()) {
                if (!lastWasSpacer) {
                    blocks.add(new PdfBlock(BlockType.SPACER, Collections.emptyList(), 6.0, 0, 0));
                    lastWasSpacer = true;
                }
                continue;
            }

            lastWasSpacer = false;
            String clean = cleanMarkdown(sanitizeText(trimmed));

            if (trimmed.startsWith("# ")) {
                String text = cleanMarkdown(sanitizeText(trimmed.substring(2).trim()));
                List<String> wrapped = wrapText(text, 55);
                blocks.add(new PdfBlock(BlockType.H1, wrapped, 20.0, 18.0, 14.0));

            } else if (trimmed.startsWith("## ")) {
                String text = cleanMarkdown(sanitizeText(trimmed.substring(3).trim()));
                List<String> wrapped = wrapText(text, 65);
                blocks.add(new PdfBlock(BlockType.H2, wrapped, 16.0, 15.0, 8.0));

            } else if (trimmed.startsWith("### ")) {
                String text = cleanMarkdown(sanitizeText(trimmed.substring(4).trim()));
                List<String> wrapped = wrapText(text, 72);
                blocks.add(new PdfBlock(BlockType.H3, wrapped, 12.0, 13.0, 6.0));

            } else if (trimmed.startsWith("#### ")) {
                String text = cleanMarkdown(sanitizeText(trimmed.substring(5).trim()));
                List<String> wrapped = wrapText(text, 72);
                blocks.add(new PdfBlock(BlockType.H3, wrapped, 10.0, 13.0, 6.0));

            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                String text = cleanMarkdown(sanitizeText(trimmed.substring(2).trim()));
                List<String> wrapped = wrapText(text, 74);
                blocks.add(new PdfBlock(BlockType.BULLET, wrapped, 2.0, 13.5, 4.0));

            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                // Numbered list item
                String text = cleanMarkdown(sanitizeText(trimmed));
                List<String> wrapped = wrapText(text, 74);
                blocks.add(new PdfBlock(BlockType.BULLET, wrapped, 2.0, 13.5, 4.0));

            } else {
                // Regular paragraph body text
                List<String> wrapped = wrapText(clean, 80);
                blocks.add(new PdfBlock(BlockType.BODY, wrapped, 4.0, 14.0, 8.0));
            }
        }

        return blocks;
    }

    /**
     * Paginates blocks across pages guaranteeing that elements never overflow into margins.
     */
    private static List<List<PdfBlock>> paginate(List<PdfBlock> blocks) {
        List<List<PdfBlock>> pages = new ArrayList<>();
        List<PdfBlock> currentPage = new ArrayList<>();
        double currentY = 740; // Top boundary on page 1 (under banner)
        final double bottomLimit = 65; // Bottom boundary (above footer)

        for (PdfBlock block : blocks) {
            double blockHeight = block.totalHeight;

            // If block doesn't fit on current page and current page has content, start new page
            if (currentY - blockHeight < bottomLimit && !currentPage.isEmpty()) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
                currentY = 785; // Top boundary on subsequent pages
            }

            // If a block has multiple lines and is taller than an entire page, break its lines
            if (blockHeight > (currentY - bottomLimit) && block.lines.size() > 1) {
                List<String> remainingLines = new ArrayList<>(block.lines);
                while (!remainingLines.isEmpty()) {
                    double availSpace = currentY - bottomLimit - block.marginTop - block.marginBottom;
                    int linesThatFit = Math.max(1, (int) (availSpace / block.leading));
                    int take = Math.min(linesThatFit, remainingLines.size());

                    List<String> chunk = new ArrayList<>(remainingLines.subList(0, take));
                    remainingLines.subList(0, take).clear();

                    PdfBlock subBlock = new PdfBlock(block.type, chunk, block.marginTop, block.leading, block.marginBottom);
                    currentPage.add(subBlock);
                    currentY -= subBlock.totalHeight;

                    if (!remainingLines.isEmpty()) {
                        pages.add(currentPage);
                        currentPage = new ArrayList<>();
                        currentY = 785;
                    }
                }
            } else {
                currentPage.add(block);
                currentY -= blockHeight;
            }
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }

        return pages;
    }

    /**
     * Wraps text into lines not exceeding maxChars, splitting cleanly on whitespace.
     */
    private static List<String> wrapText(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxChars) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(word);
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Cleans markdown formatting characters (e.g. **bold**, *italic*, `code`).
     */
    private static String cleanMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                   .replaceAll("\\*(.*?)\\*", "$1")
                   .replaceAll("__(.*?)__", "$1")
                   .replaceAll("_(.*?)_", "$1")
                   .replaceAll("`(.*?)`", "$1");
    }

    /**
     * Sanitizes unicode characters to standard ASCII printable characters for PDF fonts.
     */
    private static String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("—", " -- ")
                   .replace("–", "-")
                   .replace("“", "\"")
                   .replace("”", "\"")
                   .replace("‘", "'")
                   .replace("’", "'")
                   .replace("•", "- ")
                   .replace("✓", "[x]")
                   .replace("✗", "[ ]")
                   .replace("\u00A0", " ")
                   .replaceAll("[^\\x20-\\x7E]", " ");
    }

    /**
     * Escapes special characters for PDF text string literals.
     */
    private static String escapePdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[\\r\\n]+", " ");
    }

    private static void write(OutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private boolean isInsideWorkspace(File file) {
        try {
            String filePath = file.getCanonicalPath();
            String basePath = new File(baseDir).getCanonicalPath();
            return filePath.startsWith(basePath);
        } catch (Exception e) {
            return false;
        }
    }

    private enum BlockType {
        H1, H2, H3, BODY, BULLET, SPACER
    }

    private static class PdfBlock {
        final BlockType type;
        final List<String> lines;
        final double marginTop;
        final double leading;
        final double marginBottom;
        final double totalHeight;

        PdfBlock(BlockType type, List<String> lines, double marginTop, double leading, double marginBottom) {
            this.type = type;
            this.lines = lines;
            this.marginTop = marginTop;
            this.leading = leading;
            this.marginBottom = marginBottom;
            this.totalHeight = marginTop + (lines.isEmpty() ? 0 : (lines.size() * leading)) + marginBottom;
        }
    }
}
