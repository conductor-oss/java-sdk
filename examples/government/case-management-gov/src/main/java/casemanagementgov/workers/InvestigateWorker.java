package casemanagementgov.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class InvestigateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmg_investigate";
    }

    @Override
    public TaskResult execute(Task task) {
        String caseId = (String) task.getInputData().get("caseId");
        System.out.printf("  [investigate] Case %s under investigation%n", caseId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", Map.of(
                "evidenceCount", 5,
                "witnesses", 2,
                "severity", "moderate"
        ));
        return result;
    }
}
