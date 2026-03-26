package threeagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewerAgentWorkerTest {

    private final ReviewerAgentWorker worker = new ReviewerAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("thr_reviewer_agent", worker.getTaskDefName());
    }

    @Test
    void producesReview() {
        Task task = taskWith(Map.of(
                "draft", "A sample draft article about technology.",
                "research", Map.of("subject", "technology"),
                "audience", "engineers",
                "systemPrompt", "Review the draft"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        assertNotNull(review);
    }

    @Test
    void reviewContainsScore() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "managers"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        assertEquals(8.5, review.get("score"));
        assertEquals(10, review.get("maxScore"));
    }

    @Test
    void reviewContainsQualityAssessments() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "developers"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        assertNotNull(review.get("accuracy"));
        assertNotNull(review.get("clarity"));
        assertNotNull(review.get("audienceFit"));
    }

    @Test
    void reviewAudienceFitReflectsInput() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "healthcare executives"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        String audienceFit = (String) review.get("audienceFit");
        assertTrue(audienceFit.contains("healthcare executives"));
    }

    @Test
    void reviewContainsSuggestions() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "analysts"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) review.get("suggestions");

        assertNotNull(suggestions);
        assertEquals(3, suggestions.size());
    }

    @Test
    void reviewVerdictIsApprovedWithMinorRevisions() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "investors"
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        assertEquals("APPROVED_WITH_MINOR_REVISIONS", review.get("verdict"));
    }

    @Test
    void outputContainsModel() {
        Task task = taskWith(Map.of(
                "draft", "Draft content",
                "audience", "general"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("reviewer-agent-v1", result.getOutputData().get("model"));
    }

    @Test
    void defaultsAudienceWhenMissing() {
        Task task = taskWith(Map.of("draft", "Draft content"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.getOutputData().get("review");
        String audienceFit = (String) review.get("audienceFit");
        assertTrue(audienceFit.contains("general readers"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
