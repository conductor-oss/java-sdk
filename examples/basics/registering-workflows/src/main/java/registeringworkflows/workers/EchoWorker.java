package registeringworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Simple worker that echoes an input message back as output.
 */
public class EchoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "echo_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String message = (String) task.getInputData().get("message");
        if (message == null) {
            message = "(no message)";
        }

        System.out.println("  [echo_task worker] " + message);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", message);
        return result;
    }
}
