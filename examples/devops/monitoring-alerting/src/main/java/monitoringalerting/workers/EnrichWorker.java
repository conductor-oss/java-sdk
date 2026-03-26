package monitoringalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class EnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ma_enrich";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enrich] Added deployment context");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("context", Map.of("deployment", true));
        return result;
    }
}
