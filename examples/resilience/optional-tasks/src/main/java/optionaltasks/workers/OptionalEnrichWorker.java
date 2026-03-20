package optionaltasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for opt_optional_enrich — enriches data with additional information.
 *
 * This task is marked as optional in the workflow definition. If it fails,
 * the workflow continues to the next task instead of failing entirely.
 *
 * Returns { enriched: "extra-data-added" } on success.
 */
public class OptionalEnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opt_optional_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [opt_optional_enrich] Enriching data...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", "extra-data-added");
        return result;
    }
}
