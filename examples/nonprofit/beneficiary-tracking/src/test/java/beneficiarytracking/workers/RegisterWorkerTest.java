package beneficiarytracking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class RegisterWorkerTest {
    @Test void testExecute() { RegisterWorker w = new RegisterWorker(); assertEquals("btr_register", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("beneficiaryName", "Test", "location", "Test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
