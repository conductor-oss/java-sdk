package ragsql.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatResultsWorkerTest {

    private final FormatResultsWorker worker = new FormatResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("sq_format_results", worker.getTaskDefName());
    }

    @Test
    void formatsAnswerWithRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("workflow_name", "order_processing");
        row1.put("execution_count", 1250);
        row1.put("avg_duration_sec", 3.2);
        rows.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("workflow_name", "payment_flow");
        row2.put("execution_count", 980);
        row2.put("avg_duration_sec", 2.8);
        rows.add(row2);

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What are the most executed workflows?",
                "sql", "SELECT workflow_name FROM t",
                "rows", rows,
                "rowCount", 2
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("What are the most executed workflows?"));
        assertTrue(answer.contains("2 results"));
        assertTrue(answer.contains("order_processing"));
        assertTrue(answer.contains("1250"));
        assertTrue(answer.contains("3.2"));
    }

    @Test
    void formatsAnswerWithEmptyRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Any workflows?",
                "sql", "SELECT * FROM t",
                "rows", List.of(),
                "rowCount", 0
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("0 results"));
    }

    @Test
    void handlesNullQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesNullRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "sql", "SELECT 1"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 results"));
    }

    @Test
    void rowCountFromInput() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workflow_name", "test_wf");
        row.put("execution_count", 100);
        row.put("avg_duration_sec", 1.0);
        rows.add(row);

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "Count?",
                "sql", "SELECT 1",
                "rows", rows,
                "rowCount", 1
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("1 results"));
        assertTrue(answer.contains("test_wf"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
