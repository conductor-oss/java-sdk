package programevaluation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DefineMetricsWorkerTest {
    @Test void testExecute() { DefineMetricsWorker w = new DefineMetricsWorker(); assertEquals("pev_define_metrics", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("programName", "Test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
