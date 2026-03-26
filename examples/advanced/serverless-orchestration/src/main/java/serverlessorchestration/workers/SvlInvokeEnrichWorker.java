package serverlessorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Invokes the enrich serverless function to add user context.
 * Input: functionArn, parsedData
 * Output: enriched (userTier, sessionCount, geo), billedMs
 */
public class SvlInvokeEnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "svl_invoke_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [lambda:enrich] Enriching parsed data with user context");

        Map<String, Object> enriched = Map.of(
                "userTier", "premium",
                "sessionCount", 42,
                "geo", "US"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", enriched);
        result.getOutputData().put("billedMs", 120);
        return result;
    }
}
