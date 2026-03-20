package deploymentai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PredictRiskWorkerTest {
    private final PredictRiskWorker worker = new PredictRiskWorker();
    @Test void taskDefName() { assertEquals("dai_predict_risk", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("changeAnalysis", java.util.Map.of("filesChanged",8,"dbMigrations",1,"apiChanges",2,"configChanges",0));
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
