package itineraryplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SearchWorker implements Worker {
    @Override public String getTaskDefName() { return "itp_search"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [search] Found options for " + task.getInputData().get("days") + "-day trip to " + task.getInputData().get("destination"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("options", Map.of("flights",5,"hotels",8,"activities",12));
        return r;
    }
}
