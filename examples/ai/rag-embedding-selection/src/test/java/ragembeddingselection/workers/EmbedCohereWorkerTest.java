package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedCohereWorkerTest {

    private final EmbedCohereWorker worker = new EmbedCohereWorker();

    @Test
    void taskDefName() {
        assertEquals("es_embed_cohere", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsCohereMetrics() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals("cohere/embed-english-v3.0", metrics.get("model"));
        assertEquals(1024, metrics.get("dimensions"));
        assertEquals(0.90, metrics.get("precisionAt1"));
        assertEquals(0.87, metrics.get("precisionAt3"));
        assertEquals(0.92, metrics.get("recallAt5"));
        assertEquals(0.88, metrics.get("ndcg"));
        assertEquals(95, metrics.get("latencyMs"));
        assertEquals(0.0001, metrics.get("costPerQuery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
