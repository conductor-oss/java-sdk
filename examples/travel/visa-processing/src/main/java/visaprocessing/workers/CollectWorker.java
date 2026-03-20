package visaprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "vsp_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Documents collected for " + task.getInputData().get("applicantId") + " (" + task.getInputData().get("visaType") + " visa)");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("applicationId", "VISA-546");
        r.getOutputData().put("documents", Map.of("passport",true,"photo",true,"invitation",true,"insurance",true));
        return r;
    }
}
