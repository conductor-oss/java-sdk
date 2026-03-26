package reverselogistics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ReceiveReturnWorkerTest {
    @Test void taskDefName() { assertEquals("rvl_receive_return", new ReceiveReturnWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("returnId","RET-001","product","Headphones","reason","defective","condition","refurbish")));
        TaskResult r = new ReceiveReturnWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
