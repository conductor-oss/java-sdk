package casemanagementgov.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmg_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [evaluate] Findings evaluated — moderate severity");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("evaluation", "substantiated");
        result.getOutputData().put("riskLevel", "medium");
        return result;
    }
}
