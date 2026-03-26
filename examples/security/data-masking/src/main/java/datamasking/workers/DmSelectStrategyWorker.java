package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Selects appropriate masking strategy based on data purpose.
 * Input: select_strategyData (from identify step)
 * Output: select_strategy, processed
 */
public class DmSelectStrategyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_select_strategy";
    }

    @Override
    public TaskResult execute(Task task) {
        String purpose = (String) task.getInputData().get("purpose");
        if (purpose == null) purpose = "general";

        System.out.println("  [strategy] Masking strategy for " + purpose + ": tokenization + redaction");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("select_strategy", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
