package taxcalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class DetermineJurisdictionWorker implements Worker {
    @Override public String getTaskDefName() { return "tax_determine_jurisdiction"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> addr = (Map<String, Object>) task.getInputData().getOrDefault("shippingAddress", Map.of());
        String state = (String) addr.getOrDefault("state", "CA");
        System.out.println("  [jurisdiction] Determining tax jurisdiction for " + state + ", US");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("jurisdiction", Map.of("state", "CA", "county", "Santa Clara", "city", "San Jose", "country", "US"));
        o.put("nexus", true); r.setOutputData(o); return r;
    }
}
