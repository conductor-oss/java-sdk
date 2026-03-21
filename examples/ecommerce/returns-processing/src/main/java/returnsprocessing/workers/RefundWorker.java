package returnsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RefundWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "ret_refund";
    }

    @Override
    public TaskResult execute(Task task) {
        String refundId = "REF-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();
        System.out.println("  [refund] Return " + task.getInputData().get("returnId")
                + ": $" + task.getInputData().get("amount") + " refunded to customer "
                + task.getInputData().get("customerId") + " -> " + refundId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("refundId", refundId);
        output.put("refunded", true);
        output.put("amount", task.getInputData().get("amount"));
        result.setOutputData(output);
        return result;
    }
}
