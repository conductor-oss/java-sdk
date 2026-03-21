package labresults.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class CollectSampleWorker implements Worker {
    @Override public String getTaskDefName() { return "lab_collect_sample"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Sample collected for " + task.getInputData().get("testType") + " - patient " + task.getInputData().get("patientId"));
        Map<String, Object> o = new LinkedHashMap<>(); o.put("sampleId", "SMP-44201"); o.put("collectedAt", Instant.now().toString()); o.put("specimenType", "blood");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.setOutputData(o); return r;
    }
}
