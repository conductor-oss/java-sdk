package bluegreendeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Switches traffic from blue to green environment.
 * Input: serviceName, from, to
 * Output: switched, previousActive
 */
public class SwitchTrafficWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bg_switch_traffic";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        String from = (String) task.getInputData().get("from");
        if (from == null) from = "blue";

        String to = (String) task.getInputData().get("to");
        if (to == null) to = "green";

        System.out.println("  [switch] Traffic shifted: " + from + " -> " + to);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("switched", true);
        result.getOutputData().put("previousActive", from);
        result.getOutputData().put("currentActive", to);
        return result;
    }
}
