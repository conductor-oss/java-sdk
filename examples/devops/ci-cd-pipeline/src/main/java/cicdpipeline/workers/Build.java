package cicdpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clones a git repository and checks out the specified branch using real
 * git commands via ProcessBuilder. Generates a build ID from the repo name,
 * branch, and timestamp.
 *
 * Input:
 *   - repoUrl (String, required): git repository URL to clone
 *   - branch (String, required): branch to check out
 *   - commitSha (String, optional): commit SHA for tagging
 *
 * Output:
 *   - buildId (String): unique build identifier
 *   - imageTag (String): docker-style image tag
 *   - buildDir (String): path to cloned/build directory
 *   - branch (String): branch that was built
 *   - commitSha (String): commit SHA used
 *   - cloneOutput (String): stdout from git clone
 *   - durationMs (long): time taken for clone + checkout
 */
public class Build implements Worker {

    @Override
    public String getTaskDefName() {
        return "cicd_build";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate required inputs
        String repoUrl = getRequiredString(task, "repoUrl");
        if (repoUrl == null || repoUrl.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: repoUrl. Provide a git repository URL.");
            return result;
        }

        String branch = getRequiredString(task, "branch");
        if (branch == null || branch.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: branch. Provide a git branch name.");
            return result;
        }

        String commitSha = getRequiredString(task, "commitSha");
        String shortSha = commitSha != null && commitSha.length() >= 7
                ? commitSha.substring(0, 7) : (commitSha != null ? commitSha : "HEAD");

        System.out.println("[cicd_build] Building " + repoUrl + " branch=" + branch + " @" + shortSha);

        Map<String, Object> output = new LinkedHashMap<>();

        // Generate deterministic build ID from inputs
        String buildId = "BLD-" + Math.abs((repoUrl + branch + shortSha).hashCode() % 900000 + 100000);
        String repoName = extractRepoName(repoUrl);
        String imageTag = repoName + ":" + branch + "-" + shortSha;

        long startMs = System.currentTimeMillis();

        try {
            // Create a temp directory for the clone
            Path buildDir = Files.createTempDirectory("cicd-build-");
            Path cloneTarget = buildDir.resolve(repoName);

            // Attempt real git clone with depth 1 for speed
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone", "--depth", "1", "--branch", branch, repoUrl, cloneTarget.toString()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            String cloneOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                cloneOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = proc.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            int exitCode = completed ? proc.exitValue() : -1;

            if (!completed) {
                proc.destroyForcibly();
            }

            long durationMs = System.currentTimeMillis() - startMs;

            if (exitCode == 0) {
                System.out.println("  Clone succeeded in " + durationMs + "ms");

                // Get actual HEAD commit if possible
                ProcessBuilder headPb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
                headPb.directory(cloneTarget.toFile());
                headPb.redirectErrorStream(true);
                Process headProc = headPb.start();
                String headOutput;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(headProc.getInputStream()))) {
                    headOutput = reader.lines().collect(Collectors.joining()).trim();
                }
                headProc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                if (headProc.exitValue() == 0 && !headOutput.isBlank()) {
                    imageTag = repoName + ":" + branch + "-" + headOutput;
                }

                output.put("buildId", buildId);
                output.put("imageTag", imageTag);
                output.put("buildDir", cloneTarget.toString());
                output.put("branch", branch);
                output.put("commitSha", commitSha);
                output.put("cloneOutput", cloneOutput);
                output.put("durationMs", durationMs);
                output.put("cloneExitCode", exitCode);
            } else {
                System.out.println("  Clone failed (exit " + exitCode + ")");
                // Clone failure is retryable (network issue, etc.)
                output.put("buildId", buildId);
                output.put("imageTag", imageTag);
                output.put("buildDir", buildDir.toString());
                output.put("branch", branch);
                output.put("commitSha", commitSha);
                output.put("cloneOutput", cloneOutput);
                output.put("durationMs", durationMs);
                output.put("cloneExitCode", exitCode);
                output.put("cloneError", "git clone exited with code " + exitCode);
            }

            // Cleanup temp directory in background
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deleteRecursively(buildDir);
                } catch (Exception ignored) { }
            }));

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("  Build setup error: " + e.getMessage());
            output.put("buildId", buildId);
            output.put("imageTag", imageTag);
            output.put("buildDir", "");
            output.put("branch", branch);
            output.put("commitSha", commitSha);
            output.put("cloneOutput", "");
            output.put("durationMs", durationMs);
            output.put("cloneError", e.getMessage());
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    static String extractRepoName(String repoUrl) {
        if (repoUrl == null) return "app";
        String name = repoUrl;
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) name = name.substring(lastSlash + 1);
        return name.isBlank() ? "app" : name;
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }

    private void deleteRecursively(Path dir) throws Exception {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                        });
            }
        }
    }
}
