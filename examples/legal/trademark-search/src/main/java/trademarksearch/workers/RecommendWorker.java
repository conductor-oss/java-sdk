package trademarksearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecommendWorker implements Worker {
    @Override public String getTaskDefName() { return "tmk_recommend"; }

    @Override public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("trademarkName");
        System.out.println("  [recommend] Generating recommendation for: " + name);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recommendation", "proceed-with-caution");
        result.getOutputData().put("suggestedAlternatives", java.util.List.of(name + "Plus", name + "Pro"));
        return result;
    }
}
