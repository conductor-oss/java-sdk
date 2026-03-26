package couponengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidateCodeWorkerTest {
    private final ValidateCodeWorker worker = new ValidateCodeWorker();
    @Test void taskDefName() { assertEquals("cpn_validate_code", worker.getTaskDefName()); }
    @Test void validatesCode() {
        Task task = taskWith(Map.of("couponCode", "SUMMER20"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("valid"));
        assertEquals("percentage", r.getOutputData().get("discountType"));
        assertEquals(20, ((Number) r.getOutputData().get("discountValue")).intValue());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
