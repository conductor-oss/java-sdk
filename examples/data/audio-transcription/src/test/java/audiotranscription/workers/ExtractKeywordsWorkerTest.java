package audiotranscription.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractKeywordsWorkerTest {

    private final ExtractKeywordsWorker worker = new ExtractKeywordsWorker();

    @Test
    void taskDefName() {
        assertEquals("au_extract_keywords", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsKeywords() {
        Task task = taskWith(Map.of("transcript", "Some transcript text about machine learning"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("machine learning"));
    }

    @Test
    void countsKeywords() {
        Task task = taskWith(Map.of("transcript", "text"));
        TaskResult result = worker.execute(task);

        int count = (int) result.getOutputData().get("keywordCount");
        assertEquals(6, count);
    }

    @SuppressWarnings("unchecked")
    @Test
    void keywordsContainExpectedTerms() {
        Task task = taskWith(Map.of("transcript", "test"));
        TaskResult result = worker.execute(task);

        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertTrue(keywords.contains("infrastructure"));
        assertTrue(keywords.contains("scalability"));
    }

    @Test
    void handlesEmptyTranscript() {
        Task task = taskWith(Map.of("transcript", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("keywords"));
    }

    @Test
    void handlesDefaultTranscript() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
