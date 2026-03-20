package aivideogeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RenderWorkerTest {

    @Test
    void testRenderWorker() {
        RenderWorker worker = new RenderWorker();
        assertEquals("avg_render", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("videoId", "VID-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("VID-ai-video-generation-001-F", result.getOutputData().get("finalVideoId"));
        assertEquals(30, result.getOutputData().get("duration"));
        assertEquals(true, result.getOutputData().get("complete"));
    }
}
