package dockersetup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Simple worker used to verify a Conductor Docker setup is working.
 * Takes a "message" input, returns the message along with a timestamp.
 */
public class DockerTestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "docker_test_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String message = (String) task.getInputData().get("message");
        if (message == null || message.isBlank()) {
            message = "Docker setup test";
        }

        System.out.println("  [docker_test_task worker] " + message);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("message", message);
        result.getOutputData().put("timestamp", Instant.now().toString());
        return result;
    }
}
