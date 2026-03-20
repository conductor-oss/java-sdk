package virtualeconomy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "vec_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Validating transaction in " + task.getInputData().get("currency"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("valid", true); r.addOutputData("rateCheck", "pass"); r.addOutputData("limitCheck", "pass");
        return r;
    }
}
