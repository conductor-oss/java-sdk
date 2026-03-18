package cicdpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs integration tests by performing real connectivity checks.
 * Verifies that common infrastructure endpoints are reachable (DNS resolution,
 * HTTP connectivity) to run the integration test phase of a CI/CD pipeline.
 *
 * Also runs a real "java -version" command to verify the toolchain.
 *
 * Input:
 *   - buildId (String): build identifier for correlation
 *
 * Output:
 *   - passed (int): number of integration checks that passed
 *   - failed (int): number of integration checks that failed
 *   - checks (List): detailed check results
 *   - durationMs (long): total time taken
 */
public class IntegrationTest implements Worker {

    private static final String[] TEST_ENDPOINTS = {
            "https://httpbin.org/status/200",
            "https://www.google.com",
            "https://api.github.com"
    };

    @Override
    public String getTaskDefName() {
        return "cicd_integration_test";
    }

    @Override
    public TaskResult execute(Task task) {
        String buildId = (String) task.getInputData().get("buildId");
        System.out.println("[cicd_integration_test] Running integration tests for build " + buildId);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        long startMs = System.currentTimeMillis();
        int passed = 0;
        int failed = 0;
        List<Map<String, Object>> checks = new ArrayList<>();

        // Check 1: Verify Java runtime
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String javaOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                javaOutput = reader.lines().collect(Collectors.joining("\n"));
            }
            proc.waitFor(10, TimeUnit.SECONDS);
            int exitCode = proc.exitValue();

            if (exitCode == 0) {
                passed++;
                checks.add(Map.of("name", "java-runtime", "passed", true, "detail", javaOutput.split("\n")[0]));
            } else {
                failed++;
                checks.add(Map.of("name", "java-runtime", "passed", false, "detail", "exit code " + exitCode));
            }
        } catch (Exception e) {
            failed++;
            checks.add(Map.of("name", "java-runtime", "passed", false, "detail", e.getMessage()));
        }

        // Check 2-4: HTTP connectivity to well-known endpoints
        for (String endpoint : TEST_ENDPOINTS) {
            try {
                long checkStart = System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "CI-IntegrationTest/1.0");
                int statusCode = conn.getResponseCode();
                long responseTime = System.currentTimeMillis() - checkStart;
                conn.disconnect();

                if (statusCode >= 200 && statusCode < 400) {
                    passed++;
                    checks.add(Map.of("name", "http-" + URI.create(endpoint).getHost(),
                            "passed", true, "statusCode", statusCode, "responseTimeMs", responseTime));
                } else {
                    failed++;
                    checks.add(Map.of("name", "http-" + URI.create(endpoint).getHost(),
                            "passed", false, "statusCode", statusCode, "responseTimeMs", responseTime));
                }
            } catch (Exception e) {
                failed++;
                checks.add(Map.of("name", "http-" + endpoint, "passed", false, "detail", e.getMessage()));
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;

        System.out.println("  Passed: " + passed + " | Failed: " + failed + " | Duration: " + durationMs + "ms");

        output.put("passed", passed);
        output.put("failed", failed);
        output.put("checks", checks);
        output.put("durationMs", durationMs);
        output.put("buildId", buildId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
