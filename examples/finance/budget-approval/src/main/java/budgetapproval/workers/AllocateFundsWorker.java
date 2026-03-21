package budgetapproval.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AllocateFundsWorker implements Worker {
    @Override public String getTaskDefName() { return "bgt_allocate_funds"; }
    @Override public TaskResult execute(Task task) {
        String decision = (String) task.getInputData().get("decision");
        boolean allocated = "approve".equals(decision) || "revise".equals(decision);
        System.out.println("  [allocate] Department " + task.getInputData().get("department") + ": " + (allocated ? "funds allocated" : "no allocation"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("allocationStatus", allocated ? "allocated" : "not_allocated");
        return r;
    }
}
