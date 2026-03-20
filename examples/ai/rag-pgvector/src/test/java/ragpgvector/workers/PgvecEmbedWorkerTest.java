package ragpgvector.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PgvecEmbedWorkerTest {

    private final PgvecEmbedWorker worker = new PgvecEmbedWorker();

    @Test
    void taskDefName() {
        assertEquals("pgvec_embed", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How does pgvector work?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object embeddingObj = result.getOutputData().get("embedding");
        assertNotNull(embeddingObj);
        assertInstanceOf(List.class, embeddingObj);

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) embeddingObj;
        assertEquals(8, embedding.size());
        assertEquals(0.1234, embedding.get(0));
        assertEquals(-0.5678, embedding.get(1));
    }

    @Test
    void embeddingIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("question", "First question")));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "Second question")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("embedding"), result2.getOutputData().get("embedding"));
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(new HashMap<>(Map.of("question", "   ")));
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
