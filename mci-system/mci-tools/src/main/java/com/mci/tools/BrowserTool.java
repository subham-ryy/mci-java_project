package com.mci.tools;

import com.mci.core.ToolRequest;
import com.mci.core.ToolResponse;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Locale;

/**
 * Dedicated tool for fetching and extracting web content via Jsoup.
 *
 * Implements strict SSRF protection:
 * - Protocols: http and https only
 * - Blocks localhost, 127.0.0.1, ::1, and private IP ranges (RFC 1918 / RFC 4193 / link-local)
 * - Safe manual redirect checking to prevent redirect SSRF
 * - Response size and timeout caps
 */
public class BrowserTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(BrowserTool.class);

    private static final int TIMEOUT_MS = 8000;
    private static final int MAX_BODY_BYTES = 512 * 1024; // 512 KB
    private static final int MAX_EXTRACT_CHARS = 8000;

    @Override
    public String name() {
        return "browser.fetch";
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String urlString = request.getString("url");
        if (urlString == null || urlString.isBlank()) {
            return ToolResponse.error("Missing required parameter: 'url'");
        }

        urlString = urlString.trim();

        try {
            URI uri = new URI(urlString);

            // Step 1: Scheme validation
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.warn("Blocked non-http scheme: {}", scheme);
                return ToolResponse.denied("Protocol rejected: only http and https are permitted: " + scheme);
            }

            // Step 2: SSRF Hostname & IP validation
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return ToolResponse.denied("Invalid hostname in URL: " + urlString);
            }

            String ssrfCheck = validateSsrf(host);
            if (ssrfCheck != null) {
                log.warn("SSRF violation blocked for {}: {}", host, ssrfCheck);
                return ToolResponse.denied("SSRF Protection Blocked: " + ssrfCheck);
            }

            // Step 3: Fetch with manual redirect validation
            Connection con = Jsoup.connect(urlString)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) MCI-System-Agent/1.0 (compatible; ResearchBot/1.0)")
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(false);

            Connection.Response response = con.execute();
            int statusCode = response.statusCode();

            int hops = 0;
            while ((statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) && hops < 3) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) break;

                URI redirectUri = uri.resolve(location);
                String redirectScheme = redirectUri.getScheme();
                if (redirectScheme == null || (!redirectScheme.equalsIgnoreCase("http") && !redirectScheme.equalsIgnoreCase("https"))) {
                    return ToolResponse.denied("SSRF: Redirect to non-http scheme blocked: " + redirectUri);
                }

                String redirectHost = redirectUri.getHost();
                String redirectSsrf = validateSsrf(redirectHost);
                if (redirectSsrf != null) {
                    return ToolResponse.denied("SSRF: Redirect to private address blocked: " + redirectSsrf);
                }

                uri = redirectUri;
                con = Jsoup.connect(redirectUri.toString())
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) MCI-System-Agent/1.0 (compatible; ResearchBot/1.0)")
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(MAX_BODY_BYTES)
                        .followRedirects(false);
                response = con.execute();
                statusCode = response.statusCode();
                hops++;
            }

            Document doc = response.parse();

            // Step 4: Clean and extract text
            String pageTitle = doc.title() != null ? doc.title().trim() : "";
            String metaDesc = "";
            Element descEl = doc.selectFirst("meta[name=description], meta[property=og:description]");
            if (descEl != null && descEl.hasAttr("content")) {
                metaDesc = descEl.attr("content").trim();
            }

            // Strip non-content elements
            doc.select("script, style, nav, footer, noscript, header, iframe, svg, [role=navigation], [role=banner]").remove();

            StringBuilder contentBuilder = new StringBuilder();
            if (!pageTitle.isBlank()) {
                contentBuilder.append("TITLE: ").append(pageTitle).append("\n\n");
            }
            if (!metaDesc.isBlank()) {
                contentBuilder.append("DESCRIPTION: ").append(metaDesc).append("\n\n");
            }

            Elements elements = doc.select("h1, h2, h3, h4, p, li");
            int charCount = contentBuilder.length();

            for (Element el : elements) {
                String text = el.text().trim();
                if (text.length() < 10) continue;

                if (charCount + text.length() > MAX_EXTRACT_CHARS) {
                    contentBuilder.append("\n[Content capped at ").append(MAX_EXTRACT_CHARS).append(" characters]");
                    break;
                }

                if (el.tagName().startsWith("h")) {
                    contentBuilder.append("\n## ").append(text).append("\n");
                } else if (el.tagName().equals("li")) {
                    contentBuilder.append("- ").append(text).append("\n");
                } else {
                    contentBuilder.append(text).append("\n\n");
                }
                charCount = contentBuilder.length();
            }

            String result = contentBuilder.toString().trim();
            if (result.isEmpty()) {
                result = "Fetched page from " + urlString + " (Title: " + pageTitle + "), but no substantive text was found.";
            }

            log.info("Successfully fetched web page: {} ({} chars)", urlString, result.length());
            return ToolResponse.success(result);

        } catch (URISyntaxException e) {
            return ToolResponse.error("Malformed URL: " + e.getMessage());
        } catch (UnknownHostException e) {
            return ToolResponse.error("Host could not be resolved: " + e.getMessage());
        } catch (IOException e) {
            return ToolResponse.error("Network error fetching page: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed fetching page {}: {}", urlString, e.getMessage(), e);
            return ToolResponse.error("Browser tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Checks if a host name or any of its resolved IP addresses are private, loopback, or link-local.
     * Returns null if safe, or a description of why it was blocked.
     */
    public static String validateSsrf(String host) {
        if (host == null || host.isBlank()) {
            return "Empty or null host";
        }

        String h = host.toLowerCase(Locale.ROOT);
        if (h.equals("localhost")
                || h.endsWith(".localhost")
                || h.equals("127.0.0.1")
                || h.equals("0.0.0.0")
                || h.equals("::1")
                || h.equals("[::1]")
                || h.endsWith(".local")
                || h.endsWith(".internal")) {
            return "Hostname is local/loopback (" + host + ")";
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrLocalAddress(addr)) {
                    return "Host " + host + " resolves to private/loopback IP (" + addr.getHostAddress() + ")";
                }
            }
        } catch (UnknownHostException e) {
            // DNS resolution failure will be caught during connect, but not necessarily SSRF
            return null;
        }

        return null;
    }

    private static boolean isPrivateOrLocalAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()) {
            return true;
        }
        if (addr.isSiteLocalAddress()) {
            return true;
        }

        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;

            // 10.0.0.0/8
            if (first == 10) return true;
            // 127.0.0.0/8
            if (first == 127) return true;
            // 169.254.0.0/16 (Link-local / AWS metadata endpoint)
            if (first == 169 && second == 254) return true;
            // 172.16.0.0/12
            if (first == 172 && (second >= 16 && second <= 31)) return true;
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            // 100.64.0.0/10 (Carrier grade NAT)
            if (first == 100 && (second >= 64 && second <= 127)) return true;
            // 0.0.0.0/8
            if (first == 0) return true;
        } else if (b.length == 16) {
            // IPv6 Unique Local Address: fc00::/7
            int first = b[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) return true;
            // IPv6 Link-Local: fe80::/10
            if (first == 0xFE && (b[1] & 0xC0) == 0x80) return true;
        }

        return false;
    }
}
