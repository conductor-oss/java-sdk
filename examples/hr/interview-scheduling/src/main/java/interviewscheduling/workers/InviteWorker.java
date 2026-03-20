package interviewscheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class InviteWorker implements Worker {
    @Override public String getTaskDefName() { return "ivs_invite"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [invite] Calendar invite sent to " + task.getInputData().get("candidateName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("invited", true);
        r.getOutputData().put("calendarEvent", true);
        return r;
    }
}
