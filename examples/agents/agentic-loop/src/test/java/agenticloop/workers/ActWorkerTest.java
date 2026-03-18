package agenticloop.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActWorkerTest {

    private final ActWorker worker = new ActWorker();

    @Test
    void taskDefName() {
        assertEquals("al_act", worker.getTaskDefName());
    }

    @Test
    void executesResearchPlan() {
        Task task = taskWith(Map.of(
                "plan", "Research and gather information on the topic",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Collected 15 relevant sources including academic papers, industry reports, and expert interviews",
                result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void executesAnalysisPlan() {
        Task task = taskWith(Map.of(
                "plan", "Analyze gathered data and identify key patterns",
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Identified 5 key patterns: consistency, partition tolerance, replication strategies, consensus protocols, and failure recovery",
                result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void executesSynthesisPlan() {
        Task task = taskWith(Map.of(
                "plan", "Synthesize findings into actionable recommendations",
                "iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals("Produced a prioritized list of 8 recommendations with implementation guidelines and trade-off analysis",
                result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsResultAndSuccess() {
        Task task = taskWith(Map.of("plan", "Research and gather information on the topic", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("success"));
    }

    @Test
    void handlesUnknownPlan() {
        Task task = taskWith(Map.of("plan", "Unknown plan text", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Completed action step successfully", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullPlan() {
        Map<String, Object> input = new HashMap<>();
        input.put("plan", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Completed action step successfully", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesBlankPlan() {
        Task task = taskWith(Map.of("plan", "  ", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Completed action step successfully", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
