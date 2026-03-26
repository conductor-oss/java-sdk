package bulkuserimport.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ParseFileWorker implements Worker {
    @Override public String getTaskDefName() { return "bui_parse_file"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [parse] Parsed " + task.getInputData().get("format") + " file: " + task.getInputData().get("fileUrl"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("records", List.of(Map.of("email", "a@ex.com"), Map.of("email", "b@ex.com")));
        r.getOutputData().put("totalParsed", 1250);
        return r;
    }
}
