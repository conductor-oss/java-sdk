package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReceiveReturnWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_receive_return"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receive] Return " + task.getInputData().get("returnId") + ": " + task.getInputData().get("product") + " — reason: " + task.getInputData().get("reason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("received", true); return r;
    }
}
