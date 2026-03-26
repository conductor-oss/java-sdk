package workflowoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfoIdentifyWasteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfo_identify_waste";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [waste] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("wasteItems", java.util.List.of(java.util.Map.of("type", "sequential_independent", "tasks", java.util.List.of("enrich", "transform"))));
        result.getOutputData().put("independentTasks", java.util.List.of(java.util.List.of("enrich", "transform")));
        result.getOutputData().put("totalWasteMs", 300);
        return result;
    }
}