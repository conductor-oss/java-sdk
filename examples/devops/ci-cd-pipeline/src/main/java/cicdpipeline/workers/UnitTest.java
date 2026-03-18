package cicdpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs unit tests via a real build tool invocation. Tries Maven (mvn test)
 * if a pom.xml exists in the build directory, otherwise runs a generic
 * "java -version" check to verify the JDK toolchain is functional.
 *
 * If the build directory from the Build worker is not available, the worker
 * executes "java -version" as a smoke test and reports success.
 *
 * Input:
 *   - buildId (String): build identifier for correlation
 *   - buildDir (String): optional path to cloned source
 *
 * Output:
 *   - passed (int): number of tests that passed
 *   - failed (int): number of tests that failed
 *   - skipped (int): number of tests skipped
 *   - testOutput (String): truncated stdout/stderr
 *   - durationMs (long): time taken to run tests
 *   - tool (String): build tool used (maven, gradle, java-version)
 */
public class UnitTest implements Worker {

    @Override
    public String getTaskDefName() {
        return "cicd_unit_test";
    }

    @Override
    public TaskResult execute(Task task) {
        String buildId = (String) task.getInputData().get("buildId");
        String buildDir = (String) task.getInputData().get("buildDir");

        System.out.println("[cicd_unit_test] Running unit tests for build " + buildId);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        long startMs = System.currentTimeMillis();

        try {
            String tool;
            String[] command;
            Path workDir = null;

            if (buildDir != null && !buildDir.isBlank()) {
                Path dir = Path.of(buildDir);
                if (Files.exists(dir.resolve("pom.xml"))) {
                    tool = "maven";
                    command = new String[]{"mvn", "test", "-q", "-B"};
                    workDir = dir;
                } else if (Files.exists(dir.resolve("build.gradle")) || Files.exists(dir.resolve("build.gradle.kts"))) {
                    tool = "gradle";
                    command = new String[]{"./gradlew", "test", "--quiet"};
                    workDir = dir;
                } else {
                    tool = "java-version";
                    command = new String[]{"java", "-version"};
                }
            } else {
                tool = "java-version";
                command = new String[]{"java", "-version"};
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (workDir != null) {
                pb.directory(workDir.toFile());
            }

            Process proc = pb.start();
            String testOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                testOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = proc.waitFor(300, TimeUnit.SECONDS);
            int exitCode = completed ? proc.exitValue() : -1;
            if (!completed) proc.destroyForcibly();

            long durationMs = System.currentTimeMillis() - startMs;

            // Parse test results from Maven/Gradle output if available
            int passed = 0, failed = 0, skipped = 0;
            if ("maven".equals(tool) || "gradle".equals(tool)) {
                // Maven format: "Tests run: X, Failures: Y, Errors: Z, Skipped: W"
                Pattern mavenPattern = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
                Matcher m = mavenPattern.matcher(testOutput);
                while (m.find()) {
                    passed += Integer.parseInt(m.group(1)) - Integer.parseInt(m.group(2)) - Integer.parseInt(m.group(3)) - Integer.parseInt(m.group(4));
                    failed += Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(3));
                    skipped += Integer.parseInt(m.group(4));
                }
                if (passed == 0 && failed == 0 && exitCode == 0) {
                    passed = 1; // At least compilation succeeded
                }
            } else {
                // java -version check
                passed = exitCode == 0 ? 1 : 0;
                failed = exitCode == 0 ? 0 : 1;
            }

            // Truncate output to last 2000 chars
            if (testOutput.length() > 2000) {
                testOutput = "...(truncated)...\n" + testOutput.substring(testOutput.length() - 2000);
            }

            System.out.println("  Tool: " + tool + " | Passed: " + passed + " | Failed: " + failed
                    + " | Skipped: " + skipped + " | Duration: " + durationMs + "ms");

            output.put("passed", passed);
            output.put("failed", failed);
            output.put("skipped", skipped);
            output.put("testOutput", testOutput);
            output.put("durationMs", durationMs);
            output.put("tool", tool);
            output.put("exitCode", exitCode);
            output.put("buildId", buildId);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("  Test execution error: " + e.getMessage());
            output.put("passed", 0);
            output.put("failed", 1);
            output.put("skipped", 0);
            output.put("testOutput", "Error: " + e.getMessage());
            output.put("durationMs", durationMs);
            output.put("tool", "error");
            output.put("exitCode", -1);
            output.put("buildId", buildId);
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
