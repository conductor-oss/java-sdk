package stakeholderreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class FormatWorker implements Worker {
    @Override public String getTaskDefName() { return "shr_format"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [format] Formatting report");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("report", Map.of("title","Weekly Status Report","health","YELLOW","sections",3)); return r;
    }
}
