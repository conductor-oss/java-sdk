package watermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DetectLeaksWorker implements Worker {
    @Override public String getTaskDefName() { return "wtr_detect_leaks"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [leaks] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confidence", "leakDetected ? 0.85 : 0.02");
        return r;
    }
}
