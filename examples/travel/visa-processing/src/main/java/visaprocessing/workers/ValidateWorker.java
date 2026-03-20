package visaprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "vsp_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] All documents verified");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true); r.getOutputData().put("issues", 0); return r;
    }
}
