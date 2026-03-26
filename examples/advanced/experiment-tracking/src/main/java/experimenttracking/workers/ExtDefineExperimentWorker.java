package experimenttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExtDefineExperimentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ext_define_experiment";
    }

    @Override
    public TaskResult execute(Task task) {
        String expName = (String) task.getInputData().getOrDefault("experimentName", "exp");
        System.out.println("  [define] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("experimentId", "EXP-" + System.currentTimeMillis());
        result.getOutputData().put("config", java.util.Map.of("epochs", 20, "batchSize", 64, "optimizer", "adam"));
        return result;
    }
}