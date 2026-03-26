package abandonedcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class ConvertWorker implements Worker {
    @Override public String getTaskDefName() { return "abc_convert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [convert] Conversion attempt for cart " + task.getInputData().get("cartId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("recovered", true); o.put("convertedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
