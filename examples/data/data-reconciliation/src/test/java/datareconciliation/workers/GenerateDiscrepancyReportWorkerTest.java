package datareconciliation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateDiscrepancyReportWorkerTest {

    private final GenerateDiscrepancyReportWorker worker = new GenerateDiscrepancyReportWorker();

    @Test
    void taskDefName() {
        assertEquals("rc_generate_discrepancy_report", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void calculatesReconciliationRate() {
        List<String> matched = List.of("O1", "O2");
        List<Map<String, Object>> mismatched = List.of(
                Map.of("key", "O3", "differingFields", List.of("amount")));
        Task task = taskWith(Map.of("matched", matched, "mismatched", mismatched,
                "missingInA", List.of(), "missingInB", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("66.7%", result.getOutputData().get("reconciliationRate"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listsDiscrepancies() {
        List<Map<String, Object>> mismatched = List.of(
                Map.of("key", "O1", "differingFields", List.of("amount", "status")));
        Task task = taskWith(Map.of("matched", List.of(), "mismatched", mismatched,
                "missingInA", List.of("O3"), "missingInB", List.of("O2")));
        TaskResult result = worker.execute(task);

        List<String> discrepancies = (List<String>) result.getOutputData().get("discrepancies");
        assertEquals(3, discrepancies.size());
        assertTrue(discrepancies.get(0).contains("fields differ"));
        assertTrue(discrepancies.stream().anyMatch(d -> d.contains("missing in source B")));
        assertTrue(discrepancies.stream().anyMatch(d -> d.contains("missing in source A")));
    }

    @Test
    void handlesAllMatched() {
        Task task = taskWith(Map.of("matched", List.of("O1", "O2"), "mismatched", List.of(),
                "missingInA", List.of(), "missingInB", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("100.0%", result.getOutputData().get("reconciliationRate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
