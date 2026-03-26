package thresholdalerting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PageOncallTest {

    private final PageOncall worker = new PageOncall();

    @Test void taskDefName() { assertEquals("th_page_oncall", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith("error_rate", 95));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsPaged() {
        TaskResult result = worker.execute(taskWith("error_rate", 95));
        assertEquals(true, result.getOutputData().get("paged"));
    }

    @Test void returnsChannelPagerduty() {
        TaskResult result = worker.execute(taskWith("error_rate", 95));
        assertEquals("pagerduty", result.getOutputData().get("channel"));
    }

    @Test void returnsOncallEngineer() {
        TaskResult result = worker.execute(taskWith("cpu_usage", 99));
        assertNotNull(result.getOutputData().get("oncallEngineer"));
    }

    @Test void returnsIncidentId() {
        TaskResult result = worker.execute(taskWith("error_rate", 95));
        String incidentId = (String) result.getOutputData().get("incidentId");
        assertTrue(incidentId.startsWith("INC-"));
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith("mem_usage", 98));
        assertNotNull(result.getOutputData().get("paged"));
        assertNotNull(result.getOutputData().get("channel"));
        assertNotNull(result.getOutputData().get("oncallEngineer"));
        assertNotNull(result.getOutputData().get("incidentId"));
    }

    private Task taskWith(String metricName, double value) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", metricName);
        input.put("currentValue", value);
        input.put("severity", "critical");
        task.setInputData(input);
        return task;
    }
}
