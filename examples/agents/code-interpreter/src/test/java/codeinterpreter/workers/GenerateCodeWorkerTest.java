package codeinterpreter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCodeWorkerTest {

    private final GenerateCodeWorker worker = new GenerateCodeWorker();

    @Test
    void taskDefName() {
        assertEquals("ci_generate_code", worker.getTaskDefName());
    }

    @Test
    void returnsPythonCodeWithPandas() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region"),
                "dataSchema", Map.of("columns", List.of("region", "sales")),
                "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String code = (String) result.getOutputData().get("code");
        assertNotNull(code);
        assertTrue(code.contains("import pandas as pd"));
        assertTrue(code.contains("groupby"));
    }

    @Test
    void codeUsesTargetAndGroupByColumns() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "revenue", "groupByColumn", "department"),
                "language", "python"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("'department'"));
        assertTrue(code.contains("'revenue'"));
        assertTrue(code.contains("Group by department"));
    }

    @Test
    void returnsLanguage() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region"),
                "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void returnsLinesOfCodeMatchingActualLines() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region"),
                "language", "python"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        int expected = code.split("\n").length;
        assertEquals(expected, result.getOutputData().get("linesOfCode"));
    }

    @Test
    void codeContainsSortAndPrint() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region"),
                "language", "python"));
        TaskResult result = worker.execute(task);

        String code = (String) result.getOutputData().get("code");
        assertTrue(code.contains("sort_values"));
        assertTrue(code.contains("print"));
    }

    @Test
    void handlesNullAnalysis() {
        Map<String, Object> input = new HashMap<>();
        input.put("analysis", null);
        input.put("language", "python");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String code = (String) result.getOutputData().get("code");
        assertNotNull(code);
        assertTrue(code.contains("sales"));
        assertTrue(code.contains("region"));
    }

    @Test
    void handlesBlankLanguageDefaultsToPython() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region"),
                "language", "  "));
        TaskResult result = worker.execute(task);

        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void handlesMissingLanguage() {
        Task task = taskWith(Map.of(
                "analysis", Map.of("targetColumn", "sales", "groupByColumn", "region")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("python", result.getOutputData().get("language"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
