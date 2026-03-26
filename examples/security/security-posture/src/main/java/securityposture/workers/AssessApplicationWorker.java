package securityposture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessApplicationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_assess_application";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [app] 78/100 — 2 critical vulnerabilities in production");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_application", true);
        result.addOutputData("processed", true);
        return result;
    }
}
