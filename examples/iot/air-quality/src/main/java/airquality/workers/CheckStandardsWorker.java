package airquality.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CheckStandardsWorker implements Worker {
    @Override public String getTaskDefName() { return "aq_check_standards"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [standards] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
