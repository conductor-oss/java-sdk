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
 * Connects to the target domain over TLS and inspects the server certificate.
 * Returns the certificate subject, issuer, expiry date, and days remaining.
 *
 * Input:
 *   - domain (String): hostname to check, e.g. "google.com"
 *   - port (int, optional): TLS port, defaults to 443
 *
 * Output:
 *   - domain (String): the checked domain
 *   - subject (String): certificate subject DN
 *   - issuer (String): certificate issuer DN
 *   - notAfter (String): expiry date ISO string
 *   - daysRemaining (long): days until certificate expires
 *   - expiringSoon (boolean): true if fewer than 30 days remain
 */
public class DiscoverWorker implements Worker {

    private static final int DEFAULT_PORT = 443;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int EXPIRY_THRESHOLD_DAYS = 30;

    @Override
    public String getTaskDefName() {
        return "cr_discover";
    }

    @Override
    public TaskResult execute(Task task) {
        String domain = (String) task.getInputData().get("domain");
        if (domain == null || domain.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Input 'domain' is required");
            return fail;
        }

        int port = DEFAULT_PORT;
        Object portObj = task.getInputData().get("port");
        if (portObj instanceof Number) {
            port = ((Number) portObj).intValue();
        }

        try {
            // Use a trust-all manager so we can inspect certs for any domain,
            // even those with self-signed or expired certs
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

                Instant notAfter = cert.getNotAfter().toInstant();
                long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), notAfter);
                boolean expiringSoon = daysRemaining < EXPIRY_THRESHOLD_DAYS;

                System.out.println("  [discover] " + domain + " cert expires in " + daysRemaining + " days"
                        + (expiringSoon ? " — EXPIRING SOON" : ""));

                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("domain", domain);
                result.addOutputData("subject", cert.getSubjectX500Principal().getName());
                result.addOutputData("issuer", cert.getIssuerX500Principal().getName());
                result.addOutputData("notAfter", notAfter.toString());
                result.addOutputData("daysRemaining", daysRemaining);
                result.addOutputData("expiringSoon", expiringSoon);
                return result;
            }
        } catch (java.net.UnknownHostException e) {
            System.err.println("  [discover] DNS resolution failed for " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("DNS resolution failed for " + domain + ": " + e.getMessage());
            return fail;
        } catch (java.net.ConnectException | java.net.SocketTimeoutException e) {
            System.err.println("  [discover] Network error connecting to " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Retryable network error connecting to " + domain + ":" + port + ": " + e.getMessage());
            return fail;
        } catch (Exception e) {
            System.err.println("  [discover] Failed to check " + domain + ": " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("TLS connection to " + domain + ":" + port + " failed: " + e.getMessage());
            return fail;
        }
    }
}
