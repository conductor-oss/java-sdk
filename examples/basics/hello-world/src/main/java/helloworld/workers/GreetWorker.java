package helloworld.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Simple worker that greets a user by name.
 */
public class GreetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "greet";
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");
        if (name == null || name.isBlank()) {
            name = "World";
        }

        System.out.println("  [greet worker] Hello, " + name + "!");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("greeting", "Hello, " + name + "! Welcome to Conductor.");
        return result;
    }
}
