package travelanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "tan_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Gathering travel data for " + task.getInputData().get("department") + ", " + task.getInputData().get("period"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rawData", Map.of("bookings",245,"travelers",82,"sources",List.of("flights","hotels","542s","meals"))); return r;
    }
}
