package compliancereporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Collects compliance evidence for a given framework and period.
 * Input: framework, period
 * Output: collect_evidenceId, success
 */
public class CollectEvidenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_collect_evidence";
    }

    @Override
    public TaskResult execute(Task task) {
        String framework = (String) task.getInputData().get("framework");
        if (framework == null) {
            framework = "unknown";
        }

        System.out.println("  [evidence] Collected 156 evidence items for " + framework);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collect_evidenceId", "COLLECT_EVIDENCE-1383");
        result.getOutputData().put("success", true);
        return result;
    }
}
