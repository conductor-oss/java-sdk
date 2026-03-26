package compliancevendor.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssessWorker implements Worker {
    @Override public String getTaskDefName() { return "vcm_assess"; }
    @Override public TaskResult execute(Task task) {
        int score = 91;
        System.out.println("  [assess] " + task.getInputData().get("vendorId") + " scored " + score + "/100 on " + task.getInputData().get("complianceStandard"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("assessmentResult", "satisfactory"); r.getOutputData().put("score", score); return r;
    }
}
