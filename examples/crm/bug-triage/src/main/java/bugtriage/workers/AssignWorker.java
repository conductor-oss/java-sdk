package bugtriage.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AssignWorker implements Worker {
    @Override public String getTaskDefName() { return "btg_assign"; }
    @Override public TaskResult execute(Task task) {
        String severity = (String) task.getInputData().getOrDefault("severity", "low");
        Map<String, String> assignees = Map.of("critical", "senior-eng-1", "high", "eng-2", "low", "eng-3");
        String assignee = assignees.getOrDefault(severity, "eng-3");
        String bugId = (String) task.getInputData().getOrDefault("bugId", "unknown");
        System.out.println("  [assign] Assigned " + bugId + " to " + assignee);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assignee", assignee);
        result.getOutputData().put("priority", severity);
        return result;
    }
}
