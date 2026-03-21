package codeinterpreter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeQuestionWorkerTest {

    private final AnalyzeQuestionWorker worker = new AnalyzeQuestionWorker();

    @Test
    void taskDefName() {
        assertEquals("ci_analyze_question", worker.getTaskDefName());
    }

    @Test
    void returnsAnalysisWithExpectedFields() {
        Task task = taskWith(Map.of(
                "question", "What is the average sales by region?",
                "dataset", Map.of("name", "sales", "rows", 100,
                        "columns", List.of("region", "sales"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertNotNull(analysis);
        assertEquals("statistical_analysis", analysis.get("type"));
        assertEquals("sales", analysis.get("targetColumn"));
        assertEquals("region", analysis.get("groupByColumn"));
        assertEquals("medium", analysis.get("complexity"));
    }

    @Test
    void analysisOperationsIncludeGroupByMeanSort() {
        Task task = taskWith(Map.of(
                "question", "Average by region",
                "dataset", Map.of("columns", List.of("region", "sales"))));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        List<String> operations = (List<String>) analysis.get("operations");
        assertEquals(3, operations.size());
        assertTrue(operations.contains("group_by"));
        assertTrue(operations.contains("mean"));
        assertTrue(operations.contains("sort"));
    }

    @Test
    void returnsDataSchemaMatchingDataset() {
        List<String> columns = List.of("region", "product", "sales", "quarter", "units");
        Task task = taskWith(Map.of(
                "question", "Analyze sales",
                "dataset", Map.of("columns", columns)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> dataSchema = (Map<String, Object>) result.getOutputData().get("dataSchema");
        assertNotNull(dataSchema);

        @SuppressWarnings("unchecked")
        List<String> schemaCols = (List<String>) dataSchema.get("columns");
        assertEquals(columns, schemaCols);

        @SuppressWarnings("unchecked")
        List<String> types = (List<String>) dataSchema.get("types");
        assertEquals(5, types.size());
        assertEquals("categorical", types.get(0)); // region
        assertEquals("numeric", types.get(2));      // sales
    }

    @Test
    void returnsLanguagePython() {
        Task task = taskWith(Map.of("question", "Analyze data"));
        TaskResult result = worker.execute(task);

        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("analysis"));
    }

    @Test
    void handlesMissingDataset() {
        Task task = taskWith(Map.of("question", "Some question"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> dataSchema = (Map<String, Object>) result.getOutputData().get("dataSchema");
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) dataSchema.get("columns");
        assertEquals(List.of("id", "value"), columns);
    }

    @Test
    void handlesDatasetWithNullColumns() {
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("name", "test");
        dataset.put("columns", null);
        Task task = taskWith(Map.of("question", "Test", "dataset", dataset));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> dataSchema = (Map<String, Object>) result.getOutputData().get("dataSchema");
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) dataSchema.get("columns");
        assertEquals(List.of("id", "value"), columns);
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(Map.of("question", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("analysis"));
        assertNotNull(result.getOutputData().get("dataSchema"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
