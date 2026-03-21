package ragredis.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisEmbedWorkerTest {

    private final RedisEmbedWorker worker = new RedisEmbedWorker();

    @Test
    void taskDefName() {
        assertEquals("redis_embed", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How does Redis handle vector search?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object embedding = result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertInstanceOf(List.class, embedding);

        @SuppressWarnings("unchecked")
        List<Double> embeddingList = (List<Double>) embedding;
        assertEquals(8, embeddingList.size());
    }

    @Test
    void embeddingIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("question", "first question")));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "second question")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("embedding"), result2.getOutputData().get("embedding"));
    }

    @Test
    void embeddingContainsExpectedValues() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertEquals(0.1234, embedding.get(0));
        assertEquals(-0.5678, embedding.get(1));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
