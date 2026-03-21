package forkindowhile.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Summarizes the results after the DO_WHILE loop completes.
 * Takes the iterations count and produces a summary string.
 */
public class SummaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fl_summary";
    }

    @Override
    public TaskResult execute(Task task) {
        Object iterationsObj = task.getInputData().get("iterations");
        int iterations;
        if (iterationsObj instanceof Number) {
            iterations = ((Number) iterationsObj).intValue();
        } else {
            iterations = 0;
        }

        String summary = iterations + " batches done";

        System.out.println("  [fl_summary] Summarizing: " + summary);

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("summary", summary);
        return taskResult;
    }
}
