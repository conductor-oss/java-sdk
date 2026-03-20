package twoagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalOutputWorkerTest {

    private final FinalOutputWorker worker = new FinalOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("tap_final_output", worker.getTaskDefName());
    }

    @Test
    void returnsFinalContentAsEditedContent() {
        Task task = taskWith(Map.of(
                "originalDraft", "Original draft text.",
                "editedContent", "Edited and improved text.",
                "editorNotes", "Fixed grammar and tone."
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String finalContent = (String) result.getOutputData().get("finalContent");
        assertEquals("Edited and improved text.", finalContent);
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSummaryWithOriginalLength() {
        Task task = taskWith(Map.of(
                "originalDraft", "Short.",
                "editedContent", "A longer edited version.",
                "editorNotes", "Expanded content."
        ));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals(6, summary.get("originalLength"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSummaryWithEditedLength() {
        Task task = taskWith(Map.of(
                "originalDraft", "Draft.",
                "editedContent", "Edited draft content here.",
                "editorNotes", "Some notes."
        ));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals("Edited draft content here.".length(), summary.get("editedLength"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSummaryWithEditorNotes() {
        Task task = taskWith(Map.of(
                "originalDraft", "Draft.",
                "editedContent", "Edited.",
                "editorNotes", "Improved clarity and flow."
        ));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals("Improved clarity and flow.", summary.get("editorNotes"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSummaryWithAgentsPipeline() {
        Task task = taskWith(Map.of(
                "originalDraft", "Draft.",
                "editedContent", "Edited.",
                "editorNotes", "Notes."
        ));
        TaskResult result = worker.execute(task);

        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);

        List<String> pipeline = (List<String>) summary.get("agentsPipeline");
        assertEquals(List.of("writer-agent-v1", "editor-agent-v1"), pipeline);
    }

    @Test
    void usesDefaultsWhenInputMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("finalContent"));
        assertNotNull(result.getOutputData().get("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
