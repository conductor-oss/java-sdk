package grantmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class FundWorker implements Worker {
    @Override public String getTaskDefName() { return "gmt_fund"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [fund] Disbursing $" + task.getInputData().get("approvedAmount") + " to " + task.getInputData().get("organization"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("grantId", "GRT-752"); r.addOutputData("funded", true); r.addOutputData("disbursedAt", "2026-03-15");
        return r;
    }
}
