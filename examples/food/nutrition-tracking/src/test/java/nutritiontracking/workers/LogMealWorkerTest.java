package nutritiontracking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LogMealWorkerTest {
    @Test void testExecute() {
        LogMealWorker worker = new LogMealWorker();
        assertEquals("nut_log_meal", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("userId", "USR-55", "mealType", "lunch"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("loggedFoods"));
    }
}
