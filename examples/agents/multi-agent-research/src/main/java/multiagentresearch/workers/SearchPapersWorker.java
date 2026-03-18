package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches academic papers for scholarly findings — takes queries and academic domains,
 * returns findings with citations, plus search engine type and total scanned count.
 */
public class SearchPapersWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_search_papers";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> queries = (List<String>) task.getInputData().get("queries");
        if (queries == null || queries.isEmpty()) {
            queries = List.of("default academic search");
        }
        List<String> academicDomains = (List<String>) task.getInputData().get("academicDomains");
        if (academicDomains == null || academicDomains.isEmpty()) {
            academicDomains = List.of("general");
        }

        System.out.println("  [ra_search_papers] Searching academic papers across " + academicDomains.size() + " domains...");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("source", "IEEE Transactions on Software Engineering");
        finding1.put("title", "Empirical Study of LLM-Assisted Code Generation in Enterprise Settings");
        finding1.put("year", 2024);
        finding1.put("keyPoint", "LLM-assisted development reduced time-to-market by 35% across 12 enterprise projects.");
        finding1.put("credibility", 0.95);
        finding1.put("citations", 142);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("source", "ACM Computing Surveys");
        finding2.put("title", "A Systematic Review of AI-Driven Software Testing Approaches");
        finding2.put("year", 2025);
        finding2.put("keyPoint", "AI-generated test suites achieve 89% branch coverage compared to 76% for manually written tests.");
        finding2.put("credibility", 0.93);
        finding2.put("citations", 87);

        List<Map<String, Object>> findings = List.of(finding1, finding2);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("searchEngine", "academic");
        result.getOutputData().put("totalScanned", 89);
        return result;
    }
}
