package certificaterotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployWorkerTest {

    private final DeployWorker worker = new DeployWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_deploy", worker.getTaskDefName());
    }

    @Test
    void deploysGeneratedCertToKeystore() {
        // First generate a real cert
        GenerateWorker gen = new GenerateWorker();
        Task genTask = taskWith(Map.of("generateData", Map.of("domain", "test.example.com")));
        TaskResult genResult = gen.execute(genTask);
        String certPem = (String) genResult.getOutputData().get("certPem");

        // Now deploy it
        Task task = taskWith(Map.of("deployData", Map.of(
                "certPem", certPem,
                "domain", "test.example.com"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
        assertEquals("test-example-com-cert", result.getOutputData().get("alias"));

        String keystorePath = (String) result.getOutputData().get("keystorePath");
        assertNotNull(keystorePath);
        assertTrue(Files.exists(Path.of(keystorePath)), "Keystore file should exist");

        // Cleanup
        try { Files.deleteIfExists(Path.of(keystorePath)); } catch (Exception ignored) {}
    }

    @Test
    void failsWithoutCertPem() {
        Task task = taskWith(Map.of("deployData", Map.of("domain", "example.com")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("certPem"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
