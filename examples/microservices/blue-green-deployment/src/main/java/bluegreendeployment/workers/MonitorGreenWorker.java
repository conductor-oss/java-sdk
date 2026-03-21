package bluegreendeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Monitors the green environment after traffic switch.
 * Input: serviceName, version
 * Output: healthy, errorRate
 */
public class MonitorGreenWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bg_monitor_green";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        String version = (String) task.getInputData().get("version");
        if (version == null) version = "unknown";

        System.out.println("  [monitor] Green running " + version + ": error rate 0.01%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("healthy", true);
        result.getOutputData().put("errorRate", 0.01);
        result.getOutputData().put("version", version);
        return result;
    }
}
