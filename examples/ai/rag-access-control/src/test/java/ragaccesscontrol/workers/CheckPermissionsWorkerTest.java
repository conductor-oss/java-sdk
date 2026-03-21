package ragaccesscontrol.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckPermissionsWorkerTest {

    private final CheckPermissionsWorker worker = new CheckPermissionsWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_check_permissions", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void teamLeadGetsHrPolicies() {
        Task task = taskWith(Map.of(
                "userId", "user-42",
                "roles", List.of("engineer", "team-lead"),
                "clearanceLevel", "confidential"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<String> allowed = (List<String>) result.getOutputData().get("allowedCollections");
        assertNotNull(allowed);
        assertTrue(allowed.contains("public-docs"));
        assertTrue(allowed.contains("engineering-wiki"));
        assertTrue(allowed.contains("hr-policies"));

        List<String> denied = (List<String>) result.getOutputData().get("deniedCollections");
        assertNotNull(denied);
        assertTrue(denied.contains("finance-reports"));
        assertTrue(denied.contains("executive-memos"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void nonTeamLeadDeniedHrPolicies() {
        Task task = taskWith(Map.of(
                "userId", "user-99",
                "roles", List.of("engineer"),
                "clearanceLevel", "internal"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<String> allowed = (List<String>) result.getOutputData().get("allowedCollections");
        assertFalse(allowed.contains("hr-policies"));

        List<String> denied = (List<String>) result.getOutputData().get("deniedCollections");
        assertTrue(denied.contains("hr-policies"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
