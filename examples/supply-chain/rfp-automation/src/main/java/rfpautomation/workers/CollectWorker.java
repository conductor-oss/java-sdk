package rfpautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "rfp_collect"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> proposals = List.of(
            Map.of("vendor","TechSolutions","score",88,"price",120000),
            Map.of("vendor","InnovateCo","score",92,"price",150000),
            Map.of("vendor","BuildRight","score",85,"price",105000),
            Map.of("vendor","ProServices","score",90,"price",118000));
        System.out.println("  [collect] Received " + proposals.size() + " proposals");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("proposals", proposals); r.getOutputData().put("proposalCount", proposals.size()); return r;
    }
}
