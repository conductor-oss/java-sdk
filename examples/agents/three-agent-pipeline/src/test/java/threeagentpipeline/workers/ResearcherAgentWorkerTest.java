package threeagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResearcherAgentWorkerTest {

    private final ResearcherAgentWorker worker = new ResearcherAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("thr_researcher_agent", worker.getTaskDefName());
    }

    @Test
    void researchesGivenSubject() {
        Task task = taskWith(Map.of(
                "subject", "cloud computing",
                "systemPrompt", "Research the topic"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        assertNotNull(research);
        assertEquals("cloud computing", research.get("subject"));
    }

    @Test
    void researchContainsKeyFacts() {
        Task task = taskWith(Map.of("subject", "blockchain"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        @SuppressWarnings("unchecked")
        List<String> keyFacts = (List<String>) research.get("keyFacts");

        assertNotNull(keyFacts);
        assertEquals(3, keyFacts.size());
        assertTrue(keyFacts.get(0).contains("blockchain"));
    }

    @Test
    void researchContainsStatistics() {
        Task task = taskWith(Map.of("subject", "AI"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) research.get("statistics");

        assertNotNull(statistics);
        assertNotNull(statistics.get("marketGrowth"));
        assertNotNull(statistics.get("regions"));
        assertNotNull(statistics.get("enterprises"));
    }

    @Test
    void researchContainsSources() {
        Task task = taskWith(Map.of("subject", "IoT"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) research.get("sources");

        assertNotNull(sources);
        assertEquals(3, sources.size());
    }

    @Test
    void outputContainsModel() {
        Task task = taskWith(Map.of("subject", "cybersecurity"));
        TaskResult result = worker.execute(task);

        assertEquals("researcher-agent-v1", result.getOutputData().get("model"));
    }

    @Test
    void defaultsSubjectWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        assertEquals("general technology trends", research.get("subject"));
    }

    @Test
    void defaultsSubjectWhenBlank() {
        Task task = taskWith(Map.of("subject", "   "));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> research = (Map<String, Object>) result.getOutputData().get("research");
        assertEquals("general technology trends", research.get("subject"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
