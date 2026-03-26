package taxcalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class CalculateRatesWorker implements Worker {
    @Override public String getTaskDefName() { return "tax_calculate_rates"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> jurisdiction = (Map<String, Object>) task.getInputData().getOrDefault("jurisdiction", Map.of());
        double stateRate = 0.0625, countyRate = 0.01, cityRate = 0.0025;
        double combinedRate = stateRate + countyRate + cityRate;
        System.out.printf("  [rates] %s: state=%.4f, county=%.4f, city=%.4f, combined=%.4f%n", jurisdiction.getOrDefault("state", "CA"), stateRate, countyRate, cityRate, combinedRate);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("stateRate", stateRate); o.put("countyRate", countyRate); o.put("cityRate", cityRate); o.put("combinedRate", combinedRate);
        r.setOutputData(o); return r;
    }
}
