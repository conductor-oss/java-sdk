package purchaseorder.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "po_track";
    }

    @Override
    public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");

        System.out.println("  [track] " + poNumber + ": vendor confirmed, shipping in 5 days");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "confirmed");
        result.getOutputData().put("eta", "5 days");
        return result;
    }
}
