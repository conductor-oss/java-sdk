package agentswarm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Swarm1WorkerTest {

    private final Swarm1Worker worker = new Swarm1Worker();

    @Test
    void taskDefName() {
        assertEquals("as_swarm_1", worker.getTaskDefName());
    }

    @Test
    void returnsMarketAnalysisFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-1",
                "subtask", Map.of("area", "Market Analysis")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Market Analysis", result.getOutputData().get("area"));
        assertEquals("swarm-agent-1", result.getOutputData().get("agentId"));
    }

    @Test
    void returnsThreeFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-1"));
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
        Task task = taskWith(Map.of("agentId", "swarm-agent-1"));
        TaskResult result = worker.execute(task);

        assertEquals(0.88, result.getOutputData().get("confidence"));
        assertEquals(14, result.getOutputData().get("sourcesConsulted"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-1"));
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
        assertEquals("swarm-agent-1", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesMissingAgentId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-1", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesBlankAgentId() {
        Task task = taskWith(Map.of("agentId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-1", result.getOutputData().get("agentId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
