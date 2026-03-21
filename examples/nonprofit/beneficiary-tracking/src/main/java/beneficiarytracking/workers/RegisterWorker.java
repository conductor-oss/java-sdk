package beneficiarytracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RegisterWorker implements Worker {
    @Override public String getTaskDefName() { return "btr_register"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [register] Registering beneficiary: " + task.getInputData().get("beneficiaryName") + " (" + task.getInputData().get("location") + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("beneficiaryId", "BEN-758"); r.addOutputData("registered", true); return r;
    }
}
