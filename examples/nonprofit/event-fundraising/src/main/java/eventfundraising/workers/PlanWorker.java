package eventfundraising.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PlanWorker implements Worker {
    @Override public String getTaskDefName() { return "efr_plan"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [plan] Planning event: " + task.getInputData().get("eventName") + " on " + task.getInputData().get("eventDate"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("eventId", "EVT-759"); r.addOutputData("venue", "Grand Ballroom"); r.addOutputData("capacity", 300); return r;
    }
}
