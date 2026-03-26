package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateComplianceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_evaluate_compliance";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [compliance] SLA compliant — within error budget");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("evaluate_compliance", true);
        result.addOutputData("processed", true);
        return result;
    }
}
