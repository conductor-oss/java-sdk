package returnsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ExchangeWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "ret_exchange";
    }

    @Override
    public TaskResult execute(Task task) {
        String exchangeOrderId = "EXC-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();
        System.out.println("  [exchange] Return " + task.getInputData().get("returnId")
                + ": exchange order " + exchangeOrderId + " created");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("exchangeOrderId", exchangeOrderId);
        output.put("exchanged", true);
        result.setOutputData(output);
        return result;
    }
}
