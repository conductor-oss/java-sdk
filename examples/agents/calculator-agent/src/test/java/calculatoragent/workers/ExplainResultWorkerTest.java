package calculatoragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExplainResultWorkerTest {

    private final ExplainResultWorker worker = new ExplainResultWorker();

    @Test
    void taskDefName() {
        assertEquals("ca_explain_result", worker.getTaskDefName());
    }

    @Test
    void happyPathReturnsExplanation() {
        List<Map<String, Object>> steps = List.of(
                Map.of("step", 1, "operation", "8+4=12"),
                Map.of("step", 2, "operation", "15*12=180"),
                Map.of("step", 3, "operation", "12/3=4"),
                Map.of("step", 4, "operation", "180-4=176")
        );
        Task task = taskWith(Map.of(
                "expression", "15 * (8 + 4) - 12 / 3",
                "steps", steps,
                "finalResult", 176
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String explanation = (String) result.getOutputData().get("explanation");
        assertNotNull(explanation);
        assertTrue(explanation.contains("176"));
        assertTrue(explanation.contains("PEMDAS"));
    }

    @Test
    void explanationContainsExpression() {
        Task task = taskWith(Map.of(
                "expression", "15 * (8 + 4) - 12 / 3",
                "steps", List.of(Map.of("step", 1)),
                "finalResult", 176
        ));
        TaskResult result = worker.execute(task);

        String explanation = (String) result.getOutputData().get("explanation");
        assertTrue(explanation.contains("15 * (8 + 4) - 12 / 3"));
    }

    @Test
    void stepCountMatchesInputSteps() {
        List<Map<String, Object>> steps = List.of(
                Map.of("step", 1, "operation", "8+4=12"),
                Map.of("step", 2, "operation", "15*12=180"),
                Map.of("step", 3, "operation", "12/3=4"),
                Map.of("step", 4, "operation", "180-4=176")
        );
        Task task = taskWith(Map.of(
                "expression", "15 * (8 + 4) - 12 / 3",
                "steps", steps,
                "finalResult", 176
        ));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("stepCount"));
    }

    @Test
    void difficultyIsIntermediate() {
        Task task = taskWith(Map.of(
                "expression", "15 * (8 + 4) - 12 / 3",
                "steps", List.of(Map.of("step", 1)),
                "finalResult", 176
        ));
        TaskResult result = worker.execute(task);

        assertEquals("intermediate", result.getOutputData().get("difficulty"));
    }

    @Test
    void handlesNullExpression() {
        Task task = taskWith(Map.of(
                "steps", List.of(Map.of("step", 1)),
                "finalResult", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String explanation = (String) result.getOutputData().get("explanation");
        assertTrue(explanation.contains("unknown"));
    }

    @Test
    void handlesBlankExpression() {
        Map<String, Object> input = new HashMap<>();
        input.put("expression", "   ");
        input.put("steps", List.of());
        input.put("finalResult", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String explanation = (String) result.getOutputData().get("explanation");
        assertTrue(explanation.contains("unknown"));
    }

    @Test
    void stepCountZeroWhenNoSteps() {
        Task task = taskWith(Map.of(
                "expression", "0",
                "steps", List.of(),
                "finalResult", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("stepCount"));
    }

    @Test
    void handlesFinalResultAsInteger() {
        Task task = taskWith(Map.of(
                "expression", "2 + 3",
                "steps", List.of(Map.of("step", 1)),
                "finalResult", 5
        ));
        TaskResult result = worker.execute(task);

        String explanation = (String) result.getOutputData().get("explanation");
        assertTrue(explanation.contains("5"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
