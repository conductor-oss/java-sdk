package referralmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloseReferralWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ref_close"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [close] Closing referral " + task.getInputData().get("referralId")
                + " — outcome: " + task.getInputData().get("outcome"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("closedStatus", "completed");
        output.put("closedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
