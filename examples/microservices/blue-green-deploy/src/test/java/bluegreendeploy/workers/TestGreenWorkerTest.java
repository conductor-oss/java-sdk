package bluegreendeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestGreenWorkerTest {

    @Test
    void taskDefName() {
        assertEquals("bg_test_green", new TestGreenWorker().getTaskDefName());
    }

    @Test
    void failsGracefullyWhenHealthEndpointUnreachable() {
        // Use a very short timeout client to make the test fast
        HttpClient quickClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
        TestGreenWorker w = new TestGreenWorker(quickClient);
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "activeEnv", "green",
                "version", "2.0.0",
                "healthUrl", "http://192.0.2.1:9999/health" // RFC 5737 TEST-NET, will not connect
        )));

        TaskResult r = w.execute(t);
        // Health check fails, so overall test fails
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertEquals("failed", r.getOutputData().get("healthCheck"));
        assertTrue((int) r.getOutputData().get("testsFailed") > 0);
    }

    @Test
    void failsWhenActiveEnvIsNotGreen() {
        HttpClient quickClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
        TestGreenWorker w = new TestGreenWorker(quickClient);
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "activeEnv", "blue",
                "version", "2.0.0",
                "healthUrl", "http://192.0.2.1:9999/health"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        // At least the environment check should have failed
        assertTrue((int) r.getOutputData().get("testsFailed") >= 1);
    }

    @Test
    void failsWhenVersionIsUnknown() {
        HttpClient quickClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
        TestGreenWorker w = new TestGreenWorker(quickClient);
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "activeEnv", "green",
                "version", "unknown",
                "healthUrl", "http://192.0.2.1:9999/health"
        )));

        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertTrue((int) r.getOutputData().get("testsFailed") >= 1);
    }

    @Test
    void outputContainsExpectedFields() {
        HttpClient quickClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(100))
                .build();
        TestGreenWorker w = new TestGreenWorker(quickClient);
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "activeEnv", "green",
                "version", "3.0.0",
                "healthUrl", "http://192.0.2.1:9999/health"
        )));

        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("testsPassed"));
        assertNotNull(r.getOutputData().get("testsFailed"));
        assertNotNull(r.getOutputData().get("healthCheck"));
        assertNotNull(r.getOutputData().get("responseTime"));
        assertEquals("green", r.getOutputData().get("activeEnv"));
        assertEquals("3.0.0", r.getOutputData().get("version"));
    }
}
