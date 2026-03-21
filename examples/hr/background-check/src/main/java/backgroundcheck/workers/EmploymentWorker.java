package backgroundcheck.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EmploymentWorker implements Worker {
    @Override public String getTaskDefName() { return "bgc_employment"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [employment] Employment history verified for " + task.getInputData().get("candidateId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "verified");
        r.getOutputData().put("employersChecked", 3);
        return r;
    }
}
