package customsclearance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "cst_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Documents for " + task.getInputData().get("declarationId") + " verified");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true); r.getOutputData().put("documents", List.of("invoice","packing_list","bill_of_lading")); return r;
    }
}
