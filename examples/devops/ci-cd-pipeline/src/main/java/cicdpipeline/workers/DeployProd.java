package cicdpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deploys the build artifacts to a production directory. Validates that
 * build artifacts exist before deploying.
 *
 * Input:
 *   - buildId (String, required): build identifier
 *   - imageTag (String, required): docker image tag or artifact version
 *   - buildDir (String, optional): path to build artifacts; validated if provided
 *
 * Output:
 *   - deployed (boolean): whether deployment succeeded
 *   - environment (String): "production"
 *   - deployDir (String): path to the production deployment
 *   - manifestPath (String): path to the deployment manifest
 *   - durationMs (long): deployment duration
 */
public class DeployProd implements Worker {

    @Override
    public String getTaskDefName() {
        return "cicd_deploy_prod";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String buildId = getRequiredString(task, "buildId");
        if (buildId == null || buildId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: buildId");
            return result;
        }

        String imageTag = getRequiredString(task, "imageTag");
        if (imageTag == null || imageTag.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: imageTag");
            return result;
        }

        // Validate build artifacts exist if buildDir is provided
        String buildDir = getRequiredString(task, "buildDir");
        if (buildDir != null && !buildDir.isBlank()) {
            Path buildPath = Path.of(buildDir);
            if (!Files.exists(buildPath)) {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("Build artifacts not found at: " + buildDir
                        + ". Ensure the build step completed successfully.");
                return result;
            }
        }

        System.out.println("[cicd_deploy_prod] Deploying " + imageTag + " to production");

        Map<String, Object> output = new LinkedHashMap<>();
        long startMs = System.currentTimeMillis();

        try {
            // Create production deployment directory
            Path prodRoot = Path.of(System.getProperty("java.io.tmpdir"), "cicd-production");
            Files.createDirectories(prodRoot);

            String deployDirName = "deploy-" + buildId + "-" + System.currentTimeMillis();
            Path deployDir = prodRoot.resolve(deployDirName);
            Files.createDirectories(deployDir);

            // Write deployment manifest
            String manifest = "{\n"
                    + "  \"buildId\": \"" + buildId + "\",\n"
                    + "  \"imageTag\": \"" + imageTag + "\",\n"
                    + "  \"environment\": \"production\",\n"
                    + "  \"deployedAt\": \"" + Instant.now() + "\",\n"
                    + "  \"deployedBy\": \"cicd-pipeline-worker\",\n"
                    + "  \"promotedFrom\": \"staging\"\n"
                    + "}\n";
            Path manifestPath = deployDir.resolve("manifest.json");
            Files.writeString(manifestPath, manifest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Write version marker
            Files.writeString(deployDir.resolve("VERSION"), imageTag,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Write health check file
            Files.writeString(deployDir.resolve("health.json"),
                    "{\"status\": \"ok\", \"version\": \"" + imageTag + "\", \"environment\": \"production\"}",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long durationMs = System.currentTimeMillis() - startMs;

            System.out.println("  Deployed to: " + deployDir);

            output.put("deployed", true);
            output.put("environment", "production");
            output.put("deployDir", deployDir.toString());
            output.put("manifestPath", manifestPath.toString());
            output.put("durationMs", durationMs);
            output.put("buildId", buildId);
            output.put("imageTag", imageTag);

        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("  Production deployment failed: " + e.getMessage());

            output.put("deployed", false);
            output.put("environment", "production");
            output.put("error", e.getMessage());
            output.put("durationMs", durationMs);
            output.put("buildId", buildId);
            output.put("imageTag", imageTag);
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
