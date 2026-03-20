package volunteercoordination.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "vol_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking hours for volunteer " + task.getInputData().get("volunteerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("hoursLogged", 4); r.addOutputData("totalHours", 28); r.addOutputData("eventsAttended", 7); return r;
    }
}
