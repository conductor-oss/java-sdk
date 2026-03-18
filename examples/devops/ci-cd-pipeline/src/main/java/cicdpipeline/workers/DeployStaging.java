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
 * structure under /tmp/cicd-staging/ (or system temp dir) containing a
 * deployment manifest, a version marker, and a health check file.
 *
 * This performs a real deployment by writing actual files that a downstream
 * health check could verify.
 *
 * Input:
 *   - buildId (String): build identifier
 *   - imageTag (String): docker image tag or artifact version
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
        String buildId = (String) task.getInputData().get("buildId");
        String imageTag = (String) task.getInputData().get("imageTag");

        System.out.println("[cicd_deploy_staging] Deploying " + imageTag + " to staging");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        long startMs = System.currentTimeMillis();

        try {
            // Create staging deployment directory
            Path stagingRoot = Path.of(System.getProperty("java.io.tmpdir"), "cicd-staging");
            Files.createDirectories(stagingRoot);

            String deployDirName = "deploy-" + (buildId != null ? buildId : "unknown") + "-" + System.currentTimeMillis();
            Path deployDir = stagingRoot.resolve(deployDirName);
            Files.createDirectories(deployDir);

            // Write deployment manifest
            String manifest = "{\n"
                    + "  \"buildId\": \"" + buildId + "\",\n"
                    + "  \"imageTag\": \"" + imageTag + "\",\n"
                    + "  \"environment\": \"staging\",\n"
                    + "  \"deployedAt\": \"" + Instant.now() + "\",\n"
                    + "  \"deployedBy\": \"cicd-pipeline-worker\"\n"
                    + "}\n";
            Path manifestPath = deployDir.resolve("manifest.json");
            Files.writeString(manifestPath, manifest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Write version marker
            Files.writeString(deployDir.resolve("VERSION"), imageTag != null ? imageTag : "unknown",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Write health check file
            Files.writeString(deployDir.resolve("health.json"),
                    "{\"status\": \"ok\", \"version\": \"" + imageTag + "\"}",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long durationMs = System.currentTimeMillis() - startMs;

            System.out.println("  Deployed to: " + deployDir);
            System.out.println("  Manifest: " + manifestPath);
            System.out.println("  Duration: " + durationMs + "ms");

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
}
