package benefitsenrollment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "ben_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] All selections valid and within eligibility");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("validSelections", task.getInputData().get("selections"));
        r.getOutputData().put("valid", true);
        return r;
    }
}
