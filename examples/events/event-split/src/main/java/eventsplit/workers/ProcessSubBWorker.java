package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes sub-event B (customer info).
 * Input: subEvent
 * Output: result ("customer_verified"), subType ("customer_info")
 */
public class ProcessSubBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_process_sub_b";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> subEvent = (Map<String, Object>) task.getInputData().get("subEvent");
        String subType = subEvent != null ? (String) subEvent.get("type") : "unknown";

        System.out.println("  [sp_process_sub_b] Processing: " + subType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "customer_verified");
        result.getOutputData().put("subType", "customer_info");
        return result;
    }
}
