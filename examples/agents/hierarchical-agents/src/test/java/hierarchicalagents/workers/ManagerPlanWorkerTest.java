package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManagerPlanWorkerTest {

    private final ManagerPlanWorker worker = new ManagerPlanWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_manager_plan", worker.getTaskDefName());
    }

    @Test
    void producesBackendAndFrontendPlans() {
        Task task = taskWith(Map.of("project", "TestApp", "deadline", "1 week"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("backendPlan"));
        assertNotNull(result.getOutputData().get("frontendPlan"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void backendPlanContainsScopeEndpointsPriority() {
        Task task = taskWith(Map.of("project", "MyApp", "deadline", "3 days"));
        TaskResult result = worker.execute(task);

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("backendPlan");
        assertTrue(plan.get("scope").toString().contains("MyApp"));
        assertNotNull(plan.get("endpoints"));
        assertEquals("high", plan.get("priority"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void frontendPlanContainsScopePagesPriority() {
        Task task = taskWith(Map.of("project", "MyApp", "deadline", "3 days"));
        TaskResult result = worker.execute(task);

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("frontendPlan");
        assertTrue(plan.get("scope").toString().contains("MyApp"));
        List<String> pages = (List<String>) plan.get("pages");
        assertFalse(pages.isEmpty());
        assertEquals("high", plan.get("priority"));
    }

    @Test
    void defaultsWhenProjectMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("backendPlan"));
        assertNotNull(result.getOutputData().get("frontendPlan"));
    }

    @Test
    void defaultsWhenProjectBlank() {
        Task task = taskWith(Map.of("project", "  ", "deadline", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
