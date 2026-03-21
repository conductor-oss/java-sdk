package impactreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class FormatWorker implements Worker {
    @Override public String getTaskDefName() { return "ipr_format"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [format] Formatting report for " + task.getInputData().get("programName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("report", Map.of("title", task.getInputData().getOrDefault("programName","Program") + " Impact Report", "sections", 5, "charts", 8)); return r;
    }
}
