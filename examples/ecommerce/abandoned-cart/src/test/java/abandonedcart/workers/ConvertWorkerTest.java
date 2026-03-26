package abandonedcart.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConvertWorkerTest {
    private final ConvertWorker w = new ConvertWorker();
    @Test void taskDefName() { assertEquals("abc_convert", w.getTaskDefName()); }
    @Test void converts() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("cartId", "C1", "customerId", "U1", "discountCode", "SAVE10")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("recovered"));
    }
}
