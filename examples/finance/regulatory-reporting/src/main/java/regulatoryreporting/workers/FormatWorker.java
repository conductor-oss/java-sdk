package regulatoryreporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FormatWorker implements Worker {
    @Override public String getTaskDefName() { return "reg_format"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String type = (String) task.getInputData().get("reportType");
        String format = "CALL".equals(type) ? "XBRL" : "XML";
        System.out.println("  [format] Formatting report as " + format + " for " + type);
        result.getOutputData().put("formattedReport", Map.of("format", format, "size", "2.4MB", "pages", 128));
        result.getOutputData().put("formatVersion", "v2.1");
        return result;
    }
}
