package forkindowhile.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a batch within the DO_WHILE loop.
 * Takes the current iteration number and returns a batchId
 * along with a processed flag.
 */
public class ProcessBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fl_process_batch";
    }

    @Override
    public TaskResult execute(Task task) {
        Object iterationObj = task.getInputData().get("iteration");
        int iteration;
        if (iterationObj instanceof Number) {
            iteration = ((Number) iterationObj).intValue();
        } else {
            iteration = 0;
        }

        int batchId = iteration + 1;

        System.out.println("  [fl_process_batch] Processing batch at iteration: " + iteration + " -> batchId: " + batchId);

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("batchId", batchId);
        taskResult.getOutputData().put("processed", true);
        return taskResult;
    }
}
