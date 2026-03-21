package systemprompts.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Compares the formal and casual LLM responses, reporting length differences
 * and an insight about how system prompts affect output tone.
 */
public class SpCompareOutputsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_compare_outputs";
    }

    @Override
    public TaskResult execute(Task task) {
        String formalResponse = (String) task.getInputData().get("formalResponse");
        String casualResponse = (String) task.getInputData().get("casualResponse");

        if (formalResponse == null) {
            formalResponse = "";
        }
        if (casualResponse == null) {
            casualResponse = "";
        }

        int formalLength = formalResponse.length();
        int casualLength = casualResponse.length();
        String longerStyle = formalLength >= casualLength ? "formal" : "casual";

        Map<String, Object> comparison = Map.of(
                "formalLength", formalLength,
                "casualLength", casualLength,
                "longerStyle", longerStyle,
                "insight", "System prompts dramatically change tone while preserving factual content"
        );

        System.out.println("  [sp_compare_outputs] Formal: " + formalLength + " chars, Casual: " + casualLength + " chars -> " + longerStyle + " is longer");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("comparison", comparison);
        return result;
    }
}
