package com.mci.tools;

import com.mci.core.AgentType;
import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BrowserToolTest {

    private final BrowserTool browserTool = new BrowserTool();

    @Test
    void testRejectsMissingUrlParameter() {
        ToolRequest request = new ToolRequest(
                "run-1", "st-1", AgentType.NAVIGATOR, "browser.fetch", Map.of()
        );
        ToolResponse response = browserTool.execute(request);
        assertFalse(response.success());
        assertTrue(response.error().contains("Missing required parameter"));
    }

    @Test
    void testRejectsNonHttpSchemes() {
        ToolRequest fileScheme = new ToolRequest(
                "run-1", "st-1", AgentType.NAVIGATOR, "browser.fetch",
                Map.of("url", "file:///etc/passwd")
        );
        ToolResponse response = browserTool.execute(fileScheme);
        assertFalse(response.success());
        assertTrue(response.denied());
        assertTrue(response.error().contains("Protocol rejected"));

        ToolRequest ftpScheme = new ToolRequest(
                "run-1", "st-1", AgentType.NAVIGATOR, "browser.fetch",
                Map.of("url", "ftp://ftp.example.com/file")
        );
        ToolResponse ftpResp = browserTool.execute(ftpScheme);
        assertFalse(ftpResp.success());
        assertTrue(ftpResp.denied());
    }

    @Test
    void testBlocksSsrfLocalhostAndLoopback() {
        ToolRequest localhostReq = new ToolRequest(
                "run-1", "st-1", AgentType.NAVIGATOR, "browser.fetch",
                Map.of("url", "http://localhost:8080/admin")
        );
        ToolResponse response = browserTool.execute(localhostReq);
        assertFalse(response.success());
        assertTrue(response.denied());
        assertTrue(response.error().contains("SSRF Protection Blocked"));

        ToolRequest loopbackReq = new ToolRequest(
                "run-1", "st-1", AgentType.NAVIGATOR, "browser.fetch",
                Map.of("url", "http://127.0.0.1:3000/api")
        );
        ToolResponse loopbackResp = browserTool.execute(loopbackReq);
        assertFalse(loopbackResp.success());
        assertTrue(loopbackResp.denied());
    }

    @Test
    void testValidateSsrfStaticHelper() {
        assertNotNull(BrowserTool.validateSsrf("localhost"));
        assertNotNull(BrowserTool.validateSsrf("127.0.0.1"));
        assertNotNull(BrowserTool.validateSsrf("169.254.169.254")); // Cloud metadata IP
        assertNotNull(BrowserTool.validateSsrf("192.168.1.1"));
        assertNotNull(BrowserTool.validateSsrf("10.0.0.5"));
        assertNotNull(BrowserTool.validateSsrf("172.16.0.1"));
        assertNotNull(BrowserTool.validateSsrf("0.0.0.0"));
    }
}
