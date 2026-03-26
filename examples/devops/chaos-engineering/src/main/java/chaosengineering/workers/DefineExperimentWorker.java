package chaosengineering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DefineExperimentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ce_define_experiment";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [define] Experiment: latency-injection on payment-service");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("define_experimentId", "DEFINE_EXPERIMENT-1332");
        result.addOutputData("success", true);
        return result;
    }
}
