package backpressure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BkpShedLoadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bkp_shed_load";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [shed] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "load_shed");
        result.getOutputData().put("shedPercent", task.getInputData().getOrDefault("shedPercent", 50));
        return result;
    }
}