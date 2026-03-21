package custommetrics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UpdateDashboardTest {

    private final UpdateDashboard worker = new UpdateDashboard();

    @Test void taskDefName() { assertEquals("cus_update_dashboard", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith(4));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsDashboardUpdated() {
        TaskResult result = worker.execute(taskWith(4));
        assertEquals(true, result.getOutputData().get("dashboardUpdated"));
    }

    @Test void returnsDashboardUrl() {
        TaskResult result = worker.execute(taskWith(4));
        String url = (String) result.getOutputData().get("dashboardUrl");
        assertTrue(url.contains("dashboard.example.com"));
    }

    @Test void returnsUpdatedAt() {
        TaskResult result = worker.execute(taskWith(4));
        assertNotNull(result.getOutputData().get("updatedAt"));
    }

    @Test void handlesNullMetricCount() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith(4));
        assertNotNull(result.getOutputData().get("dashboardUpdated"));
        assertNotNull(result.getOutputData().get("dashboardUrl"));
        assertNotNull(result.getOutputData().get("updatedAt"));
    }

    private Task taskWith(int metricCount) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("metricCount", metricCount, "aggregatedMetrics", List.of())));
        return task;
    }
}
