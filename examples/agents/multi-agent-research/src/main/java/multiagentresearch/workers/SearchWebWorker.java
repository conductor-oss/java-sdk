package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the web for relevant findings — takes queries and maxResults,
 * returns a list of findings with source, title, year, key point, and credibility.
 */
public class SearchWebWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_search_web";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> queries = (List<String>) task.getInputData().get("queries");
        if (queries == null || queries.isEmpty()) {
            queries = List.of("default web search");
        }

        System.out.println("  [ra_search_web] Searching web with " + queries.size() + " queries...");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("source", "techreview.com");
        finding1.put("title", "Transformative Impact of LLMs on Developer Productivity");
        finding1.put("year", 2024);
        finding1.put("keyPoint", "Studies show 40% productivity improvement in code generation tasks when using LLM-assisted tools.");
        finding1.put("credibility", 0.87);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("source", "devsurvey.org");
        finding2.put("title", "Annual Developer Tools Survey Results");
        finding2.put("year", 2024);
        finding2.put("keyPoint", "73% of professional developers now use AI-assisted coding tools in their daily workflow.");
        finding2.put("credibility", 0.92);

        Map<String, Object> finding3 = new LinkedHashMap<>();
        finding3.put("source", "softwareengineering.news");
        finding3.put("title", "Challenges in LLM-Generated Code Quality");
        finding3.put("year", 2025);
        finding3.put("keyPoint", "Code review rejection rates for LLM-generated code average 23%, compared to 18% for human-written code.");
        finding3.put("credibility", 0.78);

        List<Map<String, Object>> findings = List.of(finding1, finding2, finding3);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("searchEngine", "web");
        result.getOutputData().put("totalScanned", 156);
        return result;
    }
}
