package threeagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Researcher agent — gathers key facts, statistics, and sources on a subject.
 */
public class ResearcherAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "thr_researcher_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        if (subject == null || subject.isBlank()) {
            subject = "general technology trends";
        }

        System.out.println("  [researcher-agent] Researching: " + subject);

        Map<String, Object> statistics = Map.of(
                "marketGrowth", "15.3% CAGR through 2030",
                "regions", "North America leads adoption at 38% market share",
                "enterprises", "72% of Fortune 500 companies have adopted related solutions"
        );

        List<String> keyFacts = List.of(
                subject + " has seen rapid adoption across enterprise environments",
                "Industry analysts project continued double-digit growth over the next 5 years",
                "Integration with existing systems remains the top implementation challenge"
        );

        List<String> sources = List.of(
                "Industry Analysis Report 2025 — Global Market Insights",
                "Enterprise Technology Survey — Forrester Research",
                "Annual Technology Trends — Gartner Group"
        );

        Map<String, Object> research = Map.of(
                "subject", subject,
                "keyFacts", keyFacts,
                "statistics", statistics,
                "sources", sources
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("research", research);
        result.getOutputData().put("model", "researcher-agent-v1");
        return result;
    }
}
