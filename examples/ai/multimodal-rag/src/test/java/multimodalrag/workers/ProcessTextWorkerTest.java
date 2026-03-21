package multimodalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTextWorkerTest {

    private final ProcessTextWorker worker = new ProcessTextWorker();

    @Test
    void taskDefName() {
        assertEquals("mm_process_text", worker.getTaskDefName());
    }

    @Test
    void returnsEightDimensionalEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is multimodal RAG?",
                "textContent", "Extracted text about multimodal retrieval"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertEquals(8, embedding.size());
    }

    @Test
    void returnsKeywords() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "textContent", "Some content"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertNotNull(keywords);
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("multimodal"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>(Map.of(
                "textContent", "Some content"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
    }

    @Test
    void handlesNullTextContent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
        assertNotNull(result.getOutputData().get("keywords"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
