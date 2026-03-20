package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManagerMergeWorkerTest {

    private final ManagerMergeWorker worker = new ManagerMergeWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_manager_merge", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void mergesResultsAndComputesTotalLines() {
        Map<String, Object> backendResults = Map.of(
                "apiResult", Map.of("linesOfCode", 320),
                "dbResult", Map.of("linesOfCode", 180)
        );
        Map<String, Object> frontendResults = Map.of(
                "uiResult", Map.of("linesOfCode", 450),
                "stylingResult", Map.of("linesOfCode", 280)
        );
        Task task = taskWith(Map.of("backendResults", backendResults, "frontendResults", frontendResults));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("projectReport");
        assertEquals("completed", report.get("status"));
        assertEquals(1230, report.get("totalLinesOfCode"));
        assertNotNull(report.get("backend"));
        assertNotNull(report.get("frontend"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void reportContainsHierarchy() {
        Map<String, Object> backendResults = Map.of(
                "apiResult", Map.of("linesOfCode", 100),
                "dbResult", Map.of("linesOfCode", 50)
        );
        Map<String, Object> frontendResults = Map.of(
                "uiResult", Map.of("linesOfCode", 200),
                "stylingResult", Map.of("linesOfCode", 100)
        );
        Task task = taskWith(Map.of("backendResults", backendResults, "frontendResults", frontendResults));
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("projectReport");
        Map<String, Object> hierarchy = (Map<String, Object>) report.get("hierarchy");
        assertEquals(1, hierarchy.get("manager"));
        assertEquals(2, hierarchy.get("leads"));
        assertEquals(4, hierarchy.get("workers"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullResults() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("projectReport");
        assertEquals("completed", report.get("status"));
        assertEquals(0, report.get("totalLinesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesPartialResults() {
        Map<String, Object> backendResults = Map.of(
                "apiResult", Map.of("linesOfCode", 320)
        );
        Task task = taskWith(Map.of("backendResults", backendResults));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("projectReport");
        assertEquals(320, report.get("totalLinesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void backendAndFrontendArePassedThrough() {
        Map<String, Object> backendResults = Map.of(
                "apiResult", Map.of("linesOfCode", 320, "endpoints", 3),
                "dbResult", Map.of("linesOfCode", 180, "migrations", 3)
        );
        Map<String, Object> frontendResults = Map.of(
                "uiResult", Map.of("linesOfCode", 450),
                "stylingResult", Map.of("linesOfCode", 280)
        );
        Task task = taskWith(Map.of("backendResults", backendResults, "frontendResults", frontendResults));
        TaskResult result = worker.execute(task);

        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("projectReport");
        assertEquals(backendResults, report.get("backend"));
        assertEquals(frontendResults, report.get("frontend"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
