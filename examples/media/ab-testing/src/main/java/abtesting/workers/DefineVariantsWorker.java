package abtesting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DefineVariantsWorker implements Worker {
    @Override public String getTaskDefName() { return "abt_define_variants"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [define] Processing " + task.getInputData().getOrDefault("variants", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("variants", List.of());
        r.getOutputData().put("id", "A");
        r.getOutputData().put("name", task.getInputData().get("variantA"));
        r.getOutputData().put("trafficSplit", 50);
        return r;
    }
}
