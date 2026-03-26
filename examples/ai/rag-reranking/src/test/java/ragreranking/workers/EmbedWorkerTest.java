package ragreranking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedWorkerTest {

    private final EmbedWorker worker = new EmbedWorker();

    @Test
    void taskDefName() {
        assertEquals("rerank_embed", worker.getTaskDefName());
    }

    @Test
    void returnsEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How does re-ranking improve RAG accuracy?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
    }

    @Test
    void handlesEmptyQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
