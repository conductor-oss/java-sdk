package workflowarchival.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for the archival demo workflow.
 * Takes a batch identifier and returns done: true.
 */
public class ArchivalTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "arch_task";
    }

    @Override
    public TaskResult execute(Task task) {
        Object batch = task.getInputData().get("batch");
        String batchStr = batch != null ? batch.toString() : "unknown";

        System.out.println("  [arch_task] Processing batch: " + batchStr);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("done", true);
        return result;
    }
}
