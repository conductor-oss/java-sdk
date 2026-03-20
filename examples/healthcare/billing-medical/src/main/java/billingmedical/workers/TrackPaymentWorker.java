package billingmedical.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class TrackPaymentWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mbl_track_payment"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking payment for claim " + task.getInputData().get("claimId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("paymentStatus", "pending");
        output.put("expectedPaymentDate", "2024-04-01");
        output.put("eraReceived", false);
        result.setOutputData(output);
        return result;
    }
}
