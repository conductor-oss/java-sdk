package pdfprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeContentWorkerTest {

    private final AnalyzeContentWorker worker = new AnalyzeContentWorker();

    @Test
    void taskDefName() {
        assertEquals("pd_analyze_content", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void countsWordsCorrectly() {
        List<Map<String, String>> sections = List.of(
                Map.of("title", "Intro", "content", "one two three"),
                Map.of("title", "Body", "content", "four five")
        );
        Task task = taskWith(Map.of("sections", sections));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("wordCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findsKeywords() {
        List<Map<String, String>> sections = List.of(
                Map.of("title", "T", "content", "data processing and analytics are important for pipelines")
        );
        Task task = taskWith(Map.of("sections", sections));
        TaskResult result = worker.execute(task);

        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertTrue(keywords.contains("data"));
        assertTrue(keywords.contains("processing"));
        assertTrue(keywords.contains("analytics"));
        assertTrue(keywords.contains("pipelines"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void calculatesAvgWordsPerSection() {
        List<Map<String, String>> sections = List.of(
                Map.of("title", "A", "content", "word1 word2 word3 word4"),
                Map.of("title", "B", "content", "word5 word6")
        );
        Task task = taskWith(Map.of("sections", sections));
        TaskResult result = worker.execute(task);

        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertEquals(3, analysis.get("avgWordsPerSection"));
    }

    @Test
    void handlesEmptySections() {
        Task task = taskWith(Map.of("sections", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("wordCount"));
    }

    @Test
    void handlesNullSections() {
        Map<String, Object> input = new HashMap<>();
        input.put("sections", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("wordCount"));
    }

    @Test
    void handlesMissingSections() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("wordCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noKeywordsWhenTextHasNone() {
        List<Map<String, String>> sections = List.of(
                Map.of("title", "T", "content", "hello world foo bar")
        );
        Task task = taskWith(Map.of("sections", sections));
        TaskResult result = worker.execute(task);

        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertTrue(keywords.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
