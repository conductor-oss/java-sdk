package publichealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RespondWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "phw_respond";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String action = (String) task.getInputData().get("action");
        System.out.printf("  [respond] Response plan activated for %s: %s%n", region, action);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responseActivated", true);
        return result;
    }
}
