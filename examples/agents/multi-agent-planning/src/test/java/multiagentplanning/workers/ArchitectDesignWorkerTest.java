package multiagentplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectDesignWorkerTest {

    private final ArchitectDesignWorker worker = new ArchitectDesignWorker();

    @Test
    void taskDefName() {
        assertEquals("pp_architect_design", worker.getTaskDefName());
    }

    @Test
    void returnsArchitectureDesign() {
        Task task = taskWith(Map.of("projectName", "E-Commerce Platform",
                "requirements", "User auth, catalog, payments"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> feComponents = (List<String>) result.getOutputData().get("frontendComponents");
        assertNotNull(feComponents);
        assertEquals(3, feComponents.size());

        @SuppressWarnings("unchecked")
        List<String> beComponents = (List<String>) result.getOutputData().get("backendComponents");
        assertNotNull(beComponents);
        assertEquals(4, beComponents.size());

        @SuppressWarnings("unchecked")
        List<String> infra = (List<String>) result.getOutputData().get("infrastructure");
        assertNotNull(infra);
        assertEquals(4, infra.size());
    }

    @Test
    void returnsComplexityLevels() {
        Task task = taskWith(Map.of("projectName", "Test", "requirements", "test"));
        TaskResult result = worker.execute(task);

        assertEquals("medium", result.getOutputData().get("frontendComplexity"));
        assertEquals("high", result.getOutputData().get("backendComplexity"));
        assertEquals("medium", result.getOutputData().get("infraComplexity"));
    }

    @Test
    void returnsArchitectureSummary() {
        Task task = taskWith(Map.of("projectName", "MyApp", "requirements", "real-time chat"));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("architectureSummary");
        assertNotNull(summary);
        assertTrue(summary.contains("MyApp"));
        assertTrue(summary.contains("real-time chat"));
    }

    @Test
    void frontendComponentsContainInteractive() {
        Task task = taskWith(Map.of("projectName", "Test", "requirements", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> feComponents = (List<String>) result.getOutputData().get("frontendComponents");
        boolean hasInteractive = feComponents.stream()
                .anyMatch(c -> c.toLowerCase().contains("interactive"));
        assertTrue(hasInteractive);
    }

    @Test
    void backendComponentsContainCore() {
        Task task = taskWith(Map.of("projectName", "Test", "requirements", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> beComponents = (List<String>) result.getOutputData().get("backendComponents");
        boolean hasCore = beComponents.stream()
                .anyMatch(c -> c.toLowerCase().contains("core"));
        assertTrue(hasCore);
    }

    @Test
    void infrastructureContainsKubernetes() {
        Task task = taskWith(Map.of("projectName", "Test", "requirements", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> infra = (List<String>) result.getOutputData().get("infrastructure");
        boolean hasK8s = infra.stream()
                .anyMatch(c -> c.toLowerCase().contains("kubernetes"));
        assertTrue(hasK8s);
    }

    @Test
    void handlesNullProjectName() {
        Map<String, Object> input = new HashMap<>();
        input.put("projectName", null);
        input.put("requirements", "test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("architectureSummary");
        assertTrue(summary.contains("Unnamed Project"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("frontendComponents"));
    }

    @Test
    void handlesBlankProjectName() {
        Task task = taskWith(Map.of("projectName", "   ", "requirements", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("architectureSummary");
        assertTrue(summary.contains("Unnamed Project"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
