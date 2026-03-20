package labresults.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class AnalyzeSampleWorker implements Worker {
    @Override public String getTaskDefName() { return "lab_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Analyzing sample " + task.getInputData().get("sampleId"));
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("results", Map.of("glucose", Map.of("value", 105, "unit", "mg/dL", "range", "70-100", "flag", "H"), "hba1c", Map.of("value", 5.8, "unit", "%", "range", "4.0-5.6", "flag", "H"), "cholesterol", Map.of("value", 195, "unit", "mg/dL", "range", "< 200", "flag", "N")));
        o.put("critical", false);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.setOutputData(o); return r;
    }
}
