package twoagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Final output worker that assembles the pipeline result.
 * Takes originalDraft, editedContent, and editorNotes as inputs.
 * Returns finalContent and a summary map with metadata about the pipeline run.
 */
public class FinalOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tap_final_output";
    }

    @Override
    public TaskResult execute(Task task) {
        String originalDraft = (String) task.getInputData().getOrDefault("originalDraft", "");
        String editedContent = (String) task.getInputData().getOrDefault("editedContent", "");
        String editorNotes = (String) task.getInputData().getOrDefault("editorNotes", "");

        Map<String, Object> summary = Map.of(
                "originalLength", originalDraft.length(),
                "editedLength", editedContent.length(),
                "editorNotes", editorNotes,
                "agentsPipeline", List.of("writer-agent-v1", "editor-agent-v1")
        );

        System.out.println("  [final-output] Assembled pipeline result: original="
                + originalDraft.length() + " chars, edited=" + editedContent.length() + " chars");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalContent", editedContent);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
