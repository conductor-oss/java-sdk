package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyTicketWorkerTest {

    private final ClassifyTicketWorker worker = new ClassifyTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_classify_ticket", worker.getTaskDefName());
    }

    @Test
    void classifiesAsBugWhenErrorKeyword() {
        Task task = taskWith(Map.of("subject", "Login error", "description", "App crashes on startup"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("bug", result.getOutputData().get("category"));
        assertEquals("high", result.getOutputData().get("severity"));
        assertEquals(0.94, result.getOutputData().get("confidence"));

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertNotNull(keywords);
        assertTrue(keywords.contains("error"));
        assertTrue(keywords.contains("crash"));
    }

    @Test
    void classifiesAsBugWithCrashKeyword() {
        Task task = taskWith(Map.of("subject", "System crash", "description", "Page fails to load"));
        TaskResult result = worker.execute(task);

        assertEquals("bug", result.getOutputData().get("category"));
        assertEquals("high", result.getOutputData().get("severity"));

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertTrue(keywords.contains("crash"));
        assertTrue(keywords.contains("fail"));
    }

    @Test
    void classifiesAsFeature() {
        Task task = taskWith(Map.of("subject", "Feature request", "description", "Please add dark mode"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("feature", result.getOutputData().get("category"));
        assertEquals("medium", result.getOutputData().get("severity"));

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertNotNull(keywords);
        assertTrue(keywords.contains("feature"));
        assertTrue(keywords.contains("request"));
        assertTrue(keywords.contains("add"));
    }

    @Test
    void classifiesAsGeneral() {
        Task task = taskWith(Map.of("subject", "How do I reset my password?", "description", "Need help with account"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
        assertEquals("low", result.getOutputData().get("severity"));

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertNotNull(keywords);
        assertTrue(keywords.contains("inquiry"));
    }

    @Test
    void confidenceAlways094() {
        Task task = taskWith(Map.of("subject", "anything", "description", "something"));
        TaskResult result = worker.execute(task);
        assertEquals(0.94, result.getOutputData().get("confidence"));
    }

    @Test
    void handlesNullSubjectAndDescription() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", null);
        input.put("description", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("category"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("category"));
        assertNotNull(result.getOutputData().get("severity"));
        assertNotNull(result.getOutputData().get("keywords"));
        assertNotNull(result.getOutputData().get("confidence"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("subject", "Test", "description", "Test desc"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("category"));
        assertTrue(result.getOutputData().containsKey("severity"));
        assertTrue(result.getOutputData().containsKey("keywords"));
        assertTrue(result.getOutputData().containsKey("confidence"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
