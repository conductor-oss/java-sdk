package watermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AnalyzeQualityWorker implements Worker {
    @Override public String getTaskDefName() { return "wtr_analyze_quality"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [quality] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
