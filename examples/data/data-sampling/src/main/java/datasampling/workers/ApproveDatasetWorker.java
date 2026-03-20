package datasampling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves a dataset that passed quality checks.
 * Input: totalRecords (int), qualityScore (double)
 * Output: approved (true), status ("APPROVED")
 */
public class ApproveDatasetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_approve_dataset";
    }

    @Override
    public TaskResult execute(Task task) {
        Object totalRecords = task.getInputData().get("totalRecords");
        Object qualityScore = task.getInputData().get("qualityScore");

        System.out.println("  [sm_approve_dataset] Approving dataset: " + totalRecords
                + " records, quality score: " + qualityScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("status", "APPROVED");
        return result;
    }
}
