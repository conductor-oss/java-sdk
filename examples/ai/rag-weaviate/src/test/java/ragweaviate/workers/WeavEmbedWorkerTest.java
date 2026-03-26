package ragweaviate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeavEmbedWorkerTest {

    private final WeavEmbedWorker worker = new WeavEmbedWorker();

    @Test
    void taskDefName() {
        assertEquals("weav_embed", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEmbeddingVector() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Weaviate?"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertEquals(5, embedding.size());
        assertEquals(0.0123, embedding.get(0));
        assertEquals(-0.0456, embedding.get(1));
        assertEquals(0.0789, embedding.get(2));
        assertEquals(-0.0321, embedding.get(3));
        assertEquals(0.0654, embedding.get(4));
    }

    @Test
    void embeddingIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("question", "First question")));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "Second question")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("embedding"),
                result2.getOutputData().get("embedding"));
    }

    @Test
    void handlesEmptyQuestion() {
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
