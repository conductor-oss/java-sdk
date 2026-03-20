package experimenttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExtRunExperimentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ext_run_experiment";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [run] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metrics", java.util.Map.of("accuracy", 0.945, "precision", 0.938, "recall", 0.951));
        result.getOutputData().put("primaryMetric", 0.945);
        return result;
    }
}