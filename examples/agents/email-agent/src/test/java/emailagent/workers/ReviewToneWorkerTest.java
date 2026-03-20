package emailagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewToneWorkerTest {

    private final ReviewToneWorker worker = new ReviewToneWorker();

    @Test
    void taskDefName() {
        assertEquals("ea_review_tone", worker.getTaskDefName());
    }

    @Test
    void returnsToneScoreOf091() {
        Task task = taskWith(Map.of(
                "subject", "Project Alpha Update",
                "body", "Dear Sarah, milestone completed.",
                "desiredTone", "professional"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.91, result.getOutputData().get("toneScore"));
    }

    @Test
    void returnsToneApprovedTrue() {
        Task task = taskWith(Map.of("subject", "Update", "body", "Hello"));
        TaskResult result = worker.execute(task);

        assertEquals("true", result.getOutputData().get("toneApproved"));
    }

    @Test
    void returnsFinalSubjectMatchingInput() {
        Task task = taskWith(Map.of("subject", "My Subject", "body", "Body text"));
        TaskResult result = worker.execute(task);

        assertEquals("My Subject", result.getOutputData().get("finalSubject"));
    }

    @Test
    void returnsFinalBodyMatchingInput() {
        Task task = taskWith(Map.of("subject", "Subj", "body", "Original body text"));
        TaskResult result = worker.execute(task);

        assertEquals("Original body text", result.getOutputData().get("finalBody"));
    }

    @Test
    void returnsAnalysisMap() {
        Task task = taskWith(Map.of("subject", "Test", "body", "Body"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertNotNull(analysis);
        assertEquals(0.94, analysis.get("professionalism"));
        assertEquals(0.89, analysis.get("clarity"));
    }

    @Test
    void returnsSuggestionsList() {
        Task task = taskWith(Map.of("subject", "Test", "body", "Body"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.getOutputData().get("suggestions");
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
    }

    @Test
    void handlesNullSubject() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", null);
        input.put("body", "Some body");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toneScore"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toneApproved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
