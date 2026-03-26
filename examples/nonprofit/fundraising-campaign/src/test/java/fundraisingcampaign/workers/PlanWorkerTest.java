package fundraisingcampaign.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PlanWorkerTest {
    @Test void testExecute() { PlanWorker w = new PlanWorker(); assertEquals("frc_plan", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("campaignName", "Test", "goalAmount", 100000));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
