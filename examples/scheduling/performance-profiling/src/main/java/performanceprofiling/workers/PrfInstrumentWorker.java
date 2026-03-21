package performanceprofiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class PrfInstrumentWorker implements Worker {
    @Override public String getTaskDefName() { return "prf_instrument"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [instrument] Instrumenting " + task.getInputData().get("serviceName") + " for " + task.getInputData().get("profileType") + " profiling");
        r.getOutputData().put("instrumentId", "prf-inst-20260308");
        r.getOutputData().put("attached", true);
        r.getOutputData().put("overhead", "2.1%");
        return r;
    }
}
