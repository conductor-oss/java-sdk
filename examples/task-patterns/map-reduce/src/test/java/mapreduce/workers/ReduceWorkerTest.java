package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReduceWorkerTest {

    private final ReduceWorker worker = new ReduceWorker();

    @Test
    void taskDefName() {
        assertEquals("mr_reduce", worker.getTaskDefName());
    }

    @Test
    void aggregatesMultipleLogResults() {
        // Simulate join output from 4 log files:
        // api-server.log:    50000 lines, 50 errors, 200 warnings
        // auth-service.log:  30000 lines, 30 errors, 120 warnings
        // payment-gateway.log: 75000 lines, 75 errors, 300 warnings
        // notification.log:  20000 lines, 20 errors, 80 warnings
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "api-server.log",
                "errorCount", 50, "warningCount", 200, "lineCount", 50000));
        mapResults.put("log_1_ref", Map.of(
                "fileName", "auth-service.log",
                "errorCount", 30, "warningCount", 120, "lineCount", 30000));
        mapResults.put("log_2_ref", Map.of(
                "fileName", "payment-gateway.log",
                "errorCount", 75, "warningCount", 300, "lineCount", 75000));
        mapResults.put("log_3_ref", Map.of(
                "fileName", "notification-service.log",
                "errorCount", 20, "warningCount", 80, "lineCount", 20000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 4));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);

        assertEquals(4, report.get("filesAnalyzed"));
        assertEquals(175, report.get("totalErrors"));   // 50 + 30 + 75 + 20
        assertEquals(700, report.get("totalWarnings")); // 200 + 120 + 300 + 80
        assertEquals(175000, report.get("totalLines")); // 50000 + 30000 + 75000 + 20000

        // errorRate = 175 / 175000 * 100 = 0.100%
        assertEquals("0.100%", report.get("errorRate"));

        // worstFile = payment-gateway.log (75 errors)
        assertEquals("payment-gateway.log", report.get("worstFile"));

        // totalErrors = 175 > 100 -> CRITICAL
        assertEquals("CRITICAL", report.get("healthStatus"));
    }

    @Test
    void warningStatusWhenErrorsBetween50And100() {
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "a.log",
                "errorCount", 40, "warningCount", 100, "lineCount", 40000));
        mapResults.put("log_1_ref", Map.of(
                "fileName", "b.log",
                "errorCount", 20, "warningCount", 50, "lineCount", 20000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");

        assertEquals(60, report.get("totalErrors")); // 40 + 20
        assertEquals("WARNING", report.get("healthStatus"));
    }

    @Test
    void healthyStatusWhenFewErrors() {
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "small.log",
                "errorCount", 10, "warningCount", 40, "lineCount", 10000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");

        assertEquals(10, report.get("totalErrors"));
        assertEquals("HEALTHY", report.get("healthStatus"));
    }

    @Test
    void errorRateFormatting() {
        // 50 errors in 10000 lines -> 0.500%
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "test.log",
                "errorCount", 50, "warningCount", 200, "lineCount", 10000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");

        assertEquals("0.500%", report.get("errorRate"));
    }

    @Test
    void worstFileIsFileWithMostErrors() {
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "low.log",
                "errorCount", 5, "warningCount", 10, "lineCount", 5000));
        mapResults.put("log_1_ref", Map.of(
                "fileName", "high.log",
                "errorCount", 45, "warningCount", 100, "lineCount", 45000));
        mapResults.put("log_2_ref", Map.of(
                "fileName", "mid.log",
                "errorCount", 20, "warningCount", 50, "lineCount", 20000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");

        assertEquals("high.log", report.get("worstFile"));
    }

    @Test
    void handlesNullMapResults() {
        Task task = taskWith(Map.of("fileCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals(0, report.get("filesAnalyzed"));
        assertEquals(0, report.get("totalErrors"));
        assertEquals(0, report.get("totalWarnings"));
        assertEquals(0, report.get("totalLines"));
        assertEquals("0.000%", report.get("errorRate"));
        assertEquals("none", report.get("worstFile"));
        assertEquals("HEALTHY", report.get("healthStatus"));
    }

    @Test
    void ignoresNonLogEntries() {
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "a.log",
                "errorCount", 10, "warningCount", 20, "lineCount", 10000));
        mapResults.put("some_other_key", Map.of(
                "fileName", "ignored.log",
                "errorCount", 999, "warningCount", 999, "lineCount", 999));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");

        // Only log_0_ref should be counted
        assertEquals(1, report.get("filesAnalyzed"));
        assertEquals(10, report.get("totalErrors"));
    }

    @Test
    void criticalThresholdBoundary() {
        // Exactly 101 errors -> CRITICAL
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "big.log",
                "errorCount", 101, "warningCount", 400, "lineCount", 101000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("CRITICAL", report.get("healthStatus"));
    }

    @Test
    void warningThresholdBoundary() {
        // Exactly 51 errors -> WARNING
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "medium.log",
                "errorCount", 51, "warningCount", 200, "lineCount", 51000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("WARNING", report.get("healthStatus"));
    }

    @Test
    void exactly50ErrorsIsHealthy() {
        Map<String, Object> mapResults = new HashMap<>();
        mapResults.put("log_0_ref", Map.of(
                "fileName", "exact.log",
                "errorCount", 50, "warningCount", 200, "lineCount", 50000));

        Task task = taskWith(Map.of("mapResults", mapResults, "fileCount", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("HEALTHY", report.get("healthStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
