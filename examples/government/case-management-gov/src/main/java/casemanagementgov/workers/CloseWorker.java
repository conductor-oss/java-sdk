package casemanagementgov.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CloseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmg_close";
    }

    @Override
    public TaskResult execute(Task task) {
        String caseId = (String) task.getInputData().get("caseId");
        String decision = (String) task.getInputData().get("decision");
        System.out.printf("  [close] Case %s closed with %s%n", caseId, decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closedAt", "2024-03-15");
        result.getOutputData().put("archived", true);
        return result;
    }
}
