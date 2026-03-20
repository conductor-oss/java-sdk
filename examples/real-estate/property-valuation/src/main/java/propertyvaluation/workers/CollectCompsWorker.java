package propertyvaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class CollectCompsWorker implements Worker {
    @Override public String getTaskDefName() { return "pvl_collect_comps"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [comps] Collecting comparables near " + task.getInputData().get("address"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("comps", List.of(Map.of("address","125 Oak Lane","price",460000,"sqft",2100),Map.of("address","130 Oak Lane","price",490000,"sqft",2300),Map.of("address","118 Elm St","price",445000,"sqft",2000)));
        return result;
    }
}
