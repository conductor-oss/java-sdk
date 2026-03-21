package threeagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Writer agent — produces a draft article incorporating research facts for the target audience.
 */
public class WriterAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "thr_writer_agent";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> research = (Map<String, Object>) task.getInputData().get("research");
        String audience = (String) task.getInputData().get("audience");
        if (audience == null || audience.isBlank()) {
            audience = "general readers";
        }

        String subject = research != null ? (String) research.get("subject") : "the topic";
        List<String> keyFacts = research != null ? (List<String>) research.get("keyFacts") : List.of();
        Map<String, Object> statistics = research != null
                ? (Map<String, Object>) research.get("statistics") : Map.of();

        System.out.println("  [writer-agent] Writing draft for audience: " + audience);

        String marketGrowth = statistics.getOrDefault("marketGrowth", "significant growth").toString();

        StringBuilder draft = new StringBuilder();
        draft.append("Understanding ").append(subject).append(": A Comprehensive Overview for ")
                .append(audience).append(". ");
        draft.append("The market is experiencing ").append(marketGrowth).append(". ");
        for (String fact : keyFacts) {
            draft.append(fact).append(". ");
        }
        draft.append("Organizations should evaluate their readiness and develop strategic adoption plans ");
        draft.append("to remain competitive in this evolving landscape.");

        String draftText = draft.toString();
        int wordCount = draftText.split("\\s+").length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("draft", draftText);
        result.getOutputData().put("wordCount", wordCount);
        result.getOutputData().put("model", "writer-agent-v1");
        return result;
    }
}
