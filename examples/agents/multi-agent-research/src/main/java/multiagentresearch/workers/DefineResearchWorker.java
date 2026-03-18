package multiagentresearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Defines the research scope — takes a topic and depth, produces search queries,
 * target domains, and database names to guide the parallel search agents.
 */
public class DefineResearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ra_define_research";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general research topic";
        }
        String depth = (String) task.getInputData().get("depth");
        if (depth == null || depth.isBlank()) {
            depth = "standard";
        }

        System.out.println("  [ra_define_research] Defining research scope for: " + topic);

        List<String> searchQueries = List.of(
                topic + " recent advances",
                topic + " industry applications",
                topic + " challenges and limitations"
        );

        List<String> domains = List.of("computer science", "engineering", "business");

        List<String> databases = List.of("internal-reports", "market-data", "patents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchQueries", searchQueries);
        result.getOutputData().put("domains", domains);
        result.getOutputData().put("databases", databases);
        result.getOutputData().put("scope", "comprehensive");
        return result;
    }
}
