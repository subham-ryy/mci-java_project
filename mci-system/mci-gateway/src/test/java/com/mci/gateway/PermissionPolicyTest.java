package com.mci.gateway;

import com.mci.core.AgentType;
import com.mci.core.Decision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionPolicyTest {

    private final PermissionPolicy policy = new PermissionPolicy();

    @Test
    void testNavigatorPermissions() {
        Decision fetchDecision = policy.check(AgentType.NAVIGATOR, "browser.fetch");
        assertTrue(fetchDecision.isAllowed(), "NAVIGATOR must be permitted browser.fetch");

        Decision writeDecision = policy.check(AgentType.NAVIGATOR, "file.write");
        assertTrue(writeDecision.isAllowed(), "NAVIGATOR must be permitted file.write");

        Decision dockerDecision = policy.check(AgentType.NAVIGATOR, "docker.run");
        assertFalse(dockerDecision.isAllowed(), "NAVIGATOR must NOT be permitted docker.run");
    }

    @Test
    void testReportPermissions() {
        Decision reportFetch = policy.check(AgentType.REPORT, "browser.fetch");
        assertFalse(reportFetch.isAllowed(), "REPORT must NOT be permitted browser.fetch");

        Decision reportWrite = policy.check(AgentType.REPORT, "file.write");
        assertTrue(reportWrite.isAllowed(), "REPORT must be permitted file.write");
    }

    @Test
    void testDataFilePermissions() {
        Decision pdfGen = policy.check(AgentType.DATA_FILE, "pdf.generate");
        assertTrue(pdfGen.isAllowed(), "DATA_FILE must be permitted pdf.generate");

        Decision fileWrite = policy.check(AgentType.DATA_FILE, "file.write");
        assertTrue(fileWrite.isAllowed(), "DATA_FILE must be permitted file.write");
    }

    @Test
    void testDockerPermissions() {
        Decision dockerRun = policy.check(AgentType.DOCKER, "docker.run");
        assertTrue(dockerRun.isAllowed(), "DOCKER must be permitted docker.run");

        Decision fileWrite = policy.check(AgentType.DOCKER, "file.write");
        assertTrue(fileWrite.isAllowed(), "DOCKER must be permitted file.write");

        Decision browserFetch = policy.check(AgentType.DOCKER, "browser.fetch");
        assertFalse(browserFetch.isAllowed(), "DOCKER must NOT be permitted browser.fetch");
    }
}
