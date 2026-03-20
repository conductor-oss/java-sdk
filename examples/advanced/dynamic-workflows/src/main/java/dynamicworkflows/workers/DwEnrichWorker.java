package dynamicworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enrich step in a dynamic pipeline.
 * Input: stepId, stepType, config, previousOutput
 * Output: result, stepType
 */
public class DwEnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        String config = (String) task.getInputData().get("config");
        if (config == null) config = "{}";

        System.out.println("  [enrich] Executing enrichment step (config: " + config + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "enrich_complete");
        result.getOutputData().put("stepType", "enrichment");
        return result;
    }
}
