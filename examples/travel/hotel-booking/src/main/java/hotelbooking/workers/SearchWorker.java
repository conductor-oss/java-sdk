package hotelbooking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class SearchWorker implements Worker {
    @Override public String getTaskDefName() { return "htl_search"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [search] Searching hotels in " + task.getInputData().get("city"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("hotels", List.of(Map.of("name","Grand Hyatt","rate",289,"rating",4.5),Map.of("name","Marriott Downtown","rate",249,"rating",4.3),Map.of("name","Hilton Garden Inn","rate",179,"rating",4.1)));
        return r;
    }
}
