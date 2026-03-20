package multidocumentrag.workers;

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
        assertEquals("mdrag_embed", worker.getTaskDefName());
    }

    @Test
    void returnsEmbeddingVector() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How do I create a workflow?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object embedding = result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertInstanceOf(List.class, embedding);
        @SuppressWarnings("unchecked")
        List<Double> vec = (List<Double>) embedding;
        assertEquals(8, vec.size());
    }

    @Test
    void embeddingIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("question", "test")));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("embedding"), r2.getOutputData().get("embedding"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
