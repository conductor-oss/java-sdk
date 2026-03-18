package uptimemonitor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AggregateResultsTest {

    private final AggregateResults worker = new AggregateResults();

    @Test
    void taskDefName() {
        assertEquals("uptime_aggregate_results", worker.getTaskDefName());
    }

    @Test
    void allHealthyReturnsHealthyStatus() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("healthy", "Google", 150),
                "check_ep_1_ref", epResult("healthy", "GitHub", 200)
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healthy", result.getOutputData().get("overallStatus"));
        assertEquals(false, result.getOutputData().get("hasFailures"));

        @SuppressWarnings("unchecked")
        var failures = (List<?>) result.getOutputData().get("failures");
        assertTrue(failures.isEmpty());
    }

    @Test
    void degradedEndpointReturnsDegradedStatus() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("healthy", "Google", 150),
                "check_ep_1_ref", epResult("degraded", "HTTPStat", 300)
        ));

        TaskResult result = worker.execute(task);

        assertEquals("degraded", result.getOutputData().get("overallStatus"));
        assertEquals(true, result.getOutputData().get("hasFailures"));

        @SuppressWarnings("unchecked")
        var failures = (List<Map<String, Object>>) result.getOutputData().get("failures");
        assertEquals(1, failures.size());
        assertEquals("HTTPStat", failures.get(0).get("name"));
    }

    @Test
    void downEndpointReturnsCriticalStatus() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("healthy", "Google", 150),
                "check_ep_1_ref", epResult("down", "BadService", 0)
        ));

        TaskResult result = worker.execute(task);

        assertEquals("critical", result.getOutputData().get("overallStatus"));
        assertEquals(true, result.getOutputData().get("hasFailures"));
    }

    @Test
    void downTakesPriorityOverDegraded() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("degraded", "SlowService", 500),
                "check_ep_1_ref", epResult("down", "DeadService", 0)
        ));

        TaskResult result = worker.execute(task);

        assertEquals("critical", result.getOutputData().get("overallStatus"));

        @SuppressWarnings("unchecked")
        var failures = (List<?>) result.getOutputData().get("failures");
        assertEquals(2, failures.size());
    }

    @Test
    void summaryCountsAreCorrect() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("healthy", "A", 100),
                "check_ep_1_ref", epResult("healthy", "B", 200),
                "check_ep_2_ref", epResult("degraded", "C", 300),
                "check_ep_3_ref", epResult("down", "D", 0)
        ));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var summary = (Map<String, Object>) result.getOutputData().get("summary");

        assertEquals(4, summary.get("totalEndpoints"));
        assertEquals(2, summary.get("healthy"));
        assertEquals(1, summary.get("degraded"));
        assertEquals(1, summary.get("down"));
    }

    @Test
    void averageResponseTimeCalculation() {
        Task task = taskWith(Map.of(
                "check_ep_0_ref", epResult("healthy", "A", 100),
                "check_ep_1_ref", epResult("healthy", "B", 300)
        ));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var summary = (Map<String, Object>) result.getOutputData().get("summary");

        assertEquals(200L, summary.get("avgResponseTimeMs"));
    }

    @Test
    void ignoresNonCheckEntries() {
        Map<String, Object> joinOutput = new HashMap<>();
        joinOutput.put("check_ep_0_ref", epResult("healthy", "Google", 150));
        joinOutput.put("some_other_task_ref", Map.of("irrelevant", true));

        Task task = taskWith(joinOutput);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var summary = (Map<String, Object>) result.getOutputData().get("summary");

        assertEquals(1, summary.get("totalEndpoints"));
    }

    @Test
    void nullJoinOutputReturnsHealthy() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healthy", result.getOutputData().get("overallStatus"));
        assertEquals(false, result.getOutputData().get("hasFailures"));
    }

    private Task taskWith(Map<String, Object> joinOutput) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("joinOutput", joinOutput)));
        return task;
    }

    private Map<String, Object> epResult(String status, String name, int responseTimeMs) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("name", name);
        result.put("url", "https://" + name.toLowerCase() + ".com");
        result.put("responseTimeMs", responseTimeMs);
        result.put("failedChecks", status.equals("healthy") ? List.of() : List.of("http"));
        return result;
    }
}
