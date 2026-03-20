package eventfundraising.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "efr_execute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Event held - " + task.getInputData().get("attendees") + " registered");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("attendees", 220); r.addOutputData("expenses", 12000); r.addOutputData("satisfaction", 4.8); return r;
    }
}
