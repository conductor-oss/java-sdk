package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synthesizes findings from all three search agents — computes total sources,
 * average credibility, produces a synthesis map, key insights, and confidence score.
 */
public class SynthesizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_synthesize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "research topic";
        }

        List<Map<String, Object>> webFindings = (List<Map<String, Object>>) task.getInputData().get("webFindings");
        List<Map<String, Object>> paperFindings = (List<Map<String, Object>>) task.getInputData().get("paperFindings");
        List<Map<String, Object>> dbFindings = (List<Map<String, Object>>) task.getInputData().get("dbFindings");

        if (webFindings == null) webFindings = List.of();
        if (paperFindings == null) paperFindings = List.of();
        if (dbFindings == null) dbFindings = List.of();

        System.out.println("  [ra_synthesize] Synthesizing findings for: " + topic);

        int totalSources = webFindings.size() + paperFindings.size() + dbFindings.size();

        double avgCredibility = computeAverageCredibility(webFindings, paperFindings, dbFindings);

        Map<String, Object> synthesis = new LinkedHashMap<>();
        synthesis.put("topic", topic);
        synthesis.put("webSourceCount", webFindings.size());
        synthesis.put("paperSourceCount", paperFindings.size());
        synthesis.put("dbSourceCount", dbFindings.size());
        synthesis.put("avgCredibility", avgCredibility);
        synthesis.put("convergenceLevel", avgCredibility >= 0.85 ? "high" : avgCredibility >= 0.70 ? "moderate" : "low");

        List<String> keyInsights = List.of(
                "LLM-assisted development tools show consistent 35-40% productivity gains across multiple studies.",
                "Code quality remains a concern, with LLM-generated code showing higher review rejection rates.",
                "Enterprise adoption is accelerating, with 73% of developers now using AI-assisted coding tools.",
                "The market for AI developer tools is projected to grow at 34% CAGR through 2028."
        );

        double confidence = totalSources > 0 ? avgCredibility : 0.5;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("synthesis", synthesis);
        result.getOutputData().put("keyInsights", keyInsights);
        result.getOutputData().put("totalSources", totalSources);
        result.getOutputData().put("confidence", confidence);
        return result;
    }

    /**
     * Computes the average credibility across all findings from the three search sources.
     * Returns 0.0 if no findings have credibility values.
     */
    double computeAverageCredibility(List<Map<String, Object>> webFindings,
                                     List<Map<String, Object>> paperFindings,
                                     List<Map<String, Object>> dbFindings) {
        double totalCredibility = 0.0;
        int count = 0;

        for (List<Map<String, Object>> findings : List.of(webFindings, paperFindings, dbFindings)) {
            for (Map<String, Object> finding : findings) {
                Object cred = finding.get("credibility");
                if (cred instanceof Number) {
                    totalCredibility += ((Number) cred).doubleValue();
                    count++;
                }
            }
        }

        return count > 0 ? totalCredibility / count : 0.0;
    }
}
