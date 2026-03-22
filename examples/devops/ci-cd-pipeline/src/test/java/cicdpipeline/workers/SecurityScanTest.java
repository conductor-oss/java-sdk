package cicdpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityScanTest {

    private final SecurityScan worker = new SecurityScan();

    @Test
    void taskDefName() {
        assertEquals("cicd_security_scan", worker.getTaskDefName());
    }

    @Test
    void failsOnMissingBuildId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsVulnerabilities() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("vulnerabilities"));
    }

    @Test
    void outputContainsCritical() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("critical"));
    }

    @Test
    void scanEmptyDirProducesNoFindings(@TempDir Path tempDir) {
        Task task = taskWithDir("BLD-100001", tempDir.toString());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, ((Number) result.getOutputData().get("vulnerabilities")).intValue());
    }

    @Test
    void detectsSensitiveFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve(".env"), "DATABASE_URL=postgres://user:pass@host/db");

        Task task = taskWithDir("BLD-100001", tempDir.toString());
        TaskResult result = worker.execute(task);

        int high = ((Number) result.getOutputData().get("high")).intValue();
        assertTrue(high > 0, "Should detect .env as a sensitive file");
    }

    @Test
    void detectsHardcodedSecrets(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.java"),
                "String apiKey = \"sk_" + "live_abcdef123456789012345678\";\n");

        Task task = taskWithDir("BLD-100001", tempDir.toString());
        TaskResult result = worker.execute(task);

        int critical = ((Number) result.getOutputData().get("critical")).intValue();
        assertTrue(critical > 0, "Should detect hardcoded API key");
    }

    @Test
    void criticalFindingBlocksDeploy(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("secret.java"),
                "String password = \"supersecretpassword123\";\n");

        Task task = taskWithDir("BLD-100001", tempDir.toString());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("blockDeploy"),
                "Critical findings should block deploy");
    }

    @Test
    void cleanDirDoesNotBlockDeploy(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("Main.java"), "public class Main { }");

        Task task = taskWithDir("BLD-100001", tempDir.toString());
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("blockDeploy"));
    }

    @Test
    void outputContainsDurationMs() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("durationMs"));
        assertTrue(((Number) result.getOutputData().get("durationMs")).longValue() >= 0);
    }

    @Test
    void outputContainsFindings() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("findings"));
    }

    private Task taskWith(String buildId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("buildId", buildId);
        task.setInputData(input);
        return task;
    }

    private Task taskWithDir(String buildId, String buildDir) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("buildId", buildId);
        input.put("buildDir", buildDir);
        task.setInputData(input);
        return task;
    }
}
