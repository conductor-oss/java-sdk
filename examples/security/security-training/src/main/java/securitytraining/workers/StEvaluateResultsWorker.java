package securitytraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Evaluates training completion and phishing process results.
 * Input: evaluate_resultsData (from phishing step)
 * Output: evaluate_results, processed
 */
public class StEvaluateResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_evaluate_results";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [evaluate] 42/45 completed training, 8% phishing susceptibility");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("evaluate_results", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
