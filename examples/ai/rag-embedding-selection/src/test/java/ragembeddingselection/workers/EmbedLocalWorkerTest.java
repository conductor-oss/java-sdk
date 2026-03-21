package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedLocalWorkerTest {

    private final EmbedLocalWorker worker = new EmbedLocalWorker();

    @Test
    void taskDefName() {
        assertEquals("es_embed_local", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsLocalMetrics() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertEquals("local/all-MiniLM-L6-v2", metrics.get("model"));
        assertEquals(384, metrics.get("dimensions"));
        assertEquals(0.82, metrics.get("precisionAt1"));
        assertEquals(0.78, metrics.get("precisionAt3"));
        assertEquals(0.86, metrics.get("recallAt5"));
        assertEquals(0.80, metrics.get("ndcg"));
        assertEquals(15, metrics.get("latencyMs"));
        assertEquals(0.0, metrics.get("costPerQuery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
