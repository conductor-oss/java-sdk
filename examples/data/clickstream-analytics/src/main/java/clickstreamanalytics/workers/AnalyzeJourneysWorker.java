package clickstreamanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes user journeys from session data.
 * Input: sessions
 * Output: analysis, topJourney, conversionRate
 */
public class AnalyzeJourneysWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ck_analyze_journeys";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) task.getInputData().get("sessions");
        if (sessions == null) {
            sessions = List.of();
        }

        List<String> journeys = new ArrayList<>();
        int converted = 0;
        int bounced = 0;
        for (Map<String, Object> s : sessions) {
            List<String> pages = (List<String>) s.get("pages");
            journeys.add(String.join(" -> ", pages));
            if (pages.contains("/checkout")) {
                converted++;
            }
            int clicks = ((Number) s.get("clicks")).intValue();
            if (clicks <= 1) {
                bounced++;
            }
        }

        String conversionRate = sessions.isEmpty() ? "0.0%"
                : String.format("%.1f%%", (converted * 100.0) / sessions.size());
        String topJourney = journeys.isEmpty() ? "none" : journeys.get(0);

        System.out.println("  [journeys] Analyzed " + journeys.size() + " journeys, conversion rate: " + conversionRate);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("journeys", journeys);
        analysis.put("converted", converted);
        analysis.put("bounced", bounced);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("topJourney", topJourney);
        result.getOutputData().put("conversionRate", conversionRate);
        return result;
    }
}
