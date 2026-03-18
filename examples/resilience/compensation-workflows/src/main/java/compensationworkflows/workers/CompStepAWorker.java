package compensationworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Worker for comp_step_a -- creates a real temporary file as a resource.
 * The file path is returned so the undo worker can delete it.
 */
public class CompStepAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_step_a";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            // Create a real temp file to represent "resource A"
            Path tempFile = Files.createTempFile("comp-resource-A-", ".txt");
            Files.writeString(tempFile, "Resource A created at " + Instant.now());

            System.out.println("  [Step A] Executing -- resource created: " + tempFile);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("result", "resource-A-created");
            result.getOutputData().put("resourcePath", tempFile.toString());
        } catch (IOException e) {
            System.out.println("  [Step A] Failed to create resource: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Failed to create resource: " + e.getMessage());
        }
        return result;
    }
}
