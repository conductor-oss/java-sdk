package carrental.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SelectWorker implements Worker {
    @Override public String getTaskDefName() { return "crl_select"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [select] Selected midsize vehicle");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("selected", Map.of("model","Toyota Camry","class","midsize","dailyRate",65));
        return r;
    }
}
