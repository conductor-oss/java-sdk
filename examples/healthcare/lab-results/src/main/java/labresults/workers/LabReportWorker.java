package labresults.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class LabReportWorker implements Worker {
    @Override public String getTaskDefName() { return "lab_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Generated report - abnormal results detected");
        Map<String, Object> o = new LinkedHashMap<>(); o.put("reportId", "RPT-LAB-88201"); o.put("flaggedCount", 2); o.put("generatedAt", Instant.now().toString());
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.setOutputData(o); return r;
    }
}
