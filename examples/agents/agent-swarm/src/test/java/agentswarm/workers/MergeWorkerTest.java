package agentswarm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeWorkerTest {

    private final MergeWorker worker = new MergeWorker();

    @Test
    void taskDefName() {
        assertEquals("as_merge", worker.getTaskDefName());
    }

    @Test
    void mergesFourSwarmResults() {
        Map<String, Object> r1 = Map.of("area", "Market Analysis",
                "findings", List.of("f1", "f2", "f3"), "confidence", 0.88, "sourcesConsulted", 14);
        Map<String, Object> r2 = Map.of("area", "Technical Landscape",
                "findings", List.of("f4", "f5", "f6"), "confidence", 0.91, "sourcesConsulted", 11);
        Map<String, Object> r3 = Map.of("area", "Use Cases",
                "findings", List.of("f7", "f8", "f9", "f10"), "confidence", 0.85, "sourcesConsulted", 18);
        Map<String, Object> r4 = Map.of("area", "Future Trends",
                "findings", List.of("f11", "f12", "f13"), "confidence", 0.82, "sourcesConsulted", 9);

        Task task = taskWith(Map.of("topic", "AI Research", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals("Research Report: AI Research", report.get("title"));
        assertEquals(13, report.get("totalFindings"));
        assertEquals(52, report.get("totalSources"));
    }

    @Test
    void computesCorrectAverageConfidence() {
        Map<String, Object> r1 = Map.of("area", "A", "findings", List.of("f1"),
                "confidence", 0.88, "sourcesConsulted", 10);
        Map<String, Object> r2 = Map.of("area", "B", "findings", List.of("f2"),
                "confidence", 0.91, "sourcesConsulted", 10);
        Map<String, Object> r3 = Map.of("area", "C", "findings", List.of("f3"),
                "confidence", 0.85, "sourcesConsulted", 10);
        Map<String, Object> r4 = Map.of("area", "D", "findings", List.of("f4"),
                "confidence", 0.82, "sourcesConsulted", 10);

        Task task = taskWith(Map.of("topic", "Test", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        double avgConfidence = (double) report.get("avgConfidence");
        // (0.88 + 0.91 + 0.85 + 0.82) / 4 = 0.865
        assertEquals(0.87, avgConfidence, 0.005);
    }

    @Test
    void reportContainsFourSections() {
        Map<String, Object> r1 = Map.of("area", "Market Analysis",
                "findings", List.of("f1", "f2", "f3"), "confidence", 0.88, "sourcesConsulted", 14);
        Map<String, Object> r2 = Map.of("area", "Technical Landscape",
                "findings", List.of("f4", "f5", "f6"), "confidence", 0.91, "sourcesConsulted", 11);
        Map<String, Object> r3 = Map.of("area", "Use Cases",
                "findings", List.of("f7", "f8", "f9", "f10"), "confidence", 0.85, "sourcesConsulted", 18);
        Map<String, Object> r4 = Map.of("area", "Future Trends",
                "findings", List.of("f11", "f12", "f13"), "confidence", 0.82, "sourcesConsulted", 9);

        Task task = taskWith(Map.of("topic", "Test Topic", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) report.get("sections");
        assertEquals(4, sections.size());

        List<String> areas = sections.stream().map(s -> (String) s.get("area")).toList();
        assertTrue(areas.contains("Market Analysis"));
        assertTrue(areas.contains("Technical Landscape"));
        assertTrue(areas.contains("Use Cases"));
        assertTrue(areas.contains("Future Trends"));
    }

    @Test
    void sectionsContainFindingCount() {
        Map<String, Object> r1 = Map.of("area", "A", "findings", List.of("f1", "f2", "f3"),
                "confidence", 0.9, "sourcesConsulted", 5);
        Map<String, Object> r2 = Map.of("area", "B", "findings", List.of("f4"),
                "confidence", 0.8, "sourcesConsulted", 3);
        Map<String, Object> r3 = Map.of("area", "C", "findings", List.of("f5", "f6"),
                "confidence", 0.7, "sourcesConsulted", 7);
        Map<String, Object> r4 = Map.of("area", "D", "findings", List.of("f7", "f8", "f9", "f10"),
                "confidence", 0.6, "sourcesConsulted", 2);

        Task task = taskWith(Map.of("topic", "Test", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) report.get("sections");
        assertEquals(3, sections.get(0).get("findingCount"));
        assertEquals(1, sections.get(1).get("findingCount"));
        assertEquals(2, sections.get(2).get("findingCount"));
        assertEquals(4, sections.get(3).get("findingCount"));
    }

    @Test
    void synthesisContainsTopic() {
        Map<String, Object> r1 = Map.of("area", "A", "findings", List.of("f1"),
                "confidence", 0.9, "sourcesConsulted", 5);
        Map<String, Object> r2 = Map.of("area", "B", "findings", List.of("f2"),
                "confidence", 0.8, "sourcesConsulted", 3);
        Map<String, Object> r3 = Map.of("area", "C", "findings", List.of("f3"),
                "confidence", 0.7, "sourcesConsulted", 7);
        Map<String, Object> r4 = Map.of("area", "D", "findings", List.of("f4"),
                "confidence", 0.6, "sourcesConsulted", 2);

        Task task = taskWith(Map.of("topic", "Quantum Computing", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        String synthesis = (String) report.get("synthesis");
        assertTrue(synthesis.contains("Quantum Computing"));
    }

    @Test
    void reportContainsAllExpectedFields() {
        Map<String, Object> r1 = Map.of("area", "A", "findings", List.of("f1"),
                "confidence", 0.9, "sourcesConsulted", 5);
        Map<String, Object> r2 = Map.of("area", "B", "findings", List.of("f2"),
                "confidence", 0.8, "sourcesConsulted", 3);
        Map<String, Object> r3 = Map.of("area", "C", "findings", List.of("f3"),
                "confidence", 0.7, "sourcesConsulted", 7);
        Map<String, Object> r4 = Map.of("area", "D", "findings", List.of("f4"),
                "confidence", 0.6, "sourcesConsulted", 2);

        Task task = taskWith(Map.of("topic", "Test", "result1", r1, "result2", r2,
                "result3", r3, "result4", r4));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertTrue(report.containsKey("title"));
        assertTrue(report.containsKey("sections"));
        assertTrue(report.containsKey("totalFindings"));
        assertTrue(report.containsKey("avgConfidence"));
        assertTrue(report.containsKey("totalSources"));
        assertTrue(report.containsKey("synthesis"));
    }

    @Test
    void handlesNullResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "Test");
        input.put("result1", null);
        input.put("result2", null);
        input.put("result3", null);
        input.put("result4", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertNotNull(report);
        assertEquals(0, report.get("totalFindings"));
        assertEquals(0, report.get("totalSources"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> r1 = Map.of("area", "A", "findings", List.of("f1"),
                "confidence", 0.9, "sourcesConsulted", 5);

        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("result1", r1);
        input.put("result2", Map.of());
        input.put("result3", Map.of());
        input.put("result4", Map.of());

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> report = (Map<String, Object>) result.getOutputData().get("report");
        assertEquals("Research Report: unspecified topic", report.get("title"));
    }

    @Test
    void handlesMissingResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "Test");

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
