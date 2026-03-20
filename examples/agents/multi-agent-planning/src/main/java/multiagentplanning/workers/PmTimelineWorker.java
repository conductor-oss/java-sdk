package multiagentplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PM timeline agent — takes all three estimates and the architecture summary,
 * computes total project duration = infraCalendar + max(feCalendar, beCalendar) + 2 buffer,
 * creates 5 milestones, and determines riskLevel based on total effort weeks.
 */
public class PmTimelineWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pp_pm_timeline";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String projectName = (String) task.getInputData().get("projectName");
        if (projectName == null || projectName.isBlank()) {
            projectName = "Unnamed Project";
        }

        Map<String, Object> feEstimate = (Map<String, Object>) task.getInputData().get("frontendEstimate");
        Map<String, Object> beEstimate = (Map<String, Object>) task.getInputData().get("backendEstimate");
        Map<String, Object> infraEstimate = (Map<String, Object>) task.getInputData().get("infraEstimate");

        int feCalendar = extractInt(feEstimate, "calendarWeeks", 0);
        int beCalendar = extractInt(beEstimate, "calendarWeeks", 0);
        int infraCalendar = extractInt(infraEstimate, "calendarWeeks", 0);

        int feTotalWeeks = extractInt(feEstimate, "totalWeeks", 0);
        int beTotalWeeks = extractInt(beEstimate, "totalWeeks", 0);
        int infraTotalWeeks = extractInt(infraEstimate, "totalWeeks", 0);
        int totalEffort = feTotalWeeks + beTotalWeeks + infraTotalWeeks;

        int bufferWeeks = 2;
        int totalProjectWeeks = infraCalendar + Math.max(feCalendar, beCalendar) + bufferWeeks;

        System.out.println("  [pp_pm_timeline] Building timeline for " + projectName
                + ": " + totalProjectWeeks + " weeks total");

        List<Map<String, Object>> milestones = new ArrayList<>();

        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("name", "Infrastructure Ready");
        m1.put("week", infraCalendar);
        m1.put("description", "Cloud infrastructure provisioned and configured");
        milestones.add(m1);

        int devStartWeek = infraCalendar;

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("name", "Frontend Alpha");
        m2.put("week", devStartWeek + feCalendar);
        m2.put("description", "Frontend components developed and unit tested");
        milestones.add(m2);

        Map<String, Object> m3 = new LinkedHashMap<>();
        m3.put("name", "Backend Alpha");
        m3.put("week", devStartWeek + beCalendar);
        m3.put("description", "Backend services developed and unit tested");
        milestones.add(m3);

        Map<String, Object> m4 = new LinkedHashMap<>();
        m4.put("name", "Integration Testing");
        m4.put("week", devStartWeek + Math.max(feCalendar, beCalendar));
        m4.put("description", "End-to-end integration testing complete");
        milestones.add(m4);

        Map<String, Object> m5 = new LinkedHashMap<>();
        m5.put("name", "Production Launch");
        m5.put("week", totalProjectWeeks);
        m5.put("description", "Final deployment and go-live with buffer for stabilization");
        milestones.add(m5);

        String riskLevel;
        if (totalEffort > 40) {
            riskLevel = "high";
        } else if (totalEffort > 20) {
            riskLevel = "medium";
        } else {
            riskLevel = "low";
        }

        Map<String, Object> timeline = new LinkedHashMap<>();
        timeline.put("projectName", projectName);
        timeline.put("totalProjectWeeks", totalProjectWeeks);
        timeline.put("infraCalendarWeeks", infraCalendar);
        timeline.put("frontendCalendarWeeks", feCalendar);
        timeline.put("backendCalendarWeeks", beCalendar);
        timeline.put("bufferWeeks", bufferWeeks);
        timeline.put("totalEffortWeeks", totalEffort);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timeline", timeline);
        result.getOutputData().put("milestones", milestones);
        result.getOutputData().put("riskLevel", riskLevel);
        return result;
    }

    private int extractInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
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
