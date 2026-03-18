package compensationworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Worker for comp_undo_a -- reverses the action performed by Step A
 * by deleting the temp file that was created.
 */
public class CompUndoAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_undo_a";
    }

    @Override
    public TaskResult execute(Task task) {
        Object original = task.getInputData().get("original");
        System.out.println("  [Undo A] Reversing: " + original);

        boolean deleted = false;
        // Try to parse the original as a file path and delete it
        if (original instanceof String) {
            String pathStr = (String) original;
            // Check if it looks like a resource path from CompStepAWorker
            if (pathStr.contains("comp-resource-A-")) {
                try {
                    Path path = Path.of(pathStr);
                    deleted = Files.deleteIfExists(path);
                    System.out.println("  [Undo A] Deleted file: " + deleted);
                } catch (Exception e) {
                    System.out.println("  [Undo A] Could not delete file: " + e.getMessage());
                }
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("undone", true);
        result.getOutputData().put("deleted", deleted);
        return result;
    }
}
