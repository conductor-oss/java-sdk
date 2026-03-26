package labresults.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class ProcessSampleWorker implements Worker {
    @Override public String getTaskDefName() { return "lab_process"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Processing sample " + task.getInputData().get("sampleId") + " for " + task.getInputData().get("testType"));
        Map<String, Object> o = new LinkedHashMap<>(); o.put("processedData", Map.of("centrifuged", true, "aliquoted", true)); o.put("processedAt", Instant.now().toString());
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.setOutputData(o); return r;
    }
}
