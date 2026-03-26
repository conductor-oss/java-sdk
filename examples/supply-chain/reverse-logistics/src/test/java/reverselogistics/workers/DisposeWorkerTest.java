package reverselogistics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DisposeWorkerTest {
    @Test void taskDefName() { assertEquals("rvl_dispose", new DisposeWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("returnId","RET-001","product","Headphones","reason","defective","condition","refurbish")));
        TaskResult r = new DisposeWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
