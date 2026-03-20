package titlesearch.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SearchRecordsWorker implements Worker {
    @Override public String getTaskDefName() { return "tts_search"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [tts_search] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", "found");
        result.getOutputData().put("certificateId", "TITLE-CERT-690");
        result.getOutputData().put("records", Map.of("deeds",4,"mortgages",2));
        return result;
    }
}
