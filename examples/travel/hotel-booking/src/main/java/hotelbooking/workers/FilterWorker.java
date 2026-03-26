package hotelbooking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class FilterWorker implements Worker {
    @Override public String getTaskDefName() { return "htl_filter"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [filter] Filtered hotels by policy compliance");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bestMatch", Map.of("name","Marriott Downtown","rate",249,"rating",4.3));
        return r;
    }
}
