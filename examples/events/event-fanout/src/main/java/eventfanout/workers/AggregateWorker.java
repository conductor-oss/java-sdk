package eventfanout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Aggregates results from the three parallel fan-out branches.
 * Input: analyticsResult, storageResult, notifyResult
 * Output: status:"all_completed", processorResults:[the 3 results]
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        Object analyticsResult = task.getInputData().get("analyticsResult");
        if (analyticsResult == null) {
            analyticsResult = "none";
        }

        Object storageResult = task.getInputData().get("storageResult");
        if (storageResult == null) {
            storageResult = "none";
        }

        Object notifyResult = task.getInputData().get("notifyResult");
        if (notifyResult == null) {
            notifyResult = "none";
        }

        System.out.println("  [fo_aggregate] Aggregating results: analytics=" + analyticsResult
                + ", storage=" + storageResult + ", notify=" + notifyResult);

        List<Object> processorResults = List.of(analyticsResult, storageResult, notifyResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "all_completed");
        result.getOutputData().put("processorResults", processorResults);
        return result;
    }
}
