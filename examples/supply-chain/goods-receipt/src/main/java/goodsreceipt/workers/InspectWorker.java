package goodsreceipt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class InspectWorker implements Worker {
    @Override public String getTaskDefName() { return "grc_inspect"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("receivedItems");
        if (items == null) items = List.of();

        System.out.println("  [inspect] Inspected " + items.size() + " items — all passed quality check");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("inspectedItems", items);
        result.getOutputData().put("passed", true);
        result.getOutputData().put("defectRate", 0.01);
        return result;
    }
}
