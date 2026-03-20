package userfeedback.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class RouteFeedbackWorker implements Worker {
    @Override public String getTaskDefName() { return "ufb_route"; }
    @Override public TaskResult execute(Task task) {
        String category = (String) task.getInputData().get("category");
        Map<String, String> teams = Map.of("bug", "engineering", "feature_request", "product", "general", "support");
        String team = teams.getOrDefault(category, "support");
        System.out.println("  [route] Routed to " + team + " team (priority: " + task.getInputData().get("priority") + ")");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("assignedTeam", team);
        r.getOutputData().put("ticketCreated", true);
        return r;
    }
}
