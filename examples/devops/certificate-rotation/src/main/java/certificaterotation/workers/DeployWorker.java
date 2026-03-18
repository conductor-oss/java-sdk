package certificaterotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Deploys the generated certificate to a Java KeyStore (JKS) file.
 * Creates a real keystore file on disk containing the certificate,
 * demonstrating the deployment step of a certificate rotation pipeline.
 *
 * In production you would deploy to a load balancer, reverse proxy,
 * or cloud certificate manager (AWS ACM, GCP Certificate Manager, etc.).
 *
 * Input:
 *   - deployData.certPem (String): PEM-encoded certificate from GenerateWorker
 *   - deployData.domain (String): domain name for the keystore alias
 *
 * Output:
 *   - keystorePath (String): path to the created JKS file
 *   - alias (String): the alias under which the cert was stored
 *   - deployed (boolean): true on success
 */
public class DeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        // Extract certificate PEM and domain from generate step
        String certPem = null;
        String domain = "localhost";
        Object deployData = task.getInputData().get("deployData");
        if (deployData instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) deployData;
            certPem = (String) data.get("certPem");
            if (data.get("domain") != null) {
                domain = data.get("domain").toString();
            }
        }

        if (certPem == null || certPem.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("No certificate PEM provided in deployData.certPem");
            return fail;
        }

        try {
            // Parse the PEM certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(certPem.getBytes()));

            // Create a JKS keystore and store the certificate
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null); // Initialize empty keystore
            String alias = domain.replace(".", "-") + "-cert";
            ks.setCertificateEntry(alias, cert);

            // Write to a temp file (in production this would be a known path or remote store)
            Path keystorePath = Files.createTempFile("conductor-cert-", ".jks");
            char[] password = "changeit".toCharArray();
            try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
                ks.store(fos, password);
            }

            System.out.println("  [deploy] Certificate deployed to keystore: " + keystorePath
                    + " (alias: " + alias + ")");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("keystorePath", keystorePath.toString());
            result.addOutputData("alias", alias);
            result.addOutputData("deployed", true);
            return result;

        } catch (Exception e) {
            System.err.println("  [deploy] Deployment failed: " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Keystore deployment failed: " + e.getMessage());
            return fail;
        }
    }
}
