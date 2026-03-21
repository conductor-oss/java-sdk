package agentswarm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Swarm2WorkerTest {

    private final Swarm2Worker worker = new Swarm2Worker();

    @Test
    void taskDefName() {
        assertEquals("as_swarm_2", worker.getTaskDefName());
    }

    @Test
    void returnsTechnicalLandscapeFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-2",
                "subtask", Map.of("area", "Technical Landscape")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Technical Landscape", result.getOutputData().get("area"));
        assertEquals("swarm-agent-2", result.getOutputData().get("agentId"));
    }

    @Test
    void returnsThreeFindings() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-2"));
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
        Task task = taskWith(Map.of("agentId", "swarm-agent-2"));
        TaskResult result = worker.execute(task);

        assertEquals(0.91, result.getOutputData().get("confidence"));
        assertEquals(11, result.getOutputData().get("sourcesConsulted"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("agentId", "swarm-agent-2"));
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
        assertEquals("swarm-agent-2", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesMissingAgentId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-2", result.getOutputData().get("agentId"));
    }

    @Test
    void handlesBlankAgentId() {
        Task task = taskWith(Map.of("agentId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("swarm-agent-2", result.getOutputData().get("agentId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
