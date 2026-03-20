package payrollworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CalculateGrossWorkerTest {
    private final CalculateGrossWorker worker = new CalculateGrossWorker();
    @Test void taskDefName() { assertEquals("prl_calculate_gross", worker.getTaskDefName()); }
    @Test void calculatesGross() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        List<Map<String,Object>> records = List.of(Map.of("empId","E-1","regularHours",80,"overtimeHours",0,"rate",50.00));
        t.setInputData(new HashMap<>(Map.of("employeeRecords", records)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("4000.00", r.getOutputData().get("grossPayroll"));
        assertEquals(1, r.getOutputData().get("employeeCount"));
    }
}
