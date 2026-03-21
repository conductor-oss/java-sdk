package threeagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriterAgentWorkerTest {

    private final WriterAgentWorker worker = new WriterAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("thr_writer_agent", worker.getTaskDefName());
    }

    @Test
    void producesDraftFromResearch() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "tech executives",
                "systemPrompt", "Write an article"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String draft = (String) result.getOutputData().get("draft");
        assertNotNull(draft);
        assertTrue(draft.contains("cloud computing"));
        assertTrue(draft.contains("tech executives"));
    }

    @Test
    void draftIncorporatesStatistics() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "developers"
        ));
        TaskResult result = worker.execute(task);

        String draft = (String) result.getOutputData().get("draft");
        assertTrue(draft.contains("15.3% CAGR"));
    }

    @Test
    void draftIncorporatesKeyFacts() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "analysts"
        ));
        TaskResult result = worker.execute(task);

        String draft = (String) result.getOutputData().get("draft");
        assertTrue(draft.contains("rapid adoption"));
    }

    @Test
    void outputContainsWordCount() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "managers"
        ));
        TaskResult result = worker.execute(task);

        Object wordCount = result.getOutputData().get("wordCount");
        assertNotNull(wordCount);
        assertTrue(((int) wordCount) > 0);
    }

    @Test
    void outputContainsModel() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "investors"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("writer-agent-v1", result.getOutputData().get("model"));
    }

    @Test
    void defaultsAudienceWhenMissing() {
        Task task = taskWith(Map.of("research", sampleResearch()));
        TaskResult result = worker.execute(task);

        String draft = (String) result.getOutputData().get("draft");
        assertTrue(draft.contains("general readers"));
    }

    @Test
    void defaultsAudienceWhenBlank() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "audience", "  "
        ));
        TaskResult result = worker.execute(task);

        String draft = (String) result.getOutputData().get("draft");
        assertTrue(draft.contains("general readers"));
    }

    private Map<String, Object> sampleResearch() {
        return Map.of(
                "subject", "cloud computing",
                "keyFacts", List.of(
                        "cloud computing has seen rapid adoption across enterprise environments",
                        "Industry analysts project continued double-digit growth over the next 5 years",
                        "Integration with existing systems remains the top implementation challenge"
                ),
                "statistics", Map.of(
                        "marketGrowth", "15.3% CAGR through 2030",
                        "regions", "North America leads adoption at 38% market share",
                        "enterprises", "72% of Fortune 500 companies have adopted related solutions"
                ),
                "sources", List.of(
                        "Industry Analysis Report 2025",
                        "Enterprise Technology Survey",
                        "Annual Technology Trends"
                )
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
