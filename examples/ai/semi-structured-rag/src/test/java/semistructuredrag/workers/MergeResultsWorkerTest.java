package semistructuredrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeResultsWorkerTest {

    private final MergeResultsWorker worker = new MergeResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("ss_merge_results", worker.getTaskDefName());
    }

    @Test
    void mergesStructuredAndUnstructuredResults() {
        List<Map<String, Object>> structured = List.of(
                new HashMap<>(Map.of("field", "revenue", "value", "$4.2M", "table", "financials_db", "match", 0.95))
        );
        List<Map<String, Object>> unstructured = List.of(
                new HashMap<>(Map.of("chunkId", "chunk-001", "score", 0.91, "snippet", "Q3 growth was strong."))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "structuredResults", structured,
                "unstructuredResults", unstructured
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String mergedContext = (String) result.getOutputData().get("mergedContext");
        assertNotNull(mergedContext);
        assertTrue(mergedContext.contains("[TABLE]"));
        assertTrue(mergedContext.contains("[TEXT]"));
        assertTrue(mergedContext.contains("revenue"));
        assertTrue(mergedContext.contains("Q3 growth"));
    }

    @Test
    void returnsTotalDocsCount() {
        List<Map<String, Object>> structured = List.of(
                new HashMap<>(Map.of("field", "revenue", "value", "$4.2M", "table", "financials_db", "match", 0.95)),
                new HashMap<>(Map.of("field", "count", "value", "342", "table", "hr_db", "match", 0.89))
        );
        List<Map<String, Object>> unstructured = List.of(
                new HashMap<>(Map.of("chunkId", "chunk-001", "score", 0.91, "snippet", "Text 1"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "structuredResults", structured,
                "unstructuredResults", unstructured
        )));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalDocs"));
    }

    @Test
    void tableEntriesContainFieldAndValue() {
        List<Map<String, Object>> structured = List.of(
                new HashMap<>(Map.of("field", "employee_count", "value", "342", "table", "hr_db", "match", 0.89))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "structuredResults", structured
        )));
        TaskResult result = worker.execute(task);

        String mergedContext = (String) result.getOutputData().get("mergedContext");
        assertTrue(mergedContext.contains("[TABLE] employee_count = 342 (from hr_db)"));
    }

    @Test
    void textEntriesContainSnippetAndChunkId() {
        List<Map<String, Object>> unstructured = List.of(
                new HashMap<>(Map.of("chunkId", "chunk-003", "score", 0.85, "snippet", "Cloud services demand"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "unstructuredResults", unstructured
        )));
        TaskResult result = worker.execute(task);

        String mergedContext = (String) result.getOutputData().get("mergedContext");
        assertTrue(mergedContext.contains("[TEXT] Cloud services demand (chunk chunk-003, score 0.85)"));
    }

    @Test
    void handlesNullStructuredResults() {
        List<Map<String, Object>> unstructured = List.of(
                new HashMap<>(Map.of("chunkId", "chunk-001", "score", 0.91, "snippet", "Some text"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "unstructuredResults", unstructured
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalDocs"));
    }

    @Test
    void handlesNullUnstructuredResults() {
        List<Map<String, Object>> structured = List.of(
                new HashMap<>(Map.of("field", "revenue", "value", "$4.2M", "table", "financials_db", "match", 0.95))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "structuredResults", structured
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalDocs"));
    }

    @Test
    void handlesBothNull() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalDocs"));
        assertEquals("", result.getOutputData().get("mergedContext"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
