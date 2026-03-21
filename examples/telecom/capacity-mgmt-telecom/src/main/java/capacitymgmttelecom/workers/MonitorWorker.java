package capacitymgmttelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmt_monitor";
    }

    @Override
    public TaskResult execute(Task task) {

        String region = (String) task.getInputData().get("region");
        System.out.printf("  [monitor] Region %s: 78%% utilization%n", region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("utilization", 78);
        result.getOutputData().put("growthRate", 5.2);
        result.getOutputData().put("peakHours", "18:00-22:00");
        return result;
    }
}
