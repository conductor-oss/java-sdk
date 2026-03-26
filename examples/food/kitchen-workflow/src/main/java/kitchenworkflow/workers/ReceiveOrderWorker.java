package kitchenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ReceiveOrderWorker implements Worker {
    @Override public String getTaskDefName() { return "kit_receive_order"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receive] Order " + task.getInputData().get("orderId") + " received in kitchen");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("parsedItems", List.of("Salmon", "Risotto", "Salad"));
        result.addOutputData("station", "grill");
        return result;
    }
}
