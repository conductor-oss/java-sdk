package citizenrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteWorkerTest {

    @Test
    void testRouteWorker() {
        RouteWorker worker = new RouteWorker();
        assertEquals("ctz_route", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("category", "infrastructure", "priority", "medium"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Public Works", result.getOutputData().get("department"));
    }
}
