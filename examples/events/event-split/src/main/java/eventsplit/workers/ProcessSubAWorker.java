package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes sub-event A (order details).
 * Input: subEvent
 * Output: result ("order_validated"), subType ("order_details")
 */
public class ProcessSubAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_process_sub_a";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> subEvent = (Map<String, Object>) task.getInputData().get("subEvent");
        String subType = subEvent != null ? (String) subEvent.get("type") : "unknown";

        System.out.println("  [sp_process_sub_a] Processing: " + subType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "order_validated");
        result.getOutputData().put("subType", "order_details");
        return result;
    }
}
