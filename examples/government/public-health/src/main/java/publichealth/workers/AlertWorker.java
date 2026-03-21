package publichealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AlertWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "phw_alert";
    }

    @Override
    public TaskResult execute(Task task) {
        String disease = (String) task.getInputData().get("disease");
        String region = (String) task.getInputData().get("region");
        System.out.printf("  [alert] PUBLIC HEALTH ALERT: %s outbreak in %s%n", disease, region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("alertIssued", true);
        result.getOutputData().put("alertLevel", task.getInputData().get("severity"));
        return result;
    }
}
