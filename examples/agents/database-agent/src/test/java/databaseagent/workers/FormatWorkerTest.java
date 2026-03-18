package databaseagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatWorkerTest {

    private final FormatWorker worker = new FormatWorker();

    @Test
    void taskDefName() {
        assertEquals("db_format", worker.getTaskDefName());
    }

    @Test
    void returnsAnswerString() {
        Task task = taskWith(Map.of(
                "question", "What are the top departments?",
                "queryResults", List.of(Map.of("department", "Engineering")),
                "rowCount", 5,
                "query", "SELECT 1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Engineering"));
        assertTrue(answer.contains("Sales"));
        assertTrue(answer.contains("Marketing"));
    }

    @Test
    void answerMentionsAllFiveDepartments() {
        Task task = taskWith(Map.of(
                "question", "Top departments",
                "rowCount", 5));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Engineering"));
        assertTrue(answer.contains("Sales"));
        assertTrue(answer.contains("Marketing"));
        assertTrue(answer.contains("Finance"));
        assertTrue(answer.contains("Operations"));
    }

    @Test
    void returnsSummaryWithTotalRevenue() {
        Task task = taskWith(Map.of("question", "Revenue report", "rowCount", 5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertEquals(9850000, summary.get("totalRevenue"));
    }

    @Test
    void returnsSummaryWithTopDepartment() {
        Task task = taskWith(Map.of("question", "Best department", "rowCount", 5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals("Engineering", summary.get("topDepartment"));
    }

    @Test
    void returnsSummaryWithDepartmentCount() {
        Task task = taskWith(Map.of("question", "Department count", "rowCount", 5));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.getOutputData().get("summary");
        assertEquals(5, summary.get("departments"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("rowCount", 5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void handlesMissingRowCount() {
        Task task = taskWith(Map.of("question", "Some question"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("summary"));
    }

    @Test
    void handlesBlankQuestion() {
        Task task = taskWith(Map.of("question", "  ", "rowCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
