package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes sub-event C (shipping info).
 * Input: subEvent
 * Output: result ("shipping_calculated"), subType ("shipping_info")
 */
public class ProcessSubCWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_process_sub_c";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> subEvent = (Map<String, Object>) task.getInputData().get("subEvent");
        String subType = subEvent != null ? (String) subEvent.get("type") : "unknown";

        System.out.println("  [sp_process_sub_c] Processing: " + subType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "shipping_calculated");
        result.getOutputData().put("subType", "shipping_info");
        return result;
    }
}
