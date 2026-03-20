package visaprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "vsp_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Application submitted to " + task.getInputData().get("country") + " embassy");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("submitted", true); r.getOutputData().put("referenceNumber", "EMB-2024-visa-processing"); return r;
    }
}
