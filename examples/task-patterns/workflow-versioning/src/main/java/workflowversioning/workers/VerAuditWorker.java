package workflowversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Audit worker for the versioned workflow.
 * Takes the final result, marks it as audited, and passes it through.
 */
public class VerAuditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ver_audit";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawFinalResult = task.getInputData().get("finalResult");
        int finalResult = (rawFinalResult instanceof Number) ? ((Number) rawFinalResult).intValue() : 0;

        System.out.println("  [ver_audit] Auditing result: " + finalResult + " -> audited=true");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", finalResult);
        result.getOutputData().put("audited", true);
        return result;
    }
}
