package multiagentplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure estimation agent — takes components, complexity, and teamSize,
 * computes a per-component breakdown (setupWeeks/configWeeks/testWeeks),
 * totalWeeks, calendarWeeks (ceil(totalWeeks / teamSize)), and risks.
 *
 * Kubernetes gets 3 setup weeks; others get 1. All get 1 config + 1 test week.
 */
public class EstimateInfraWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pp_estimate_infra";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<String> components = (List<String>) task.getInputData().get("components");
        if (components == null) {
            components = List.of();
        }
        int teamSize = toInt(task.getInputData().get("teamSize"), 2);

        System.out.println("  [pp_estimate_infra] Estimating " + components.size()
                + " infrastructure components with team size " + teamSize);

        List<Map<String, Object>> breakdown = new ArrayList<>();
        int totalWeeks = 0;

        for (String component : components) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("component", component);

            boolean kubernetes = component.toLowerCase().contains("kubernetes");
            int setupWeeks = kubernetes ? 3 : 1;
            int configWeeks = 1;
            int testWeeks = 1;

            entry.put("setupWeeks", setupWeeks);
            entry.put("configWeeks", configWeeks);
            entry.put("testWeeks", testWeeks);
            int subtotal = setupWeeks + configWeeks + testWeeks;
            entry.put("subtotal", subtotal);
            totalWeeks += subtotal;
            breakdown.add(entry);
        }

        int calendarWeeks = (int) Math.ceil((double) totalWeeks / teamSize);

        List<String> risks = List.of(
                "Cloud provider outages during setup",
                "Security compliance requirements may extend timelines"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("breakdown", breakdown);
        result.getOutputData().put("totalWeeks", totalWeeks);
        result.getOutputData().put("calendarWeeks", calendarWeeks);
        result.getOutputData().put("teamSize", teamSize);
        result.getOutputData().put("risks", risks);
        return result;
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
