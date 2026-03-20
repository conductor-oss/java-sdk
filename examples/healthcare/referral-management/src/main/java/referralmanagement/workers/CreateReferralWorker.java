package referralmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreateReferralWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ref_create"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [create] Referral " + task.getInputData().get("referralId")
                + ": " + task.getInputData().get("specialty") + " — " + task.getInputData().get("reason"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("insurancePlan", "Blue Cross PPO");
        output.put("urgency", "routine");
        output.put("createdAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
