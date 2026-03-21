package bidmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "bid_collect"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> responses = List.of(
            Map.of("vendor","Alpha Corp","amount",85000,"timeline","8 weeks"),
            Map.of("vendor","Beta Ltd","amount",55000,"timeline","10 weeks"),
            Map.of("vendor","Gamma Inc","amount",91000,"timeline","6 weeks"));
        System.out.println("  [collect] Received " + responses.size() + " bid responses");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("responses", responses); r.getOutputData().put("responseCount", responses.size()); return r;
    }
}
