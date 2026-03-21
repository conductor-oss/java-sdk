package aivideogeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScriptWorkerTest {

    @Test
    void testScriptWorker() {
        ScriptWorker worker = new ScriptWorker();
        assertEquals("avg_script", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("topic", "test topic", "duration", "30"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("scenes"));
        assertEquals(320, result.getOutputData().get("wordCount"));
    }
}
