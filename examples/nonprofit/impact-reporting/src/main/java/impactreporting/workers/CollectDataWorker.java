package impactreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "ipr_collect_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collecting data for " + task.getInputData().get("programName") + ", year " + task.getInputData().get("year"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("data", Map.of("beneficiaries", 5200, "events", 48, "volunteers", 320)); return r;
    }
}
