package litigationhold.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "lth_collect"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collecting data for case " + task.getInputData().get("caseId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collectedData", java.util.Map.of("documents", 1250, "emails", 8400, "messages", 3200));
        result.getOutputData().put("totalItems", 12850);
        return result;
    }
}
