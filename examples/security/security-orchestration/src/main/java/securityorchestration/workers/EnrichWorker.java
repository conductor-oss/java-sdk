package securityorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "soar_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enrich] Added threat intel, asset context, user history");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("enrich", true);
        result.addOutputData("processed", true);
        return result;
    }
}
