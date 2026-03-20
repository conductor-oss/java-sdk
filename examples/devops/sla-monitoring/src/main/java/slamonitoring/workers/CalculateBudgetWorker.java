package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateBudgetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_calculate_budget";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [budget] Error budget: 73% remaining, 21.6 min left this month");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("calculate_budget", true);
        result.addOutputData("processed", true);
        return result;
    }
}
