package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches internal databases for proprietary findings — takes queries and database names,
 * returns findings with source, title, year, key point, and credibility.
 */
public class SearchDatabasesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_search_databases";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> queries = (List<String>) task.getInputData().get("queries");
        if (queries == null || queries.isEmpty()) {
            queries = List.of("default database search");
        }
        List<String> databases = (List<String>) task.getInputData().get("databases");
        if (databases == null || databases.isEmpty()) {
            databases = List.of("default-db");
        }

        System.out.println("  [ra_search_databases] Searching " + databases.size() + " databases...");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("source", "internal-reports");
        finding1.put("title", "Q4 Engineering Productivity Metrics After LLM Tool Adoption");
        finding1.put("year", 2024);
        finding1.put("keyPoint", "Pull request cycle time decreased by 28% after introducing LLM-assisted code review.");
        finding1.put("credibility", 0.91);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("source", "market-data");
        finding2.put("title", "AI Developer Tools Market Forecast 2025-2030");
        finding2.put("year", 2025);
        finding2.put("keyPoint", "The AI developer tools market is projected to reach $14.1B by 2028, growing at 34% CAGR.");
        finding2.put("credibility", 0.85);

        Map<String, Object> finding3 = new LinkedHashMap<>();
        finding3.put("source", "patents");
        finding3.put("title", "Patent Landscape Analysis: AI-Assisted Software Development");
        finding3.put("year", 2024);
        finding3.put("keyPoint", "Patent filings for LLM-based code generation increased 312% year-over-year in 2024.");
        finding3.put("credibility", 0.88);

        List<Map<String, Object>> findings = List.of(finding1, finding2, finding3);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("searchEngine", "internal");
        result.getOutputData().put("totalScanned", 234);
        return result;
    }
}
