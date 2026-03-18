package creatingworkers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Pure synchronous worker that transforms text input.
 *
 * Takes a text string and returns uppercase, lowercase, and character length.
 * Demonstrates the simplest worker pattern: no I/O, no side effects, pure logic.
 */
public class SimpleTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "simple_transform";
    }

    @Override
    public TaskResult execute(Task task) {
        String text = (String) task.getInputData().get("text");
        if (text == null || text.isBlank()) {
            text = "";
        }

        System.out.println("  [simple_transform] Transforming: \"" + text + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("upper", text.toUpperCase());
        result.getOutputData().put("lower", text.toLowerCase());
        result.getOutputData().put("length", text.length());
        result.getOutputData().put("original", text);
        return result;
    }
}
