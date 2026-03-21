package visaprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "vsp_receive"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receive] Visa approved and passport returned");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "approved"); r.getOutputData().put("validFrom", "2024-04-15"); r.getOutputData().put("validTo", "2024-10-15"); return r;
    }
}
