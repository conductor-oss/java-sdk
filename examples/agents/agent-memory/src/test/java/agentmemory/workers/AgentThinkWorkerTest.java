package agentmemory.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentThinkWorkerTest {

    private final AgentThinkWorker worker = new AgentThinkWorker();

    @Test
    void taskDefName() {
        assertEquals("am_agent_think", worker.getTaskDefName());
    }

    @Test
    void returnsThoughts() {
        Task task = taskWith(Map.of("userMessage", "How do transformers work?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String thoughts = (String) result.getOutputData().get("thoughts");
        assertNotNull(thoughts);
        assertTrue(thoughts.length() > 10);
    }

    @Test
    void returnsRelevantFacts() {
        Task task = taskWith(Map.of("userMessage", "How do transformers work?"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) result.getOutputData().get("relevantFacts");
        assertNotNull(facts);
        assertEquals(3, facts.size());
        for (String fact : facts) {
            assertFalse(fact.isBlank());
        }
    }

    @Test
    void returnsFactsRecalledCount() {
        Task task = taskWith(Map.of("userMessage", "How do transformers work?"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("factsRecalledCount"));
    }

    @Test
    void factsRecalledCountMatchesFactsSize() {
        Task task = taskWith(Map.of("userMessage", "Explain attention"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) result.getOutputData().get("relevantFacts");
        int count = (int) result.getOutputData().get("factsRecalledCount");
        assertEquals(facts.size(), count);
    }

    @Test
    void handlesNullUserMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("userMessage", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("thoughts"));
    }

    @Test
    void handlesMissingUserMessage() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("relevantFacts"));
    }

    @Test
    void handlesBlankUserMessage() {
        Task task = taskWith(Map.of("userMessage", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("thoughts"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
