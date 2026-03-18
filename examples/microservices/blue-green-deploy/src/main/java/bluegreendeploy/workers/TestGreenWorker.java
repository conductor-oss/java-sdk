package bluegreendeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestGreenWorker implements Worker {
    private final HttpClient httpClient;

    public TestGreenWorker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Constructor for testing with a custom HttpClient. */
    public TestGreenWorker(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getTaskDefName() {
        return "bg_test_green";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String activeEnv = (String) task.getInputData().getOrDefault("activeEnv", "green");
        String version = (String) task.getInputData().getOrDefault("version", "unknown");
        String healthUrl = (String) task.getInputData().getOrDefault("healthUrl",
                "http://localhost:8080/actuator/health");

        int testsPassed = 0;
        int testsFailed = 0;
        String healthCheck = "failed";
        long responseTimeMs = -1;

        // Test 1: HTTP health check
        long startTime = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                healthCheck = "passed";
                testsPassed++;
            } else {
                testsFailed++;
                System.out.println("  [test] Health check returned HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            testsFailed++;
            System.out.println("  [test] Health check failed: " + e.getMessage());
        }

        // Test 2: Response time within acceptable threshold (< 5000ms)
        if (responseTimeMs >= 0 && responseTimeMs < 5000) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  [test] Response time too high: " + responseTimeMs + "ms");
        }

        // Test 3: Verify the environment label matches
        if ("green".equals(activeEnv)) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  [test] Expected green environment, got: " + activeEnv);
        }

        // Test 4: Verify version is specified
        if (version != null && !"unknown".equals(version) && !version.isBlank()) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  [test] Version not specified or unknown");
        }

        boolean allPassed = testsFailed == 0;
        System.out.println("  [test] Green environment tests: " + testsPassed + " passed, "
                + testsFailed + " failed (health=" + healthCheck + ", responseTime=" + responseTimeMs + "ms)");

        if (allPassed) {
            result.setStatus(TaskResult.Status.COMPLETED);
        } else {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Green environment tests failed: " + testsFailed + " test(s) failed");
        }

        result.getOutputData().put("testsPassed", testsPassed);
        result.getOutputData().put("testsFailed", testsFailed);
        result.getOutputData().put("healthCheck", healthCheck);
        result.getOutputData().put("responseTime", responseTimeMs + "ms");
        result.getOutputData().put("activeEnv", activeEnv);
        result.getOutputData().put("version", version);

        return result;
    }
}
