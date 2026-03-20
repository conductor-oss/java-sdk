package dashboarddata.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildWidgetsWorkerTest {

    private final BuildWidgetsWorker worker = new BuildWidgetsWorker();

    @Test
    void taskDefName() {
        assertEquals("dh_build_widgets", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("kpis", List.of(), "metrics", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsSixWidgets() {
        Task task = taskWith(Map.of("kpis", List.of(), "metrics", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(6, result.getOutputData().get("widgetCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void widgetsHaveIds() {
        Task task = taskWith(Map.of("kpis", List.of(), "metrics", Map.of()));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> widgets = (List<Map<String, Object>>) result.getOutputData().get("widgets");
        for (Map<String, Object> w : widgets) {
            assertNotNull(w.get("id"));
            assertNotNull(w.get("type"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
