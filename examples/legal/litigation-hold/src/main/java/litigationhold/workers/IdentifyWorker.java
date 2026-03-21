package litigationhold.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IdentifyWorker implements Worker {
    @Override public String getTaskDefName() { return "lth_identify"; }

    @Override public TaskResult execute(Task task) {
        String caseId = (String) task.getInputData().get("caseId");
        System.out.println("  [identify] Identifying custodians for case " + caseId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("holdId", "HOLD-" + caseId + "-001");
        result.getOutputData().put("identifiedCustodians", task.getInputData().get("custodians"));
        result.getOutputData().put("dataSources", java.util.List.of("email", "slack", "drive"));
        return result;
    }
}
