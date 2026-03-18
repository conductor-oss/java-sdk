package uptimemonitor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.net.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;

/**
 * Performs real HTTP, DNS, and TLS health checks on a single endpoint.
 */
public class CheckEndpoint implements Worker {

    @Override
    public String getTaskDefName() {
        return "uptime_check_endpoint";
    }

    @Override
    public TaskResult execute(Task task) {
        String urlStr = (String) task.getInputData().get("url");
        String name = (String) task.getInputData().get("name");
        int expectedStatus = toInt(task.getInputData().get("expectedStatus"), 200);
        int timeout = toInt(task.getInputData().get("timeout"), 5000);

        System.out.println("[uptime_check_endpoint] Checking: " + name + " (" + urlStr + ")");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("url", urlStr);
        output.put("name", name);
        output.put("timestamp", Instant.now().toString());

        List<String> passedChecks = new ArrayList<>();
        List<String> failedChecks = new ArrayList<>();

        // DNS check
        try {
            URL url = new URL(urlStr);
            InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
            List<String> ips = new ArrayList<>();
            for (InetAddress addr : addresses) {
                ips.add(addr.getHostAddress());
            }
            output.put("dns", Map.of("resolved", true, "addresses", ips));
            passedChecks.add("dns");
            System.out.println("  DNS: resolved to " + ips.size() + " address(es)");
        } catch (Exception e) {
            output.put("dns", Map.of("resolved", false, "error", e.getMessage()));
            failedChecks.add("dns");
            System.out.println("  DNS: FAILED — " + e.getMessage());
        }

        // HTTP check
        long responseTimeMs = 0;
        int actualStatus = 0;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "UptimeMonitor/1.0");

            long start = System.currentTimeMillis();
            conn.connect();
            actualStatus = conn.getResponseCode();
            responseTimeMs = System.currentTimeMillis() - start;

            // Read and discard body
            try (InputStream is = actualStatus < 400 ? conn.getInputStream() : conn.getErrorStream()) {
                if (is != null) {
                    byte[] buf = new byte[4096];
                    while (is.read(buf) != -1) { /* drain */ }
                }
            }
            conn.disconnect();

            boolean statusOk = actualStatus == expectedStatus;
            output.put("http", Map.of(
                    "statusCode", actualStatus,
                    "expectedStatus", expectedStatus,
                    "statusMatch", statusOk,
                    "responseTimeMs", responseTimeMs
            ));

            if (statusOk) {
                passedChecks.add("http");
                System.out.println("  HTTP: " + actualStatus + " (" + responseTimeMs + "ms)");
            } else {
                failedChecks.add("http");
                System.out.println("  HTTP: FAILED — got " + actualStatus + ", expected " + expectedStatus);
            }
        } catch (Exception e) {
            output.put("http", Map.of("error", e.getMessage(), "responseTimeMs", 0));
            failedChecks.add("http");
            System.out.println("  HTTP: FAILED — " + e.getMessage());
        }

        // TLS check (only for HTTPS)
        if (urlStr.startsWith("https://")) {
            try {
                URL url = new URL(urlStr);
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                try (SSLSocket socket = (SSLSocket) factory.createSocket(url.getHost(), 443)) {
                    socket.setSoTimeout(timeout);
                    socket.startHandshake();

                    var certs = socket.getSession().getPeerCertificates();
                    if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                        X509Certificate x509 = (X509Certificate) certs[0];
                        long daysUntilExpiry = (x509.getNotAfter().getTime() - System.currentTimeMillis())
                                / (1000 * 60 * 60 * 24);

                        output.put("tls", Map.of(
                                "valid", true,
                                "issuer", x509.getIssuerX500Principal().getName(),
                                "expiresAt", x509.getNotAfter().toInstant().toString(),
                                "daysUntilExpiry", daysUntilExpiry
                        ));
                        passedChecks.add("tls");
                        System.out.println("  TLS: valid, expires in " + daysUntilExpiry + " days");
                    }
                }
            } catch (Exception e) {
                output.put("tls", Map.of("valid", false, "error", e.getMessage()));
                failedChecks.add("tls");
                System.out.println("  TLS: FAILED — " + e.getMessage());
            }
        }

        // Determine status — HTTP is the core check
        String status;
        if (failedChecks.isEmpty()) {
            status = "healthy";
        } else if (failedChecks.contains("http") || failedChecks.contains("dns")) {
            status = "down";
        } else {
            // HTTP and DNS passed, but TLS failed — service reachable but degraded
            status = "degraded";
        }

        output.put("passedChecks", passedChecks);
        output.put("failedChecks", failedChecks);
        output.put("status", status);
        output.put("responseTimeMs", responseTimeMs);

        System.out.println("  Status: " + status.toUpperCase());

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
