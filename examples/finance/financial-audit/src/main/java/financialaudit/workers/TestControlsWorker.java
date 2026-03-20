package financialaudit.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class TestControlsWorker implements Worker {
    @Override public String getTaskDefName() { return "fau_test_controls"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [controls] Testing controls with " + task.getInputData().get("evidenceItems") + " evidence items");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("controlEffectiveness", "adequate"); r.getOutputData().put("findingsCount", 3);
        r.getOutputData().put("findings", List.of(Map.of("id","F-001","severity","medium","area","accounts_payable","description","Segregation of duties gap"),Map.of("id","F-002","severity","low","area","inventory","description","Count discrepancy in 2 locations"),Map.of("id","F-003","severity","low","area","payroll","description","Missing approval on 3 overtime entries")));
        r.getOutputData().put("controlsTestedCount", 24);
        return r;
    }
}
