package returnsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveReturnWorkerTest {

    private final ReceiveReturnWorker worker = new ReceiveReturnWorker();

    @Test
    void taskDefName() { assertEquals("ret_receive", worker.getTaskDefName()); }

    @Test
    void returnsReturnId() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "items", List.of(Map.of("sku", "A")), "customerId", "c1"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("returnId").toString().startsWith("RET-"));
    }

    @Test
    void returnIdIsUnique() {
        Task t1 = taskWith(Map.of("orderId", "A", "items", List.of(), "customerId", "c1"));
        Task t2 = taskWith(Map.of("orderId", "B", "items", List.of(), "customerId", "c2"));
        assertNotEquals(worker.execute(t1).getOutputData().get("returnId"),
                worker.execute(t2).getOutputData().get("returnId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
