package searchagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SynthesizeWorkerTest {

    private final SynthesizeWorker worker = new SynthesizeWorker();

    @Test
    void taskDefName() {
        assertEquals("sa_synthesize", worker.getTaskDefName());
    }

    @Test
    void returnsAnswerContainingQuestion() {
        Task task = taskWith(Map.of(
                "question", "What is quantum computing?",
                "rankedResults", List.of(
                        Map.of("title", "QC Article", "snippet", "Quantum computing uses qubits.",
                                "relevance", 0.9, "source", "google")),
                "topSources", List.of("QC Article")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("What is quantum computing?"));
    }

    @Test
    void returnsAnswerTypeSynthesis() {
        Task task = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(
                        Map.of("snippet", "Some info", "source", "google")),
                "topSources", List.of("Source")));
        TaskResult result = worker.execute(task);

        assertEquals("synthesis", result.getOutputData().get("answerType"));
    }

    @Test
    void returnsConfidenceAsDouble() {
        Task task = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(
                        Map.of("snippet", "Info", "source", "google", "relevance", 0.9),
                        Map.of("snippet", "More", "source", "wikipedia", "relevance", 0.85)),
                "topSources", List.of("A", "B")));
        TaskResult result = worker.execute(task);

        Object confidence = result.getOutputData().get("confidence");
        assertNotNull(confidence);
        assertInstanceOf(Double.class, confidence);
        double val = (Double) confidence;
        assertTrue(val > 0.0 && val <= 1.0);
    }

    @Test
    void returnsSourcesUsedFromResults() {
        Task task = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(
                        Map.of("snippet", "A", "source", "google"),
                        Map.of("snippet", "B", "source", "wikipedia"),
                        Map.of("snippet", "C", "source", "google")),
                "topSources", List.of("X")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> sourcesUsed = (List<String>) result.getOutputData().get("sourcesUsed");
        assertNotNull(sourcesUsed);
        assertTrue(sourcesUsed.contains("google"));
        assertTrue(sourcesUsed.contains("wikipedia"));
        assertEquals(2, sourcesUsed.size()); // distinct
    }

    @Test
    void handlesEmptyRankedResults() {
        Task task = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(),
                "topSources", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No relevant sources"));
    }

    @Test
    void handlesNullRankedResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "test");
        input.put("rankedResults", null);
        input.put("topSources", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("rankedResults", List.of(
                Map.of("snippet", "Data", "source", "google")));
        input.put("topSources", List.of("Src"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void confidenceHigherWithMultipleSources() {
        // Single source
        Task singleTask = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(
                        Map.of("snippet", "A", "source", "google"),
                        Map.of("snippet", "B", "source", "google")),
                "topSources", List.of("X")));
        TaskResult singleResult = worker.execute(singleTask);
        double singleConfidence = (Double) singleResult.getOutputData().get("confidence");

        // Multiple sources
        Task multiTask = taskWith(Map.of(
                "question", "test",
                "rankedResults", List.of(
                        Map.of("snippet", "A", "source", "google"),
                        Map.of("snippet", "B", "source", "wikipedia")),
                "topSources", List.of("X")));
        TaskResult multiResult = worker.execute(multiTask);
        double multiConfidence = (Double) multiResult.getOutputData().get("confidence");

        assertTrue(multiConfidence > singleConfidence,
                "Multiple sources should yield higher confidence");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
