package medicalimaging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() { return "img_report"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Radiology report generated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("reportId", "RAD-RPT-5501");
        output.put("status", "preliminary");
        output.put("generatedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
