package interviewscheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RemindWorker implements Worker {
    @Override public String getTaskDefName() { return "ivs_remind"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [remind] Reminder sent to " + task.getInputData().get("candidateName") + " and panel");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reminded", true);
        return r;
    }
}
