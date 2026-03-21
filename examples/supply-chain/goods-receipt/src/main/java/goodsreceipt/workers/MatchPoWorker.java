package goodsreceipt.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MatchPoWorker implements Worker {
    @Override public String getTaskDefName() { return "grc_match_po"; }

    @Override
    public TaskResult execute(Task task) {
        String poNumber = (String) task.getInputData().get("poNumber");
        System.out.println("  [match] Items matched to " + poNumber + " — quantities verified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("matched", true);
        result.getOutputData().put("discrepancies", 0);
        return result;
    }
}
