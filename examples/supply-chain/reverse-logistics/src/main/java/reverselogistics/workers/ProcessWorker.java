package reverselogistics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessWorker implements Worker {
    @Override public String getTaskDefName() { return "rvl_process"; }
    @Override public TaskResult execute(Task task) {
        String condition = (String) task.getInputData().get("condition");
        System.out.println("  [process] Return " + task.getInputData().get("returnId") + " processed — action: " + condition);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", condition); r.getOutputData().put("completed", true); return r;
    }
}
