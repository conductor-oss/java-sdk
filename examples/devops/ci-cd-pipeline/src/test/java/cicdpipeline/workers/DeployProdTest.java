package cicdpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeployProdTest {

    private final DeployProd worker = new DeployProd();

    @Test
    void taskDefName() {
        assertEquals("cicd_deploy_prod", worker.getTaskDefName());
    }

    @Test
    void failsOnMissingBuildId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("imageTag", "app:1.0")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnMissingImageTag() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("buildId", "BLD-100001")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnNonExistentBuildDir() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "buildId", "BLD-100001",
                "imageTag", "app:1.0",
                "buildDir", "/nonexistent/build/dir/xyz123"
        )));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Build artifacts not found"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputDeployedIsTrue() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("deployed"));
    }

    @Test
    void outputEnvironmentIsProduction() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);
        assertEquals("production", result.getOutputData().get("environment"));
    }

    @Test
    void createsRealDeploymentDirectory() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);

        String deployDir = (String) result.getOutputData().get("deployDir");
        assertNotNull(deployDir);
        assertTrue(Files.exists(Path.of(deployDir)), "Deploy directory should exist");
    }

    @Test
    void createsManifestFile() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);

        String manifestPath = (String) result.getOutputData().get("manifestPath");
        assertNotNull(manifestPath);
        assertTrue(Files.exists(Path.of(manifestPath)), "Manifest file should exist");
    }

    @Test
    void manifestContainsBuildInfo() throws Exception {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);

        String manifestPath = (String) result.getOutputData().get("manifestPath");
        String content = Files.readString(Path.of(manifestPath));
        assertTrue(content.contains("BLD-100001"));
        assertTrue(content.contains("app:1.2.3"));
        assertTrue(content.contains("production"));
    }

    @Test
    void outputContainsDurationMs() {
        Task task = taskWith("BLD-100001", "app:1.2.3");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("durationMs"));
        assertTrue(((Number) result.getOutputData().get("durationMs")).longValue() >= 0);
    }

    private Task taskWith(String buildId, String imageTag) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("buildId", buildId);
        input.put("imageTag", imageTag);
        task.setInputData(input);
        return task;
    }
}
