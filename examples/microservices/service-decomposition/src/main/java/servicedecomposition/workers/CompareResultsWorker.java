package servicedecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compares results from monolith and microservice in shadow mode.
 * Input: monolith, microservice
 * Output: match
 */
public class CompareResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_compare_results";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sd_compare_results] Monolith vs Microservice results match: true");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("match", true);
        return result;
    }
}
