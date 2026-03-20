package eventdrivenmicroservices.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    private final FinalizeWorker worker = new FinalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_finalize", worker.getTaskDefName());
    }

    @Test
    void finalizesWorkflow() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-DM-1001",
                "eventsEmitted", 3,
                "servicesInvolved", 4));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("servicesCompleted"));
        assertEquals(3, result.getOutputData().get("eventsPublished"));
        assertEquals("completed", result.getOutputData().get("status"));
    }

    @Test
    void outputContainsServicesCompleted() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "eventsEmitted", 2,
                "servicesInvolved", 3));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("servicesCompleted"));
    }

    @Test
    void outputContainsEventsPublished() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "eventsEmitted", 5,
                "servicesInvolved", 6));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("eventsPublished"));
    }

    @Test
    void outputStatusIsCompleted() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "eventsEmitted", 1,
                "servicesInvolved", 2));
        TaskResult result = worker.execute(task);

        assertEquals("completed", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullEventsEmitted() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("eventsEmitted", null);
        input.put("servicesInvolved", 4);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("eventsPublished"));
    }

    @Test
    void handlesNullServicesInvolved() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-500");
        input.put("eventsEmitted", 3);
        input.put("servicesInvolved", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("servicesCompleted"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("servicesCompleted"));
        assertEquals(0, result.getOutputData().get("eventsPublished"));
        assertEquals("completed", result.getOutputData().get("status"));
    }

    @Test
    void handlesNullOrderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("eventsEmitted", 3);
        input.put("servicesInvolved", 4);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("servicesCompleted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
