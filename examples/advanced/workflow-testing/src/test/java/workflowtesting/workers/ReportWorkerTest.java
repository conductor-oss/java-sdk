package workflowtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {

    private final ReportWorker worker = new ReportWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_report", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("testSuite", "my-suite", "allPassed", true,
                "assertions", List.of(
                        Map.of("name", "check1", "passed", true),
                        Map.of("name", "check2", "passed", true))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void reportShowsPassedWhenAllPassed() {
        Task task = taskWith(Map.of("testSuite", "suite-a", "allPassed", true,
                "assertions", List.of(
                        Map.of("name", "check1", "passed", true),
                        Map.of("name", "check2", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("PASSED", report.get("result"));
    }

    @Test
    void reportShowsFailedWhenNotAllPassed() {
        Task task = taskWith(Map.of("testSuite", "suite-b", "allPassed", false,
                "assertions", List.of(
                        Map.of("name", "check1", "passed", false),
                        Map.of("name", "check2", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("FAILED", report.get("result"));
    }

    @Test
    void reportContainsSuiteName() {
        Task task = taskWith(Map.of("testSuite", "order-tests", "allPassed", true,
                "assertions", List.of(Map.of("name", "check1", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("order-tests", report.get("suite"));
    }

    @Test
    void totalAssertionsComputedFromInput() {
        Task task = taskWith(Map.of("testSuite", "suite-c", "allPassed", true,
                "assertions", List.of(
                        Map.of("name", "status_check", "passed", true),
                        Map.of("name", "count_check", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(2, report.get("totalAssertions"));
        assertEquals(2, report.get("passedAssertions"));
    }

    @Test
    void passedAssertionsCountsOnlyPassed() {
        Task task = taskWith(Map.of("testSuite", "suite-d", "allPassed", false,
                "assertions", List.of(
                        Map.of("name", "status_check", "passed", true),
                        Map.of("name", "count_check", "passed", false))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(2, report.get("totalAssertions"));
        assertEquals(1, report.get("passedAssertions"));
    }

    @Test
    void zeroAssertionsWhenNoneProvided() {
        Task task = taskWith(Map.of("testSuite", "suite-e", "allPassed", true));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(0, report.get("totalAssertions"));
        assertEquals(0, report.get("passedAssertions"));
    }

    @Test
    void handlesStringTrueForAllPassed() {
        Task task = taskWith(Map.of("testSuite", "suite-f", "allPassed", "true",
                "assertions", List.of(Map.of("name", "check1", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("PASSED", report.get("result"));
    }

    @Test
    void teardownCleanReflectsInput() {
        Task task = taskWith(Map.of("testSuite", "suite-g", "allPassed", true,
                "teardownClean", true,
                "assertions", List.of(Map.of("name", "check1", "passed", true))));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(true, report.get("teardownClean"));
    }

    @Test
    void teardownCleanFalseWhenNotProvided() {
        Task task = taskWith(Map.of("testSuite", "suite-h", "allPassed", true));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals(false, report.get("teardownClean"));
    }

    @Test
    void handlesNullTestSuite() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuite", null);
        input.put("allPassed", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("unknown-suite", report.get("suite"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
