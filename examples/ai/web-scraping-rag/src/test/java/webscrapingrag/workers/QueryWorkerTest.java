package webscrapingrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryWorkerTest {

    private final QueryWorker worker = new QueryWorker();

    @Test
    void taskDefName() {
        assertEquals("wsrag_query", worker.getTaskDefName());
    }

    @Test
    void returnsContextChunks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do Conductor workers function?",
                "storedIds", List.of("web-chunk-0", "web-chunk-1")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> context = (List<Map<String, Object>>) result.getOutputData().get("context");
        assertNotNull(context);
        assertEquals(2, context.size());
    }

    @Test
    void contextContainsTextAndScore() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?",
                "storedIds", List.of("web-chunk-0")
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> context = (List<Map<String, Object>>) result.getOutputData().get("context");
        Map<String, Object> first = context.get(0);
        assertNotNull(first.get("text"));
        assertNotNull(first.get("score"));
        assertEquals(0.94, (double) first.get("score"), 0.01);
    }

    @Test
    void secondContextHasLowerScore() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Tell me about workers",
                "storedIds", List.of("web-chunk-0", "web-chunk-1")
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> context = (List<Map<String, Object>>) result.getOutputData().get("context");
        double firstScore = (double) context.get(0).get("score");
        double secondScore = (double) context.get(1).get("score");
        assertTrue(firstScore > secondScore);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
