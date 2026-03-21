package changerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AssessImpactWorker implements Worker {
    @Override public String getTaskDefName() { return "chr_assess_impact"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [impact] Assessing impact of " + task.getInputData().get("changeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("impact", Map.of("schedule","+2 weeks","cost","$5000","risk","medium")); return r;
    }
}
