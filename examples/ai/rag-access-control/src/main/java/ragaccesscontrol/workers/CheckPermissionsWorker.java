package ragaccesscontrol.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker that checks user permissions based on roles and clearance level.
 * Determines which document collections the user is allowed to access.
 */
public class CheckPermissionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_check_permissions";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        List<String> roles = (List<String>) task.getInputData().get("roles");
        String clearanceLevel = (String) task.getInputData().get("clearanceLevel");

        List<String> allCollections = List.of(
                "public-docs", "engineering-wiki", "hr-policies",
                "finance-reports", "executive-memos");

        List<String> allowedCollections = new ArrayList<>();
        List<String> deniedCollections = new ArrayList<>();

        allowedCollections.add("public-docs");
        allowedCollections.add("engineering-wiki");

        if (roles.contains("team-lead")) {
            allowedCollections.add("hr-policies");
        }

        for (String collection : allCollections) {
            if (!allowedCollections.contains(collection)) {
                deniedCollections.add(collection);
            }
        }

        System.out.println("  [permissions] User " + userId + " allowed: " + allowedCollections
                + ", denied: " + deniedCollections);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allowedCollections", allowedCollections);
        result.getOutputData().put("deniedCollections", deniedCollections);
        return result;
    }
}
