package aiimagegeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PromptWorkerTest {

    @Test
    void testPromptWorker() {
        PromptWorker worker = new PromptWorker();
        assertEquals("aig_prompt", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("prompt", "test", "style", "abstract"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String processedPrompt = (String) result.getOutputData().get("processedPrompt");
        assertTrue(processedPrompt.contains("test"));
        assertTrue(processedPrompt.contains("abstract"));
        assertEquals(42, result.getOutputData().get("tokens"));
    }
}
