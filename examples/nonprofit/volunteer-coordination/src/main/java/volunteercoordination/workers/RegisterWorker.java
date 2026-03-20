package volunteercoordination.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RegisterWorker implements Worker {
    @Override public String getTaskDefName() { return "vol_register"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [register] Registering volunteer: " + task.getInputData().get("volunteerName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("volunteerId", "VOL-753"); r.addOutputData("registered", true); return r;
    }
}
