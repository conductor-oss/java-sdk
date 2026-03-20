package grantmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "gmt_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Grant " + task.getInputData().get("grantId") + " report filed for " + task.getInputData().get("organization"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("grant", Map.of("grantId", task.getInputData().getOrDefault("grantId","GRT-752"), "organization", task.getInputData().getOrDefault("organization","HopeWorks"), "status", "FUNDED"));
        return r;
    }
}
