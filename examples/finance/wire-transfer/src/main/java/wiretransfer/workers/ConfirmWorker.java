package wiretransfer.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "wir_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Wire " + task.getInputData().get("transferId") + " confirmed — ref: " + task.getInputData().get("transactionRef"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true); r.getOutputData().put("confirmationNumber", "CONF-WIRE-44201");
        r.getOutputData().put("notifiedParties", List.of("sender", "recipient"));
        return r;
    }
}
