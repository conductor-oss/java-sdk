package customerchurn.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeReasonsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccn_analyze_reasons";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [analyze] Reasons: price sensitivity, competitor offers, declining usage");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reasons", java.util.List.of("price", "competitor", "usage-decline"));
        result.getOutputData().put("primaryReason", "price");
        return result;
    }
}
