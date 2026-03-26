package threeagentpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalOutputWorkerTest {

    private final FinalOutputWorker worker = new FinalOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("thr_final_output", worker.getTaskDefName());
    }

    @Test
    void assemblesFinalReport() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "A comprehensive article about cloud computing.",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        assertNotNull(finalReport);
    }

    @Test
    void finalReportContainsContent() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "The article content here.",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        assertEquals("The article content here.", finalReport.get("content"));
    }

    @Test
    void finalReportContainsReviewScore() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "Draft text",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        assertEquals(8.5, finalReport.get("reviewScore"));
    }

    @Test
    void finalReportContainsVerdict() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "Draft text",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        assertEquals("APPROVED_WITH_MINOR_REVISIONS", finalReport.get("verdict"));
    }

    @Test
    void finalReportContainsSuggestions() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "Draft text",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) finalReport.get("suggestions");
        assertEquals(3, suggestions.size());
    }

    @Test
    void finalReportContainsSourcesUsed() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "Draft text",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        @SuppressWarnings("unchecked")
        List<String> sourcesUsed = (List<String>) finalReport.get("sourcesUsed");
        assertEquals(3, sourcesUsed.size());
    }

    @Test
    void finalReportContainsAgentsPipeline() {
        Task task = taskWith(Map.of(
                "research", sampleResearch(),
                "draft", "Draft text",
                "review", sampleReview()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalReport = (Map<String, Object>) result.getOutputData().get("finalReport");
        @SuppressWarnings("unchecked")
        List<String> pipeline = (List<String>) finalReport.get("agentsPipeline");

        assertEquals(3, pipeline.size());
        assertEquals("researcher-agent-v1", pipeline.get(0));
        assertEquals("writer-agent-v1", pipeline.get(1));
        assertEquals("reviewer-agent-v1", pipeline.get(2));
    }

    private Map<String, Object> sampleResearch() {
        return Map.of(
                "subject", "cloud computing",
                "keyFacts", List.of("fact1", "fact2", "fact3"),
                "statistics", Map.of("marketGrowth", "15%"),
                "sources", List.of(
                        "Industry Analysis Report 2025",
                        "Enterprise Technology Survey",
                        "Annual Technology Trends"
                )
        );
    }

    private Map<String, Object> sampleReview() {
        return Map.of(
                "score", 8.5,
                "maxScore", 10,
                "accuracy", "High",
                "clarity", "Good",
                "audienceFit", "Strong",
                "suggestions", List.of(
                        "Add a case study",
                        "Include year-over-year comparisons",
                        "Add an executive summary"
                ),
                "verdict", "APPROVED_WITH_MINOR_REVISIONS"
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
