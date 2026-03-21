package recommendationengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersonalizeWorkerTest {

    private final PersonalizeWorker worker = new PersonalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("rec_personalize", worker.getTaskDefName());
    }

    @Test
    void returnsTop3Recommendations() {
        List<Map<String, Object>> ranked = List.of(
                Map.of("sku", "A", "score", 0.95, "category", "audio", "rank", 1),
                Map.of("sku", "B", "score", 0.88, "category", "electronics", "rank", 2),
                Map.of("sku", "C", "score", 0.82, "category", "electronics", "rank", 3),
                Map.of("sku", "D", "score", 0.76, "category", "accessories", "rank", 4));
        Task task = taskWith(Map.of("rankedProducts", ranked, "context", "product_page"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.getOutputData().get("recommendations");
        assertEquals(3, recs.size());
    }

    @Test
    void recommendationsContainReasonString() {
        List<Map<String, Object>> ranked = List.of(
                Map.of("sku", "X", "score", 0.9, "category", "audio", "rank", 1));
        Task task = taskWith(Map.of("rankedProducts", ranked, "context", "homepage"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.getOutputData().get("recommendations");
        String reason = (String) recs.get(0).get("reason");
        assertTrue(reason.contains("audio"));
    }

    @Test
    void outputContainsContext() {
        List<Map<String, Object>> ranked = List.of(
                Map.of("sku", "A", "score", 0.9, "category", "audio", "rank", 1));
        Task task = taskWith(Map.of("rankedProducts", ranked, "context", "checkout"));
        TaskResult result = worker.execute(task);

        assertEquals("checkout", result.getOutputData().get("context"));
    }

    @Test
    void handlesFewerThan3Products() {
        List<Map<String, Object>> ranked = List.of(
                Map.of("sku", "A", "score", 0.9, "category", "audio", "rank", 1),
                Map.of("sku", "B", "score", 0.8, "category", "electronics", "rank", 2));
        Task task = taskWith(Map.of("rankedProducts", ranked, "context", "homepage"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.getOutputData().get("recommendations");
        assertEquals(2, recs.size());
    }

    @Test
    void handlesEmptyRankedList() {
        Task task = taskWith(Map.of("rankedProducts", List.of(), "context", "homepage"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) result.getOutputData().get("recommendations");
        assertEquals(0, recs.size());
    }

    @Test
    void handlesNullContext() {
        List<Map<String, Object>> ranked = List.of(
                Map.of("sku", "A", "score", 0.9, "category", "audio", "rank", 1));
        Map<String, Object> input = new HashMap<>();
        input.put("rankedProducts", ranked);
        input.put("context", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("homepage", result.getOutputData().get("context"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("recommendations"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
