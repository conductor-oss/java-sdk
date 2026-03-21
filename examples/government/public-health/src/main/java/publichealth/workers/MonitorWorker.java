package publichealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "phw_monitor";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String nextCheckDate = (String) task.getInputData().get("nextCheckDate");
        System.out.printf("  [monitor] Continued monitoring of %s until %s%n", region, nextCheckDate);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("monitoring", true);
        return result;
    }
}
