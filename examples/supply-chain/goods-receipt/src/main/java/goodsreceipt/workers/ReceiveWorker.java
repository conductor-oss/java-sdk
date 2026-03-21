package goodsreceipt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "grc_receive"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String shipmentId = (String) task.getInputData().get("shipmentId");
        List<Map<String, Object>> items = (List<Map<String, Object>>) task.getInputData().get("items");
        if (items == null) items = List.of();

        System.out.println("  [receive] Shipment " + shipmentId + ": " + items.size() + " line items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("receiptId", "GR-655-001");
        result.getOutputData().put("receivedItems", items);
        return result;
    }
}
