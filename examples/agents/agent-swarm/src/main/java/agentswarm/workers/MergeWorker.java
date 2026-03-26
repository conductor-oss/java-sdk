package agentswarm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merge worker — combines results from all 4 swarm agents into a unified
 * research report. Computes totalFindings, avgConfidence, and totalSources
 * across all swarm outputs, then assembles a report with title, sections,
 * and synthesis.
 */
public class MergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_merge";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "unspecified topic";
        }

        Map<String, Object> result1 = (Map<String, Object>) task.getInputData().get("result1");
        Map<String, Object> result2 = (Map<String, Object>) task.getInputData().get("result2");
        Map<String, Object> result3 = (Map<String, Object>) task.getInputData().get("result3");
        Map<String, Object> result4 = (Map<String, Object>) task.getInputData().get("result4");

        System.out.println("  [as_merge] Merging results from 4 swarm agents for topic: " + topic);

        List<Map<String, Object>> allResults = List.of(
                result1 != null ? result1 : Map.of(),
                result2 != null ? result2 : Map.of(),
                result3 != null ? result3 : Map.of(),
                result4 != null ? result4 : Map.of()
        );

        int totalFindings = 0;
        double totalConfidence = 0.0;
        int totalSources = 0;
        int resultCount = 0;
        List<Map<String, Object>> sections = new ArrayList<>();

        for (Map<String, Object> swarmResult : allResults) {
            List<String> findings = swarmResult.containsKey("findings")
                    ? (List<String>) swarmResult.get("findings")
                    : List.of();
            totalFindings += findings.size();

            Object confObj = swarmResult.get("confidence");
            if (confObj instanceof Number) {
                totalConfidence += ((Number) confObj).doubleValue();
                resultCount++;
            }

            Object srcObj = swarmResult.get("sourcesConsulted");
            if (srcObj instanceof Number) {
                totalSources += ((Number) srcObj).intValue();
            }

            String area = swarmResult.containsKey("area")
                    ? (String) swarmResult.get("area")
                    : "Unknown Area";

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("area", area);
            section.put("findingCount", findings.size());
            section.put("findings", findings);
            sections.add(section);
        }

        double avgConfidence = resultCount > 0
                ? Math.round(totalConfidence / resultCount * 100.0) / 100.0
                : 0.0;

        String synthesis = String.format(
                "Comprehensive analysis of '%s' yielded %d findings across %d research areas "
                        + "with an average confidence of %.2f, drawing from %d sources. "
                        + "The research covers market dynamics, technical architecture, "
                        + "practical applications, and future projections.",
                topic, totalFindings, sections.size(), avgConfidence, totalSources);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", "Research Report: " + topic);
        report.put("sections", sections);
        report.put("totalFindings", totalFindings);
        report.put("avgConfidence", avgConfidence);
        report.put("totalSources", totalSources);
        report.put("synthesis", synthesis);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
