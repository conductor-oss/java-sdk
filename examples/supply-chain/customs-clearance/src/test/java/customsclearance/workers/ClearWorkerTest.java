package customsclearance.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ClearWorkerTest {
    @Test void taskDefName() { assertEquals("cst_clear", new ClearWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("shipmentId","SHP-001","origin","CN","destination","US","goods",List.of(Map.of("description","Elec","hsCode","8542","value",45000)),"declarationId","DEC-001","dutyAmount",2500.0,"clearanceId","CLR-001")));
        TaskResult r = new ClearWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
