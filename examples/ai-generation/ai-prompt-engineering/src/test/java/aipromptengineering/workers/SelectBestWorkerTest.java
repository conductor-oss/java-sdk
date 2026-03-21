package aipromptengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SelectBestWorkerTest {

    @Test
    void testSelectBestWorker() {
        SelectBestWorker worker = new SelectBestWorker();
        assertEquals("ape_select_best", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("rankings", "P1-P2"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("P3", result.getOutputData().get("bestPromptId"));
        assertEquals(0.91, result.getOutputData().get("bestScore"));
    }
}
