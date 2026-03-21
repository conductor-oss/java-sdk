package threatintelligence.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DistributeIntelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ti_distribute_intel";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [distribute] Intel distributed to SIEM, firewall, and EDR");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("distribute_intel", true);
        return result;
    }
}
