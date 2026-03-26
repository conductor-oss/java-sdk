package semistructuredrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchStructuredWorkerTest {

    private final SearchStructuredWorker worker = new SearchStructuredWorker();

    @Test
    void taskDefName() {
        assertEquals("ss_search_structured", worker.getTaskDefName());
    }

    @Test
    void returnsThreeResults() {
        List<Map<String, String>> fields = List.of(
                Map.of("field", "revenue", "type", "numeric", "source", "financials_db")
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is the revenue?",
                "structuredFields", fields
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void resultsHaveRequiredKeys() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        for (Map<String, Object> r : results) {
            assertNotNull(r.get("field"));
            assertNotNull(r.get("value"));
            assertNotNull(r.get("table"));
            assertNotNull(r.get("match"));
            assertInstanceOf(String.class, r.get("field"));
            assertInstanceOf(String.class, r.get("value"));
            assertInstanceOf(String.class, r.get("table"));
            assertInstanceOf(Double.class, r.get("match"));
        }
    }

    @Test
    void resultsHaveExpectedMatchScores() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertEquals(0.95, results.get(0).get("match"));
        assertEquals(0.89, results.get(1).get("match"));
        assertEquals(0.82, results.get(2).get("match"));
    }

    @Test
    void resultsContainExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");

        assertEquals("revenue", results.get(0).get("field"));
        assertEquals("employee_count", results.get(1).get("field"));
        assertEquals("department", results.get(2).get("field"));
    }

    @Test
    void handlesNullStructuredFields() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("results"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
