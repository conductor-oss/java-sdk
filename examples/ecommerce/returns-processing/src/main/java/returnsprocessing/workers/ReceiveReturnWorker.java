package returnsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReceiveReturnWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "ret_receive";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String returnId = "RET-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "-" + COUNTER.incrementAndGet();
        List<?> items = (List<?>) task.getInputData().getOrDefault("items", List.of());

        System.out.println("  [receive] Return " + returnId + ": " + items.size()
                + " items from order " + task.getInputData().get("orderId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("returnId", returnId);
        output.put("receivedAt", Instant.now().toString());
        output.put("itemCount", items.size());
        result.setOutputData(output);
        return result;
    }
}
