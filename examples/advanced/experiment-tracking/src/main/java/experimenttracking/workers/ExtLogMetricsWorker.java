package experimenttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExtLogMetricsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ext_log_metrics";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [log] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("runUrl", "https://mlflow.example.com/runs/" + task.getInputData().getOrDefault("experimentId", ""));
        return result;
    }
}