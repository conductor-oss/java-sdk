package workerpools.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WplCategorizeTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wpl_categorize_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String category = (String) task.getInputData().getOrDefault("taskCategory", "general");
        java.util.Map<String,String> profiles = java.util.Map.of("compute","high-cpu","io","high-io","general","balanced","ml","gpu");
        String profile = profiles.getOrDefault(category, "balanced");
        System.out.println("  [categorize] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", category);
        result.getOutputData().put("resourceProfile", profile);
        return result;
    }
}