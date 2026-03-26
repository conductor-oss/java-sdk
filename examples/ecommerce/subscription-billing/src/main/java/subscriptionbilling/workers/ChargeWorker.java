package subscriptionbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ChargeWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() { return "sub_charge"; }

    @Override
    public TaskResult execute(Task task) {
        String chargeId = "chg-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();
        System.out.println("  [charge] Invoice " + task.getInputData().get("invoiceId")
                + ": charged $" + task.getInputData().get("amount") + " to customer "
                + task.getInputData().get("customerId") + " -> " + chargeId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("chargeId", chargeId);
        output.put("charged", true);
        output.put("chargedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
