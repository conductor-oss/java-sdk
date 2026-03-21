package performanceprofiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectProfileWorker implements Worker {
    @Override public String getTaskDefName() { return "prf_collect_profile"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting " + task.getInputData().get("profilingDuration") + " profile");
        r.getOutputData().put("sampleCount", 45000);
        r.getOutputData().put("profileSizeKb", 2400);
        return r;
    }
}
