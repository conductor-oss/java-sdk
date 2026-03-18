package apigateway.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RouteRequestWorkerTest {
    @Test void executesSuccessfully() {
        RouteRequestWorker w = new RouteRequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("endpoint", "/api/users", "method", "GET",
                "clientId", "client-1", "rawResponse", Map.of("data", "test"),
                "transformedResponse", Map.of("data", "test"), "statusCode", 200)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
