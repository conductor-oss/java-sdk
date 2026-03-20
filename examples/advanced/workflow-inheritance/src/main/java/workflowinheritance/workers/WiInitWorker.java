package workflowinheritance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WiInitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wi_init";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().getOrDefault("requestId", "unknown");
        String variant = (String) task.getInputData().getOrDefault("variant", "standard");
        System.out.println("  [init] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("initialized", true);
        return result;
    }
}