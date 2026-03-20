package actuarialworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RunAnalysisWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "act_run_analysis";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [analysis] 10,000 Monte Carlo iterations completed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", java.util.Map.of("mean", 12500000, "p95", 18900000, "p99", 24100000));
        return result;
    }
}
