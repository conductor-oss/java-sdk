package rolemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AssignRoleWorker implements Worker {
    @Override public String getTaskDefName() { return "rom_assign"; }
    @Override public TaskResult execute(Task task) {
        String role = (String) task.getInputData().get("role");
        Map<String, List<String>> permsMap = Map.of(
                "admin", List.of("read", "write", "delete", "manage_users"),
                "editor", List.of("read", "write"),
                "viewer", List.of("read"));
        List<String> rolePerms = permsMap.getOrDefault(role, List.of("read"));
        System.out.println("  [assign] Role \"" + role + "\" assigned with " + rolePerms.size() + " permissions");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assigned", true);
        result.getOutputData().put("permissions", rolePerms);
        return result;
    }
}
