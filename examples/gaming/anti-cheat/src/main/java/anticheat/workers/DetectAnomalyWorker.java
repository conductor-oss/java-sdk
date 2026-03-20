package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class DetectAnomalyWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_detect_anomaly"; }
    @Override public TaskResult execute(Task task) {
        double aimAccuracy = 0.45;
        Object metrics = task.getInputData().get("metrics");
        if (metrics instanceof Map) {
            Object aa = ((Map<?,?>) metrics).get("aimAccuracy");
            if (aa instanceof Number) aimAccuracy = ((Number) aa).doubleValue();
        }
        String verdict = aimAccuracy > 0.9 ? "cheat" : aimAccuracy > 0.7 ? "suspect" : "clean";
        System.out.println("  [detect] Analysis complete - verdict: " + verdict);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("verdict", verdict); r.addOutputData("confidence", 0.92);
        r.addOutputData("flags", List.of()); r.addOutputData("evidence", null);
        return r;
    }
}
