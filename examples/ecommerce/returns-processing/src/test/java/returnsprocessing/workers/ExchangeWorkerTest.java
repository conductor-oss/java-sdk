package returnsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeWorkerTest {

    private final ExchangeWorker worker = new ExchangeWorker();

    @Test
    void taskDefName() { assertEquals("ret_exchange", worker.getTaskDefName()); }

    @Test
    void returnsExchangeOrderId() {
        Task task = taskWith(Map.of("returnId", "RET-1", "orderId", "ORD-1", "items", List.of(), "customerId", "c1"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("exchangeOrderId").toString().startsWith("EXC-"));
        assertEquals(true, r.getOutputData().get("exchanged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
