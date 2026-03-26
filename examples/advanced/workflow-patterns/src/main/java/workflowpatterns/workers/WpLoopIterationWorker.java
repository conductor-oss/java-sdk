package workflowpatterns.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Loop iteration: processes one iteration in a DO_WHILE loop.
 * Input: iteration, merged
 * Output: iteration, processed
 */
public class WpLoopIterationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wp_loop_iteration";
    }

    @Override
    public TaskResult execute(Task task) {
        Object iterObj = task.getInputData().get("iteration");
        int iter = 0;
        if (iterObj instanceof Number) {
            iter = ((Number) iterObj).intValue();
        }

        System.out.println("  [loop] Iteration " + iter + " -- processing merged data");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("iteration", iter);
        result.getOutputData().put("processed", true);
        return result;
    }
}
