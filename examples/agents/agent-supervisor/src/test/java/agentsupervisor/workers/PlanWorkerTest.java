package agentsupervisor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanWorkerTest {

    private final PlanWorker worker = new PlanWorker();

    @Test
    void taskDefName() {
        assertEquals("sup_plan", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsPlanWithFeatureAndPriority() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "user-authentication",
                "priority", "high",
                "systemPrompt", "You are a supervisor."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertNotNull(plan);
        assertEquals("user-authentication", plan.get("feature"));
        assertEquals("high", plan.get("priority"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void planContainsFivePhases() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "user-authentication",
                "priority", "high"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        List<String> phases = (List<String>) plan.get("phases");
        assertNotNull(phases);
        assertEquals(5, phases.size());
        assertTrue(phases.contains("design"));
        assertTrue(phases.contains("implementation"));
        assertTrue(phases.contains("testing"));
        assertTrue(phases.contains("documentation"));
        assertTrue(phases.contains("review"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void planContainsDeadline() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "search",
                "priority", "medium"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertNotNull(plan.get("deadline"));
    }

    @Test
    void returnsTaskAssignments() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "user-authentication",
                "priority", "high"
        )));
        TaskResult result = worker.execute(task);

        String codingTask = (String) result.getOutputData().get("codingTask");
        String testingTask = (String) result.getOutputData().get("testingTask");
        String documentationTask = (String) result.getOutputData().get("documentationTask");

        assertNotNull(codingTask);
        assertNotNull(testingTask);
        assertNotNull(documentationTask);
        assertTrue(codingTask.contains("user-authentication"));
        assertTrue(testingTask.contains("user-authentication"));
        assertTrue(documentationTask.contains("user-authentication"));
    }

    @Test
    void codingTaskDescribesImplementation() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "payment-processing",
                "priority", "critical"
        )));
        TaskResult result = worker.execute(task);

        String codingTask = (String) result.getOutputData().get("codingTask");
        assertTrue(codingTask.contains("Implement"));
        assertTrue(codingTask.contains("payment-processing"));
    }

    @Test
    void testingTaskDescribesTestSuites() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "payment-processing",
                "priority", "critical"
        )));
        TaskResult result = worker.execute(task);

        String testingTask = (String) result.getOutputData().get("testingTask");
        assertTrue(testingTask.contains("test"));
        assertTrue(testingTask.contains("payment-processing"));
    }

    @Test
    void documentationTaskDescribesDocs() {
        Task task = taskWith(new HashMap<>(Map.of(
                "feature", "payment-processing",
                "priority", "critical"
        )));
        TaskResult result = worker.execute(task);

        String docTask = (String) result.getOutputData().get("documentationTask");
        assertTrue(docTask.contains("documentation") || docTask.contains("Write"));
        assertTrue(docTask.contains("payment-processing"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultsWhenInputMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("user-authentication", plan.get("feature"));
        assertEquals("high", plan.get("priority"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultsWhenNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("feature", null);
        input.put("priority", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> plan = (Map<String, Object>) result.getOutputData().get("plan");
        assertEquals("user-authentication", plan.get("feature"));
        assertEquals("high", plan.get("priority"));
    }

    @Test
    void sameInputProducesSameOutput() {
        Task task1 = taskWith(new HashMap<>(Map.of("feature", "search", "priority", "low")));
        Task task2 = taskWith(new HashMap<>(Map.of("feature", "search", "priority", "low")));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("plan"), r2.getOutputData().get("plan"));
        assertEquals(r1.getOutputData().get("codingTask"), r2.getOutputData().get("codingTask"));
        assertEquals(r1.getOutputData().get("testingTask"), r2.getOutputData().get("testingTask"));
        assertEquals(r1.getOutputData().get("documentationTask"), r2.getOutputData().get("documentationTask"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
