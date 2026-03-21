package multiagentplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frontend estimation agent — takes components, complexity, and teamSize,
 * computes a per-component breakdown (designWeeks/devWeeks/testWeeks),
 * totalWeeks, calendarWeeks (ceil(totalWeeks / teamSize)), and risks.
 *
 * Interactive components get 2/4/1 weeks; others get 1/2/1.
 */
public class EstimateFrontendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pp_estimate_frontend";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<String> components = (List<String>) task.getInputData().get("components");
        if (components == null) {
            components = List.of();
        }
        int teamSize = toInt(task.getInputData().get("teamSize"), 2);

        System.out.println("  [pp_estimate_frontend] Estimating " + components.size()
                + " frontend components with team size " + teamSize);

        List<Map<String, Object>> breakdown = new ArrayList<>();
        int totalWeeks = 0;

        for (String component : components) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("component", component);

            boolean interactive = component.toLowerCase().contains("interactive");
            int designWeeks = interactive ? 2 : 1;
            int devWeeks = interactive ? 4 : 2;
            int testWeeks = interactive ? 1 : 1;

            entry.put("designWeeks", designWeeks);
            entry.put("devWeeks", devWeeks);
            entry.put("testWeeks", testWeeks);
            int subtotal = designWeeks + devWeeks + testWeeks;
            entry.put("subtotal", subtotal);
            totalWeeks += subtotal;
            breakdown.add(entry);
        }

        int calendarWeeks = (int) Math.ceil((double) totalWeeks / teamSize);

        List<String> risks = List.of(
                "Cross-browser compatibility may add testing time",
                "UX revisions could extend design phase"
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
