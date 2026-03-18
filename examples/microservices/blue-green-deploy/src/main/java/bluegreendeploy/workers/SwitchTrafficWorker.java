package bluegreendeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class SwitchTrafficWorker implements Worker {
    @Override public String getTaskDefName() { return "bg_switch_traffic"; }

    @Override public TaskResult execute(Task task) {
        String from = (String) task.getInputData().getOrDefault("from", "blue");
        String to = (String) task.getInputData().getOrDefault("to", "green");
        System.out.println("  [switch] Switching traffic from " + from + " to " + to + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("activeEnv", to);
        result.getOutputData().put("previousEnv", from);
        result.getOutputData().put("switchedAt", Instant.now().toString());
        result.getOutputData().put("dnsUpdated", true);
        return result;
    }
}
