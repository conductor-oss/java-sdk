package twoagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EditorAgentWorkerTest {

    private final EditorAgentWorker worker = new EditorAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("tap_editor_agent", worker.getTaskDefName());
    }

    @Test
    void returnsEditedContent() {
        Task task = taskWith(Map.of(
                "draft", "Some initial draft text.",
                "tone", "formal",
                "systemPrompt", "Edit carefully."
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String editedContent = (String) result.getOutputData().get("editedContent");
        assertNotNull(editedContent);
        assertTrue(editedContent.contains("Some initial draft text."));
        assertTrue(editedContent.contains("formal"));
    }

    @Test
    void returnsNotes() {
        Task task = taskWith(Map.of(
                "draft", "A draft.",
                "tone", "casual",
                "systemPrompt", "Be thorough."
        ));
        TaskResult result = worker.execute(task);

        String notes = (String) result.getOutputData().get("notes");
        assertNotNull(notes);
        assertTrue(notes.contains("casual"));
        assertTrue(notes.contains("Improved sentence structure"));
    }

    @Test
    void returnsChangesCount() {
        Task task = taskWith(Map.of(
                "draft", "Draft content.",
                "tone", "professional",
                "systemPrompt", "Standard editor."
        ));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("changesCount"));
    }

    @Test
    void returnsModel() {
        Task task = taskWith(Map.of(
                "draft", "Draft.",
                "tone", "neutral",
                "systemPrompt", "Edit."
        ));
        TaskResult result = worker.execute(task);

        assertEquals("editor-agent-v1", result.getOutputData().get("model"));
    }

    @Test
    void usesDefaultsWhenInputMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String editedContent = (String) result.getOutputData().get("editedContent");
        assertNotNull(editedContent);
        assertTrue(editedContent.contains("professional"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
