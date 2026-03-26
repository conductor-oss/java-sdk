package purchaseorder.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "po_send";
    }

    @Override
    public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        String vendor = (String) task.getInputData().get("vendor");

        System.out.println("  [send] " + poNumber + " sent to " + vendor);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("method", "EDI");
        return result;
    }
}
