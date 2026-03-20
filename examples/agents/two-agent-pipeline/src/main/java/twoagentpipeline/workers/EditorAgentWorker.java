package twoagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Editor agent that reviews and improves a draft.
 * Takes draft, tone, and systemPrompt as inputs and returns editedContent,
 * notes, changesCount, and model identifier.
 */
public class EditorAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tap_editor_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String draft = (String) task.getInputData().getOrDefault("draft", "");
        String tone = (String) task.getInputData().getOrDefault("tone", "professional");
        String systemPrompt = (String) task.getInputData().getOrDefault("systemPrompt",
                "You are a meticulous editor.");

        String editedContent = "EDITED: " + draft
                + " [Refined for clarity, coherence, grammar, and " + tone + " tone]";

        String notes = "Improved sentence structure for better flow. "
                + "Corrected grammatical inconsistencies. "
                + "Enhanced vocabulary to match " + tone + " tone. "
                + "Added transitional phrases for coherence.";

        System.out.println("  [editor-agent] Edited draft with 4 changes, tone: " + tone);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("editedContent", editedContent);
        result.getOutputData().put("notes", notes);
        result.getOutputData().put("changesCount", 4);
        result.getOutputData().put("model", "editor-agent-v1");
        return result;
    }
}
