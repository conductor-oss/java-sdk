package cicdpipeline;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import cicdpipeline.workers.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates worker-to-worker data flow through
 * the CI/CD pipeline: Build -> UnitTest -> SecurityScan -> DeployProd
 */
class CiCdIntegrationTest {

    @Test
    void fullPipelineFlowsDataBetweenWorkers(@TempDir Path tempDir) throws IOException {
        // Create a mock build directory with a Java file
        Files.writeString(tempDir.resolve("Main.java"),
                "public class Main { public static void main(String[] args) { } }");

        // Step 1: Build -- use a non-cloneable URL so it falls back, but still produces a buildId
        Build buildWorker = new Build();
        Task buildTask = new Task();
        buildTask.setStatus(Task.Status.IN_PROGRESS);
        buildTask.setInputData(new HashMap<>(Map.of(
                "repoUrl", "https://github.com/octocat/Hello-World.git",
                "branch", "main",
                "commitSha", "abc1234"
        )));
        TaskResult buildResult = buildWorker.execute(buildTask);
        assertEquals(TaskResult.Status.COMPLETED, buildResult.getStatus());

        String buildId = (String) buildResult.getOutputData().get("buildId");
        assertNotNull(buildId);
        assertTrue(buildId.startsWith("BLD-"));

        String imageTag = (String) buildResult.getOutputData().get("imageTag");
        assertNotNull(imageTag);

        // Step 2: UnitTest -- uses buildId from Build step
        UnitTest unitTestWorker = new UnitTest();
        Task unitTestTask = new Task();
        unitTestTask.setStatus(Task.Status.IN_PROGRESS);
        unitTestTask.setInputData(new HashMap<>(Map.of("buildId", buildId)));
        TaskResult unitTestResult = unitTestWorker.execute(unitTestTask);

        assertEquals(TaskResult.Status.COMPLETED, unitTestResult.getStatus());
        assertEquals(buildId, unitTestResult.getOutputData().get("buildId"));

        // Step 3: SecurityScan -- uses buildId from Build, scans the temp directory
        SecurityScan secScanWorker = new SecurityScan();
        Task secScanTask = new Task();
        secScanTask.setStatus(Task.Status.IN_PROGRESS);
        secScanTask.setInputData(new HashMap<>(Map.of(
                "buildId", buildId,
                "buildDir", tempDir.toString()
        )));
        TaskResult secScanResult = secScanWorker.execute(secScanTask);

        assertEquals(TaskResult.Status.COMPLETED, secScanResult.getStatus());
        assertEquals(buildId, secScanResult.getOutputData().get("buildId"));
        // Clean Java file should produce no critical findings
        assertEquals(0, ((Number) secScanResult.getOutputData().get("critical")).intValue());
        assertEquals(false, secScanResult.getOutputData().get("blockDeploy"));

        // Step 4: DeployProd -- uses buildId and imageTag from Build
        DeployProd deployWorker = new DeployProd();
        Task deployTask = new Task();
        deployTask.setStatus(Task.Status.IN_PROGRESS);
        deployTask.setInputData(new HashMap<>(Map.of(
                "buildId", buildId,
                "imageTag", imageTag
        )));
        TaskResult deployResult = deployWorker.execute(deployTask);

        assertEquals(TaskResult.Status.COMPLETED, deployResult.getStatus());
        assertEquals(true, deployResult.getOutputData().get("deployed"));
        assertEquals("production", deployResult.getOutputData().get("environment"));
        assertEquals(buildId, deployResult.getOutputData().get("buildId"));
        assertEquals(imageTag, deployResult.getOutputData().get("imageTag"));

        // Verify deployment files exist
        String deployDir = (String) deployResult.getOutputData().get("deployDir");
        assertTrue(Files.exists(Path.of(deployDir)));
        assertTrue(Files.exists(Path.of(deployDir, "manifest.json")));
        assertTrue(Files.exists(Path.of(deployDir, "VERSION")));
        assertTrue(Files.exists(Path.of(deployDir, "health.json")));
    }

    @Test
    void securityScanBlocksDeployOnCriticalFinding(@TempDir Path tempDir) throws IOException {
        // Create a file with a hardcoded secret
        Files.writeString(tempDir.resolve("config.java"),
                "String password = \"supersecretpassword123\";\n");

        SecurityScan secScanWorker = new SecurityScan();
        Task secScanTask = new Task();
        secScanTask.setStatus(Task.Status.IN_PROGRESS);
        secScanTask.setInputData(new HashMap<>(Map.of(
                "buildId", "BLD-999",
                "buildDir", tempDir.toString()
        )));
        TaskResult secScanResult = secScanWorker.execute(secScanTask);

        assertTrue(((Number) secScanResult.getOutputData().get("critical")).intValue() > 0);
        assertEquals(true, secScanResult.getOutputData().get("blockDeploy"),
                "Critical finding should block deploy");
    }

    @Test
    void deployFailsOnNonExistentBuildArtifacts() {
        DeployProd deployWorker = new DeployProd();
        Task deployTask = new Task();
        deployTask.setStatus(Task.Status.IN_PROGRESS);
        deployTask.setInputData(new HashMap<>(Map.of(
                "buildId", "BLD-GHOST",
                "imageTag", "app:ghost",
                "buildDir", "/tmp/nonexistent-build-dir-xyz-" + System.nanoTime()
        )));
        TaskResult deployResult = deployWorker.execute(deployTask);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, deployResult.getStatus());
        assertTrue(deployResult.getReasonForIncompletion().contains("Build artifacts not found"));
    }
}
