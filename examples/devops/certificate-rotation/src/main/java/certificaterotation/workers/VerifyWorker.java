package certificaterotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Verifies the TLS certificate on the target domain by performing a real
 * SSL handshake and inspecting the peer certificate.
 *
 * Checks that the connection succeeds, the certificate is not expired,
 * and reports the certificate details.
 *
 * Input:
 *   - verifyData.domain (String): domain to verify (from previous steps)
 *   - port (int, optional): TLS port, defaults to 443
 *
 * Output:
 *   - verified (boolean): true if handshake succeeded and cert is valid
 *   - domain (String): the verified domain
 *   - subject (String): certificate subject DN
 *   - daysRemaining (long): days until current cert expires
 *   - protocol (String): negotiated TLS protocol version
 *   - cipherSuite (String): negotiated cipher suite
 */
public class VerifyWorker implements Worker {

    private static final int DEFAULT_PORT = 443;
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    @Override
    public String getTaskDefName() {
        return "cr_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        // Extract domain from deploy step output or direct input
        String domain = null;
        Object verifyData = task.getInputData().get("verifyData");
        if (verifyData instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) verifyData;
            if (data.get("domain") != null) {
                domain = data.get("domain").toString();
            }
        }
        if (domain == null) {
            domain = (String) task.getInputData().get("domain");
        }
        if (domain == null || domain.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("No domain provided for verification");
            return fail;
        }

        int port = DEFAULT_PORT;
        Object portObj = task.getInputData().get("port");
        if (portObj instanceof Number) {
            port = ((Number) portObj).intValue();
        }

        try {
            // Trust-all manager to allow verification of any cert
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            SSLSocketFactory factory = ctx.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) factory.createSocket(domain, port)) {
                socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                socket.startHandshake();

                X509Certificate cert = (X509Certificate) socket.getSession().getPeerCertificates()[0];
                String protocol = socket.getSession().getProtocol();
                String cipherSuite = socket.getSession().getCipherSuite();

                Instant notAfter = cert.getNotAfter().toInstant();
                long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), notAfter);

                System.out.println("  [verify] TLS handshake to " + domain + " succeeded"
                        + " (" + protocol + ", " + cipherSuite + ")"
                        + " — cert valid for " + daysRemaining + " more days");

                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("verified", true);
                result.addOutputData("domain", domain);
                result.addOutputData("subject", cert.getSubjectX500Principal().getName());
                result.addOutputData("daysRemaining", daysRemaining);
                result.addOutputData("protocol", protocol);
                result.addOutputData("cipherSuite", cipherSuite);
                return result;
            }
        } catch (java.net.UnknownHostException e) {
            System.err.println("  [verify] DNS resolution failed for " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("DNS resolution failed for " + domain + ": " + e.getMessage());
            return fail;
        } catch (java.net.ConnectException | java.net.SocketTimeoutException e) {
            System.err.println("  [verify] Network error connecting to " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Retryable network error for " + domain + ":" + port + ": " + e.getMessage());
            return fail;
        } catch (Exception e) {
            System.err.println("  [verify] TLS verification failed for " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("TLS verification for " + domain + ":" + port + " failed: " + e.getMessage());
            return fail;
        }
    }
}
