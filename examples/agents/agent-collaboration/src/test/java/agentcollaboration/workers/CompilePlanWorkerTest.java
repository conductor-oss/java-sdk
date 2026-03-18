package agentcollaboration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompilePlanWorkerTest {

    private final CompilePlanWorker worker = new CompilePlanWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_compile_plan", worker.getTaskDefName());
    }

    @Test
    void compilesPlanFromFullInput() {
        Task task = taskWith(fullInput());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertNotNull(plan);
        assertEquals("Stabilize & Retain — Execution Plan", plan.get("title"));
        assertEquals(4, plan.get("insightsUsed"));
        assertEquals(3, plan.get("strategyPillars"));
        assertEquals(6, plan.get("actionItems"));
        assertEquals("8 weeks", plan.get("timeline"));
        assertEquals("ready_for_review", plan.get("status"));
    }

    @Test
    void planHasAllExpectedFields() {
        Task task = taskWith(fullInput());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals(6, plan.size());
        assertTrue(plan.containsKey("title"));
        assertTrue(plan.containsKey("insightsUsed"));
        assertTrue(plan.containsKey("strategyPillars"));
        assertTrue(plan.containsKey("actionItems"));
        assertTrue(plan.containsKey("timeline"));
        assertTrue(plan.containsKey("status"));
    }

    @Test
    void handlesNullInsights() {
        Map<String, Object> input = new HashMap<>();
        input.put("insights", null);
        input.put("strategy", Map.of("name", "Test", "pillars", List.of("A", "B")));
        input.put("actionItems", List.of(Map.of("id", "ACT-001")));
        input.put("timeline", Map.of("totalWeeks", 4));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals(0, plan.get("insightsUsed"));
    }

    @Test
    void handlesNullStrategy() {
        Map<String, Object> input = new HashMap<>();
        input.put("insights", List.of(Map.of("id", "INS-001")));
        input.put("strategy", null);
        input.put("actionItems", List.of(Map.of("id", "ACT-001")));
        input.put("timeline", Map.of("totalWeeks", 4));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals(0, plan.get("strategyPillars"));
    }

    @Test
    void handlesNullActionItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("insights", List.of(Map.of("id", "INS-001")));
        input.put("strategy", Map.of("name", "Test", "pillars", List.of("A")));
        input.put("actionItems", null);
        input.put("timeline", Map.of("totalWeeks", 4));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals(0, plan.get("actionItems"));
    }

    @Test
    void handlesNullTimeline() {
        Map<String, Object> input = new HashMap<>();
        input.put("insights", List.of(Map.of("id", "INS-001")));
        input.put("strategy", Map.of("name", "Test", "pillars", List.of("A")));
        input.put("actionItems", List.of(Map.of("id", "ACT-001")));
        input.put("timeline", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("0 weeks", plan.get("timeline"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("plan"));
    }

    @Test
    void strategyWithoutPillarsYieldsZeroPillars() {
        Map<String, Object> input = new HashMap<>();
        input.put("strategy", Map.of("name", "NoPillars"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> plan =
                (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals(0, plan.get("strategyPillars"));
    }

    private Map<String, Object> fullInput() {
        List<Map<String, Object>> insights = List.of(
                Map.of("id", "INS-001", "finding", "f1", "severity", "high", "category", "retention"),
                Map.of("id", "INS-002", "finding", "f2", "severity", "critical", "category", "revenue"),
                Map.of("id", "INS-003", "finding", "f3", "severity", "medium", "category", "operations"),
                Map.of("id", "INS-004", "finding", "f4", "severity", "high", "category", "engagement")
        );

        Map<String, Object> strategy = new HashMap<>();
        strategy.put("name", "Stabilize & Retain");
        strategy.put("thesis", "Address churn");
        strategy.put("pillars", List.of("CX Overhaul", "Support Acceleration", "Loyalty Revitalization"));

        List<Map<String, Object>> actionItems = List.of(
                Map.of("id", "ACT-001"), Map.of("id", "ACT-002"),
                Map.of("id", "ACT-003"), Map.of("id", "ACT-004"),
                Map.of("id", "ACT-005"), Map.of("id", "ACT-006")
        );

        Map<String, Object> timeline = Map.of("totalWeeks", 8);

        Map<String, Object> input = new HashMap<>();
        input.put("insights", insights);
        input.put("strategy", strategy);
        input.put("actionItems", actionItems);
        input.put("timeline", timeline);
        return input;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
