package com.mci.tools;

import com.mci.core.AgentType;
import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PdfToolTest {

    @Test
    void testGeneratesCleanPdfWithoutErrors(@TempDir Path tempDir) throws Exception {
        PdfTool pdfTool = new PdfTool(tempDir.toString());

        String markdownContent = """
                # Executive Results: Autonomous AI Systems

                ## 1. Executive Summary & Market Intelligence
                The emergence of specialized multi-agent architectures has revolutionized enterprise computing.
                Organizations deploying cooperative multi-agent networks demonstrate marked gains in efficiency.

                ## 2. Quantitative Benchmarks
                - Enterprise Adoption Rate: 72% across Fortune 500
                - Operational Latency Reduction: 44.8% improvement
                - Compute Efficiency Multiplier: 3.2x vs monolithic workflows
                - Security Compliance Verification: 100% policy enforcement

                ## 3. Strategic Assessment
                Multi-agent systems provide isolated execution boundaries through explicit tool gateways.
                Each agent operates strictly within its designated allowlist, preventing unintended access.

                - Recommendation 1: Mandate fine-grained permission auditing
                - Recommendation 2: Adopt deterministic sandbox boundary enforcement
                """;

        ToolRequest request = new ToolRequest(
                "run-test-1",
                "st-1",
                AgentType.DATA_FILE,
                "pdf.generate",
                Map.of(
                        "path", "run-test-1/results.pdf",
                        "content", markdownContent,
                        "title", "Research & Intelligence Results"
                )
        );

        ToolResponse response = pdfTool.execute(request);
        assertTrue(response.success(), "PDF generation should succeed");

        File generatedPdf = tempDir.resolve("run-test-1/results.pdf").toFile();
        assertTrue(generatedPdf.exists(), "PDF file must exist");
        assertTrue(generatedPdf.length() > 500, "PDF should contain valid bytes");

        byte[] bytes = Files.readAllBytes(generatedPdf.toPath());
        String header = new String(bytes, 0, 8);
        assertTrue(header.startsWith("%PDF-1.4"), "Must have valid PDF header");

        String tail = new String(bytes, Math.max(0, bytes.length - 64), Math.min(64, bytes.length));
        assertTrue(tail.contains("%%EOF"), "Must end with %%EOF");
    }
}

