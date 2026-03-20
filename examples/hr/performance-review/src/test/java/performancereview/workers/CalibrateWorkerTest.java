package performancereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalibrateWorkerTest {

    private final CalibrateWorker worker = new CalibrateWorker();

    @Test
    void taskDefName() {
        assertEquals("pfr_calibrate", worker.getTaskDefName());
    }

    @Test
    void calibratesRatings() {
        Task task = taskWith(Map.of("selfRating", 4, "managerRating", 4.2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("4.1", result.getOutputData().get("finalRating"));
        assertEquals("exceeds-expectations", result.getOutputData().get("band"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
