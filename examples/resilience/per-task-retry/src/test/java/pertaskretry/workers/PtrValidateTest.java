package pertaskretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PtrValidateTest {

    private final PtrValidate worker = new PtrValidate();

    @Test
    void taskDefName() {
        assertEquals("ptr_validate", worker.getTaskDefName());
    }

    @Test
    void returnsValidResult() {
        Task task = taskWith("ORD-001");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("result"));
        assertEquals("ORD-001", result.getOutputData().get("orderId"));
    }

    @Test
    void handlesNullOrderId() {
        Task task = taskWith(null);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("valid", result.getOutputData().get("result"));
        assertNull(result.getOutputData().get("orderId"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith("ORD-TEST");

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("orderId"));
    }

    private Task taskWith(String orderId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", orderId);
        task.setInputData(input);
        return task;
    }
}
