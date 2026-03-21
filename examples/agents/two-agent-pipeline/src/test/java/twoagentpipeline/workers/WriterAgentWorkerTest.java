package twoagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriterAgentWorkerTest {

    private final WriterAgentWorker worker = new WriterAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("tap_writer_agent", worker.getTaskDefName());
    }

    @Test
    void returnsDraftAboutTopic() {
        Task task = taskWith(Map.of(
                "topic", "machine learning",
                "tone", "casual",
                "systemPrompt", "Write engagingly."
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String draft = (String) result.getOutputData().get("draft");
        assertNotNull(draft);
        assertTrue(draft.contains("machine learning"));
        assertTrue(draft.contains("casual"));
    }

    @Test
    void returnsWordCount() {
        Task task = taskWith(Map.of(
                "topic", "quantum computing",
                "tone", "professional",
                "systemPrompt", "Be precise."
        ));
        TaskResult result = worker.execute(task);

        assertEquals(35, result.getOutputData().get("wordCount"));
    }

    @Test
    void returnsModel() {
        Task task = taskWith(Map.of(
                "topic", "robotics",
                "tone", "formal",
                "systemPrompt", "Standard prompt."
        ));
        TaskResult result = worker.execute(task);

        assertEquals("writer-agent-v1", result.getOutputData().get("model"));
    }

    @Test
    void usesDefaultsWhenInputMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String draft = (String) result.getOutputData().get("draft");
        assertNotNull(draft);
        assertTrue(draft.contains("technology"));
        assertTrue(draft.contains("professional"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
