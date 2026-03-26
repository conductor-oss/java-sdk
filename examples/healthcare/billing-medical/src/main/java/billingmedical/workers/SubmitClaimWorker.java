package billingmedical.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SubmitClaimWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mbl_submit_claim"; }

    @Override
    public TaskResult execute(Task task) {
        double total = 0;
        try { total = Double.parseDouble(String.valueOf(task.getInputData().getOrDefault("totalCharge", "0"))); }
        catch (NumberFormatException ignored) {}
        double coverage = 80;
        try { coverage = Double.parseDouble(String.valueOf(task.getInputData().getOrDefault("coverage", "80"))); }
        catch (NumberFormatException ignored) {}
        double expected = Math.round(total * (coverage / 100) * 100.0) / 100.0;
        System.out.println("  [submit] Submitting claim: $" + total + " total, expected insurance payment: $" + expected);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("claimId", "CLM-EDI-78901");
        output.put("expectedPayment", expected);
        output.put("submittedAt", Instant.now().toString());
        output.put("clearinghouse", "Availity");
        result.setOutputData(output);
        return result;
    }
}
