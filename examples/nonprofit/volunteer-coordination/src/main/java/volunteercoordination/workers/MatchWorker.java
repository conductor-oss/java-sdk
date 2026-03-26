package volunteercoordination.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class MatchWorker implements Worker {
    @Override public String getTaskDefName() { return "vol_match"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [match] Matching volunteer " + task.getInputData().get("volunteerId") + " with opportunities");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("opportunity", Map.of("id", "OPP-101", "name", "Food Bank Sorting", "location", "Downtown Center")); return r;
    }
}
