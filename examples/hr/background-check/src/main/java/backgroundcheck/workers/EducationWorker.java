package backgroundcheck.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EducationWorker implements Worker {
    @Override public String getTaskDefName() { return "bgc_education"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [education] Degree verified for " + task.getInputData().get("candidateId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "verified");
        r.getOutputData().put("degree", "BS Computer Science");
        return r;
    }
}
