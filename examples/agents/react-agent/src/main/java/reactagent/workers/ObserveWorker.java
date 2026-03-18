package reactagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Observes the result of the action and determines whether the
 * information is useful for answering the question. Always marks
 * the observation as useful.
 */
public class ObserveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rx_observe";
    }

    @Override
    public TaskResult execute(Task task) {
        String actionResult = (String) task.getInputData().get("actionResult");
        if (actionResult == null) {
            actionResult = "";
        }

        Object iterationObj = task.getInputData().get("iteration");
        int iteration = 1;
        if (iterationObj instanceof Number) {
            iteration = ((Number) iterationObj).intValue();
        }

        String observation = "Iteration " + iteration + ": " + actionResult;

        System.out.println("  [rx_observe] " + observation);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("observation", observation);
        result.getOutputData().put("useful", true);
        return result;
    }
}
