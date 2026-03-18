package reactagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReasonWorkerTest {

    private final ReasonWorker worker = new ReasonWorker();

    @Test
    void taskDefName() {
        assertEquals("rx_reason", worker.getTaskDefName());
    }

    @Test
    void firstIterationReturnsSearchAction() {
        Task task = taskWith(Map.of("question", "What is the current world population?", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("search", result.getOutputData().get("action"));
        assertNotNull(result.getOutputData().get("thought"));
        assertNotNull(result.getOutputData().get("query"));
    }

    @Test
    void secondIterationReturnsSearchAction() {
        Task task = taskWith(Map.of("question", "What is the current world population?", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("search", result.getOutputData().get("action"));
    }

    @Test
    void thirdIterationReturnsSynthesizeAction() {
        Task task = taskWith(Map.of("question", "What is the current world population?", "iteration", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("synthesize", result.getOutputData().get("action"));
    }

    @Test
    void eachIterationHasDistinctThought() {
        TaskResult r1 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 1)));
        TaskResult r2 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 2)));
        TaskResult r3 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 3)));

        String t1 = (String) r1.getOutputData().get("thought");
        String t2 = (String) r2.getOutputData().get("thought");
        String t3 = (String) r3.getOutputData().get("thought");

        assertNotEquals(t1, t2);
        assertNotEquals(t2, t3);
        assertNotEquals(t1, t3);
    }

    @Test
    void eachIterationHasDistinctQuery() {
        TaskResult r1 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 1)));
        TaskResult r2 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 2)));
        TaskResult r3 = worker.execute(taskWith(Map.of("question", "Q", "iteration", 3)));

        String q1 = (String) r1.getOutputData().get("query");
        String q2 = (String) r2.getOutputData().get("query");
        String q3 = (String) r3.getOutputData().get("query");

        assertNotEquals(q1, q2);
        assertNotEquals(q2, q3);
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("question", "What is the current world population?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("thought"));
        assertNotNull(result.getOutputData().get("action"));
    }

    @Test
    void handlesNullQuestion() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("thought"));
    }

    @Test
    void clampsHighIteration() {
        Task task = taskWith(Map.of("question", "Q", "iteration", 99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("synthesize", result.getOutputData().get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
