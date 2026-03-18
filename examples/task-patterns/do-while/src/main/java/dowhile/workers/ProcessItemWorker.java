package dowhile.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a single item within the DO_WHILE loop.
 * Takes the current iteration number and returns the next iteration
 * along with a processing result.
 */
public class ProcessItemWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dw_process_item";
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

        System.out.println("  [dw_process_item] Processing item at iteration: " + iteration);

        int nextIteration = iteration + 1;
        String result = "Item-" + nextIteration + " processed";

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("itemProcessed", true);
        taskResult.getOutputData().put("iteration", nextIteration);
        taskResult.getOutputData().put("result", result);
        return taskResult;
    }
}
