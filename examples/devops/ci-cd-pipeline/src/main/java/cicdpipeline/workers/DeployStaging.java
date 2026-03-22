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
 * Deploys the build artifacts to a staging directory. Creates a real directory
 * structure under java.io.tmpdir/cicd-staging/ containing a deployment manifest,
 * a version marker, and a health check file.
 *
 * Input:
 *   - buildId (String, required): build identifier
 *   - imageTag (String, required): docker image tag or artifact version
 *
 * Output:
 *   - deployed (boolean): whether deployment succeeded
 *   - environment (String): "staging"
 *   - deployDir (String): path to the staging deployment
 *   - manifestPath (String): path to the deployment manifest
 *   - durationMs (long): deployment duration
 */
public class DeployStaging implements Worker {

    @Override
    public String getTaskDefName() {
        return "cicd_deploy_staging";
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

        System.out.println("[cicd_deploy_staging] Deploying " + imageTag + " to staging");

        Map<String, Object> output = new LinkedHashMap<>();
        long startMs = System.currentTimeMillis();

        try {
            Path stagingRoot = Path.of(System.getProperty("java.io.tmpdir"), "cicd-staging");
            Files.createDirectories(stagingRoot);

            String deployDirName = "deploy-" + buildId + "-" + System.currentTimeMillis();
            Path deployDir = stagingRoot.resolve(deployDirName);
            Files.createDirectories(deployDir);

            String manifest = "{\n"
                    + "  \"buildId\": \"" + buildId + "\",\n"
                    + "  \"imageTag\": \"" + imageTag + "\",\n"
                    + "  \"environment\": \"staging\",\n"
                    + "  \"deployedAt\": \"" + Instant.now() + "\",\n"
                    + "  \"deployedBy\": \"cicd-pipeline-worker\"\n"
                    + "}\n";
            Path manifestPath = deployDir.resolve("manifest.json");
            Files.writeString(manifestPath, manifest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Files.writeString(deployDir.resolve("VERSION"), imageTag,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Files.writeString(deployDir.resolve("health.json"),
                    "{\"status\": \"ok\", \"version\": \"" + imageTag + "\"}",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long durationMs = System.currentTimeMillis() - startMs;

            System.out.println("  Deployed to: " + deployDir);

            output.put("deployed", true);
            output.put("environment", "staging");
            output.put("deployDir", deployDir.toString());
            output.put("manifestPath", manifestPath.toString());
            output.put("durationMs", durationMs);
            output.put("buildId", buildId);
            output.put("imageTag", imageTag);

        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("  Staging deployment failed: " + e.getMessage());

            output.put("deployed", false);
            output.put("environment", "staging");
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
