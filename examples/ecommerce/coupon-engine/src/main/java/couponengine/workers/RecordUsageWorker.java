package couponengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class RecordUsageWorker implements Worker {
    private static final AtomicLong COUNTER = new AtomicLong();
    @Override public String getTaskDefName() { return "cpn_record_usage"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [record] Code \"" + task.getInputData().get("couponCode")
                + "\" used by " + task.getInputData().get("customerId")
                + ": -$" + task.getInputData().get("discountApplied"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("recorded", true);
        output.put("usageId", "usage-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet());
        output.put("recordedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
