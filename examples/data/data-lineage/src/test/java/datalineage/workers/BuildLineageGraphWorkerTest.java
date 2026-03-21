package datalineage.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildLineageGraphWorkerTest {

    private final BuildLineageGraphWorker worker = new BuildLineageGraphWorker();

    @Test
    void taskDefName() {
        assertEquals("ln_build_lineage_graph", worker.getTaskDefName());
    }

    @Test
    void buildsGraph() {
        List<Map<String, Object>> lineage = List.of(
                Map.of("name", "source_db"),
                Map.of("name", "uppercase"),
                Map.of("name", "lowercase"),
                Map.of("name", "dest_wh"));
        Task task = taskWith(Map.of("lineage", lineage, "recordCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("transformSteps"));
        assertEquals(4, result.getOutputData().get("depth"));
        String chain = (String) result.getOutputData().get("lineageChain");
        assertTrue(chain.contains("source_db"));
        assertTrue(chain.contains("dest_wh"));
    }

    @Test
    void summaryContainsRecordCount() {
        Task task = taskWith(Map.of(
                "lineage", List.of(Map.of("name", "src")),
                "recordCount", 42));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("42"));
    }

    @Test
    void handlesEmptyLineage() {
        Task task = taskWith(Map.of("lineage", List.of(), "recordCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("transformSteps"));
    }

    @Test
    void handlesNullLineage() {
        Map<String, Object> input = new HashMap<>();
        input.put("lineage", null);
        input.put("recordCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("depth"));
    }

    @Test
    void chainUsesArrowSeparator() {
        List<Map<String, Object>> lineage = List.of(
                Map.of("name", "A"), Map.of("name", "B"));
        Task task = taskWith(Map.of("lineage", lineage, "recordCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals("A -> B", result.getOutputData().get("lineageChain"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
