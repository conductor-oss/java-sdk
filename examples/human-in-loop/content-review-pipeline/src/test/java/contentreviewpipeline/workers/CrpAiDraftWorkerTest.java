package contentreviewpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrpAiDraftWorkerTest {

    @Test
    void taskDefName() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        assertEquals("crp_ai_draft", worker.getTaskDefName());
    }

    @Test
    void generatesDraftWithTopicAndAudience() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "AI in Healthcare", "audience", "developers"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Draft content about AI in Healthcare for developers",
                result.getOutputData().get("content"));
        assertEquals(42, result.getOutputData().get("wordCount"));
        assertEquals("claude-3.5", result.getOutputData().get("model"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "Testing", "audience", "QA engineers"));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("content"));
        assertTrue(result.getOutputData().containsKey("wordCount"));
        assertTrue(result.getOutputData().containsKey("model"));
    }

    @Test
    void wordCountIsAlways42() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "Microservices", "audience", "architects"));

        TaskResult result = worker.execute(task);

        assertEquals(42, result.getOutputData().get("wordCount"));
    }

    @Test
    void modelIsAlwaysClaude35() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "DevOps", "audience", "engineers"));

        TaskResult result = worker.execute(task);

        assertEquals("claude-3.5", result.getOutputData().get("model"));
    }

    @Test
    void contentIncludesTopic() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "Kubernetes", "audience", "beginners"));

        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("Kubernetes"));
    }

    @Test
    void contentIncludesAudience() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "Security", "audience", "managers"));

        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("managers"));
    }

    @Test
    void alwaysCompletes() {
        CrpAiDraftWorker worker = new CrpAiDraftWorker();
        Task task = taskWith(Map.of("topic", "Cloud", "audience", "students"));

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
