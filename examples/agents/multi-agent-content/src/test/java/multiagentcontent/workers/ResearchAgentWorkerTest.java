package multiagentcontent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResearchAgentWorkerTest {

    private final ResearchAgentWorker worker = new ResearchAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("cc_research_agent", worker.getTaskDefName());
    }

    @Test
    void returnsFourFacts() {
        Task task = taskWith(Map.of("topic", "AI in Healthcare", "targetAudience", "doctors"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) result.getOutputData().get("facts");
        assertNotNull(facts);
        assertEquals(4, facts.size());
        assertTrue(facts.get(0).contains("AI in Healthcare"));
    }

    @Test
    void returnsThreeSources() {
        Task task = taskWith(Map.of("topic", "Cloud Computing", "targetAudience", "engineers"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) result.getOutputData().get("sources");
        assertNotNull(sources);
        assertEquals(3, sources.size());
        assertTrue(sources.get(2).contains("engineers"));
    }

    @Test
    void returnsTopicRelevance() {
        Task task = taskWith(Map.of("topic", "Machine Learning"));
        TaskResult result = worker.execute(task);

        assertEquals(0.92, result.getOutputData().get("topicRelevance"));
    }

    @Test
    void factsIncludeTopicAndAudience() {
        Task task = taskWith(Map.of("topic", "Blockchain", "targetAudience", "finance teams"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) result.getOutputData().get("facts");
        assertTrue(facts.get(1).contains("finance teams"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("facts"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("facts"));
    }

    @Test
    void handlesBlankAudience() {
        Task task = taskWith(Map.of("topic", "Testing", "targetAudience", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) result.getOutputData().get("sources");
        assertNotNull(sources);
        assertEquals(3, sources.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
