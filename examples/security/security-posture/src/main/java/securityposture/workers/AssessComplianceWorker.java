package securityposture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessComplianceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_assess_compliance";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [compliance] 92/100 — access review overdue for 1 department");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_compliance", true);
        result.addOutputData("processed", true);
        return result;
    }
}
