package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LeadFrontendWorkerTest {

    private final LeadFrontendWorker worker = new LeadFrontendWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_lead_frontend", worker.getTaskDefName());
    }

    @Test
    void producesSummaryAndTasks() {
        Map<String, Object> workstream = Map.of(
                "scope", "Build UI",
                "pages", List.of("Home", "Settings"),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
        assertNotNull(result.getOutputData().get("uiTask"));
        assertNotNull(result.getOutputData().get("stylingTask"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void uiTaskContainsPagesFrameworkState() {
        Map<String, Object> workstream = Map.of(
                "scope", "Dashboard UI",
                "pages", List.of("Dashboard", "Profile"),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        Map<String, Object> uiTask = (Map<String, Object>) result.getOutputData().get("uiTask");
        List<String> pages = (List<String>) uiTask.get("pages");
        assertEquals(List.of("Dashboard", "Profile"), pages);
        assertEquals("React", uiTask.get("framework"));
        assertEquals("Redux", uiTask.get("stateManagement"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void stylingTaskContainsDesignSystemResponsiveDarkMode() {
        Map<String, Object> workstream = Map.of(
                "scope", "Styling",
                "pages", List.of(),
                "priority", "medium"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        Map<String, Object> stylingTask = (Map<String, Object>) result.getOutputData().get("stylingTask");
        assertEquals("Material UI", stylingTask.get("designSystem"));
        assertEquals(true, stylingTask.get("responsive"));
        assertEquals(true, stylingTask.get("darkMode"));
    }

    @Test
    void summaryContainsScope() {
        Map<String, Object> workstream = Map.of(
                "scope", "Build analytics dashboard",
                "pages", List.of(),
                "priority", "high"
        );
        Task task = taskWith(Map.of("workstream", workstream));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("Build analytics dashboard"));
    }

    @Test
    void handlesNullWorkstream() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("uiTask"));
        assertNotNull(result.getOutputData().get("stylingTask"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
