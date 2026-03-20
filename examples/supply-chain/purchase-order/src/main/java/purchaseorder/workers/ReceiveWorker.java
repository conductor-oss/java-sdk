package purchaseorder.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReceiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "po_receive";
    }

    @Override
    public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        String trackingStatus = (String) task.getInputData().get("trackingStatus");

        System.out.println("  [receive] Goods for " + poNumber + " received — status: " + trackingStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("received", true);
        result.getOutputData().put("condition", "good");
        result.getOutputData().put("matchesPO", true);
        return result;
    }
}
