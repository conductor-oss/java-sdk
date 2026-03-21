package casemanagementgov.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OpenCaseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmg_open_case";
    }

    @Override
    public TaskResult execute(Task task) {
        String caseType = (String) task.getInputData().get("caseType");
        String reporterId = (String) task.getInputData().get("reporterId");
        System.out.printf("  [open] Case opened: %s by %s%n", caseType, reporterId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("caseId", "CASE-case-management-gov-001");
        result.getOutputData().put("openedAt", "2024-03-01");
        return result;
    }
}
