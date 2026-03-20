package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EvaluateWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_evaluate"; }
    @Override public TaskResult execute(Task task) {
        String status = "at_risk";
        System.out.println("  [evaluate] 70% done -> " + status);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", status); r.getOutputData().put("pctDone", 70); return r;
    }
}
