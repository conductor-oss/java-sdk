package agentmemory.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentRespondWorkerTest {

    private final AgentRespondWorker worker = new AgentRespondWorker();

    @Test
    void taskDefName() {
        assertEquals("am_agent_respond", worker.getTaskDefName());
    }

    @Test
    void returnsResponse() {
        Task task = taskWith(Map.of("userMessage", "How do transformers work?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.length() > 10);
    }

    @Test
    void returnsConfidence() {
        Task task = taskWith(Map.of("userMessage", "Explain attention"));
        TaskResult result = worker.execute(task);

        Object confidence = result.getOutputData().get("confidence");
        assertNotNull(confidence);
        assertEquals(0.92, (double) confidence, 0.001);
    }

    @Test
    void returnsTokensUsed() {
        Task task = taskWith(Map.of("userMessage", "Explain attention"));
        TaskResult result = worker.execute(task);

        assertEquals(87, result.getOutputData().get("tokensUsed"));
    }

    @Test
    void responseContainsTransformerContent() {
        Task task = taskWith(Map.of("userMessage", "How do transformers work?"));
        TaskResult result = worker.execute(task);

        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("attention"));
    }

    @Test
    void handlesNullUserMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("userMessage", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void handlesMissingUserMessage() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
        assertNotNull(result.getOutputData().get("confidence"));
        assertNotNull(result.getOutputData().get("tokensUsed"));
    }

    @Test
    void handlesBlankUserMessage() {
        Task task = taskWith(Map.of("userMessage", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
