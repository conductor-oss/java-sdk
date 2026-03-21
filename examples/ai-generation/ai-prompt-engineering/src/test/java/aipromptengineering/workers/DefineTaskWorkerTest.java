package aipromptengineering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefineTaskWorkerTest {

    @Test
    void testDefineTaskWorker() {
        DefineTaskWorker worker = new DefineTaskWorker();
        assertEquals("ape_define_task", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("taskDescription", "test task"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("summarization-text-200", result.getOutputData().get("taskSpec"));
    }
}
