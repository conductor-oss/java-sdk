package subscriptionbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActivateWorker implements Worker {

    @Override
    public String getTaskDefName() { return "sub_activate"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [activate] Subscription " + task.getInputData().get("subscriptionId")
                + ": next period starts " + task.getInputData().get("nextPeriodStart"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("active", true);
        output.put("nextPeriodStart", task.getInputData().get("nextPeriodStart"));
        output.put("renewedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
