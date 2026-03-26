package agentswarm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Swarm4WorkerTest {

    private final Swarm4Worker worker = new Swarm4Worker();

    @Test
    void taskDefName() {
        assertEquals("as_swarm_4", worker.getTaskDefName());
    }

    @Test
    void returnsFutureTrendsFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-4",
                "subtask", Map.of("area", "Future Trends")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Future Trends", result.getOutputData().get("area"));
        assertEquals("swarm-agent-4", result.getOutputData().get("agentId"));
    }

    @Test
    void returnsThreeFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-4"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> findings = (List<String>) result.getOutputData().get("findings");
        assertNotNull(findings);
        assertEquals(3, findings.size());
        for (String finding : findings) {
            assertTrue(finding.length() > 10, "Each finding should be substantive");
        }
    }

    @Test
    void returnsCorrectConfidenceAndSources() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-4"));
        TaskResult result = worker.execute(task);

        assertEquals(0.82, result.getOutputData().get("confidence"));
        assertEquals(9, result.getOutputData().get("sourcesConsulted"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-4"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("agentId"));
        assertTrue(result.getOutputData().containsKey("area"));
        assertTrue(result.getOutputData().containsKey("findings"));
        assertTrue(result.getOutputData().containsKey("confidence"));
        assertTrue(result.getOutputData().containsKey("sourcesConsulted"));
    }

    @Test
    void handlesNullAgentId() {
        Map<String, Object> input = new HashMap<>();
        input.put("agentId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-4", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesMissingAgentId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-4", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesBlankAgentId() {
        Task task = taskWith(Map.of("agentId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-4", result.getOutputData().get("agentId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
