package ragmilvus.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MilvusEmbedWorkerTest {

    private final MilvusEmbedWorker worker = new MilvusEmbedWorker();

    @Test
    void taskDefName() {
        assertEquals("milvus_embed", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEmbedding() {
        Task task = taskWith(Map.of("question", "What index types does Milvus support?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object embedding = result.getOutputData().get("embedding");
        assertInstanceOf(List.class, embedding);

        @SuppressWarnings("unchecked")
        List<Double> embeddingList = (List<Double>) embedding;
        assertEquals(8, embeddingList.size());
        assertEquals(0.021, embeddingList.get(0));
        assertEquals(-0.034, embeddingList.get(1));
    }

    @Test
    void returnsDimensionAndModel() {
        Task task = taskWith(Map.of("question", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(8, result.getOutputData().get("dimension"));
        assertEquals("text-embedding-ada-002", result.getOutputData().get("model"));
    }

    @Test
    void handlesEmptyQuestion() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
    }

    @Test
    void embeddingIsDeterministic() {
        Task task1 = taskWith(Map.of("question", "first"));
        Task task2 = taskWith(Map.of("question", "second"));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("embedding"),
                     result2.getOutputData().get("embedding"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
