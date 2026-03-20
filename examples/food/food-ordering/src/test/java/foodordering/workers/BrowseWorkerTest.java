package foodordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BrowseWorkerTest {

    @Test
    void testExecute() {
        BrowseWorker worker = new BrowseWorker();
        assertEquals("fod_browse", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("restaurantId", "REST-10"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("selectedItems"));
        assertInstanceOf(List.class, result.getOutputData().get("selectedItems"));
    }
}
