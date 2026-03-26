package eventordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessNextWorkerTest {

    private final ProcessNextWorker worker = new ProcessNextWorker();

    private static final List<Map<String, Object>> SORTED_EVENTS = List.of(
            Map.of("seq", 1, "type", "create", "data", "first"),
            Map.of("seq", 2, "type", "modify", "data", "second"),
            Map.of("seq", 3, "type", "update", "data", "third")
    );

    @Test
    void taskDefName() {
        assertEquals("oo_process_next", worker.getTaskDefName());
    }

    @Test
    void processesFirstEvent() {
        Task task = taskWith(Map.of(
                "sortedEvents", SORTED_EVENTS,
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("processedSeq"));
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void processesSecondEvent() {
        Task task = taskWith(Map.of(
                "sortedEvents", SORTED_EVENTS,
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("processedSeq"));
        assertEquals(1, result.getOutputData().get("iteration"));
    }

    @Test
    void processesThirdEvent() {
        Task task = taskWith(Map.of(
                "sortedEvents", SORTED_EVENTS,
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("processedSeq"));
        assertEquals(2, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesOutOfBoundsIteration() {
        Task task = taskWith(Map.of(
                "sortedEvents", SORTED_EVENTS,
                "iteration", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedSeq"));
    }

    @Test
    void handlesNullSortedEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("sortedEvents", null);
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedSeq"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("sortedEvents", SORTED_EVENTS));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("processedSeq"));
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedSeq"));
        assertEquals(0, result.getOutputData().get("iteration"));
    }

    @Test
    void outputsIterationValue() {
        Task task = taskWith(Map.of(
                "sortedEvents", SORTED_EVENTS,
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("iteration"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
