package multiagentcontent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Research agent — gathers facts and sources on a given topic for the target audience.
 * Returns 4 facts, 3 sources, and a topicRelevance score.
 */
public class ResearchAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cc_research_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "general topic";
        }

        String targetAudience = (String) task.getInputData().get("targetAudience");
        if (targetAudience == null || targetAudience.isBlank()) {
            targetAudience = "general audience";
        }

        System.out.println("  [cc_research_agent] Researching topic: " + topic + " for audience: " + targetAudience);

        List<String> facts = List.of(
                topic + " has seen a 40% increase in adoption over the past year",
                "Industry experts predict " + topic + " will transform " + targetAudience + " workflows by 2027",
                "Organizations investing in " + topic + " report 3x productivity gains",
                "The global market for " + topic + " solutions is projected to reach $50B by 2028"
        );

        List<String> sources = List.of(
                "Industry Research Quarterly, Vol. 12, 2025",
                "Global Technology Trends Report 2025",
                targetAudience + " Best Practices Handbook, 3rd Edition"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("facts", facts);
        result.getOutputData().put("sources", sources);
        result.getOutputData().put("topicRelevance", 0.92);
        return result;
    }
}
