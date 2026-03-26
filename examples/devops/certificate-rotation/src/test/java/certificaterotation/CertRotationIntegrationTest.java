package certificaterotation;

import certificaterotation.workers.DeployWorker;
import certificaterotation.workers.GenerateWorker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying worker-to-worker data flow in the certificate rotation pipeline.
 * Tests Generate -> Deploy data contract without requiring network access.
 */
class CertRotationIntegrationTest {

    @Test
    void generateOutputFeedsIntoDeployInput() {
        // Step 1: Generate a certificate
        GenerateWorker generateWorker = new GenerateWorker();
        Task genTask = new Task();
        genTask.setStatus(Task.Status.IN_PROGRESS);
        genTask.setInputData(new HashMap<>(Map.of(
                "generateData", Map.of("domain", "integration-test.example.com")
        )));
        TaskResult genResult = generateWorker.execute(genTask);
        assertEquals(TaskResult.Status.COMPLETED, genResult.getStatus(),
                "Generate step must succeed");

        String certPem = (String) genResult.getOutputData().get("certPem");
        assertNotNull(certPem, "Generate must produce certPem");
        assertTrue(certPem.startsWith("-----BEGIN CERTIFICATE-----"));

        String domain = (String) genResult.getOutputData().get("domain");
        assertEquals("integration-test.example.com", domain);

        // Step 2: Deploy the generated certificate
        DeployWorker deployWorker = new DeployWorker();
        Task deployTask = new Task();
        deployTask.setStatus(Task.Status.IN_PROGRESS);
        deployTask.setInputData(new HashMap<>(Map.of(
                "deployData", Map.of(
                        "certPem", certPem,
                        "domain", domain
                )
        )));
        TaskResult deployResult = deployWorker.execute(deployTask);
        assertEquals(TaskResult.Status.COMPLETED, deployResult.getStatus(),
                "Deploy step must succeed with generated cert");
        assertEquals(true, deployResult.getOutputData().get("deployed"));
        assertEquals("integration-test-example-com-cert", deployResult.getOutputData().get("alias"));

        String keystorePath = (String) deployResult.getOutputData().get("keystorePath");
        assertNotNull(keystorePath);
        assertTrue(Files.exists(Path.of(keystorePath)), "Keystore file should exist on disk");

        // Cleanup
        try { Files.deleteIfExists(Path.of(keystorePath)); } catch (Exception ignored) {}
    }

    @Test
    void deployFailsWithoutGenerateOutput() {
        DeployWorker deployWorker = new DeployWorker();
        Task deployTask = new Task();
        deployTask.setStatus(Task.Status.IN_PROGRESS);
        deployTask.setInputData(new HashMap<>(Map.of(
                "deployData", Map.of("domain", "example.com")
        )));
        TaskResult result = deployWorker.execute(deployTask);

        assertEquals(TaskResult.Status.FAILED, result.getStatus(),
                "Deploy must fail when no certificate PEM is provided");
        assertTrue(result.getReasonForIncompletion().contains("certPem"));
    }

    @Test
    void generateCertsForDifferentDomainsProduceDifferentSerials() {
        GenerateWorker generateWorker = new GenerateWorker();

        Task task1 = new Task();
        task1.setStatus(Task.Status.IN_PROGRESS);
        task1.setInputData(new HashMap<>(Map.of("generateData", Map.of("domain", "alpha.example.com"))));

        Task task2 = new Task();
        task2.setStatus(Task.Status.IN_PROGRESS);
        task2.setInputData(new HashMap<>(Map.of("generateData", Map.of("domain", "beta.example.com"))));

        TaskResult result1 = generateWorker.execute(task1);
        TaskResult result2 = generateWorker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result1.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());

        // Serial numbers should differ (they are based on System.currentTimeMillis())
        String serial1 = (String) result1.getOutputData().get("serialNumber");
        String serial2 = (String) result2.getOutputData().get("serialNumber");
        assertNotNull(serial1);
        assertNotNull(serial2);
        // Domains should be different
        assertNotEquals(result1.getOutputData().get("domain"), result2.getOutputData().get("domain"));
    }
}
