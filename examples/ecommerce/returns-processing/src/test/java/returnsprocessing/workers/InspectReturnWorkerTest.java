package returnsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InspectReturnWorkerTest {

    private final InspectReturnWorker worker = new InspectReturnWorker();

    @Test
    void taskDefName() { assertEquals("ret_inspect", worker.getTaskDefName()); }

    @Test
    void returnsDecision() {
        Task task = taskWith(Map.of("returnId", "RET-1", "items", List.of(), "returnReason", "defective"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("refund", r.getOutputData().get("decision"));
        assertEquals("good", r.getOutputData().get("condition"));
    }

    @Test
    void returnsRefundAmount() {
        Task task = taskWith(Map.of("returnId", "RET-2", "items", List.of(), "returnReason", "wrong item"));
        TaskResult r = worker.execute(task);
        assertTrue(((Number) r.getOutputData().get("refundAmount")).doubleValue() > 0);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
