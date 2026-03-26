package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedOpenaiWorkerTest {

    private final EmbedOpenaiWorker worker = new EmbedOpenaiWorker();

    @Test
    void taskDefName() {
        assertEquals("es_embed_openai", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsOpenaiMetrics() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals("openai/text-embedding-3-large", metrics.get("model"));
        assertEquals(3072, metrics.get("dimensions"));
        assertEquals(0.93, metrics.get("precisionAt1"));
        assertEquals(0.89, metrics.get("precisionAt3"));
        assertEquals(0.95, metrics.get("recallAt5"));
        assertEquals(0.91, metrics.get("ndcg"));
        assertEquals(120, metrics.get("latencyMs"));
        assertEquals(0.00013, metrics.get("costPerQuery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
