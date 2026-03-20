package featureflagrollout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreateFlagWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_create_flag"; }

    @Override
    public TaskResult execute(Task task) {
        String flagName = (String) task.getInputData().get("flagName");
        if (flagName == null) flagName = "unknown-flag";

        System.out.println("  [flag] Created: " + flagName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("flagId", "FLAG-20260115");
        result.getOutputData().put("created", true);
        result.getOutputData().put("flagName", flagName);
        return result;
    }
}
