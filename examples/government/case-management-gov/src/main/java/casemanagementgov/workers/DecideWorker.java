package casemanagementgov.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DecideWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmg_decide";
    }

    @Override
    public TaskResult execute(Task task) {
        String caseId = (String) task.getInputData().get("caseId");
        System.out.printf("  [decide] Decision: corrective action required for case %s%n", caseId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", "corrective-action");
        result.getOutputData().put("actionPlan", "compliance-training");
        return result;
    }
}
