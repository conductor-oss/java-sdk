package mistralai.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extracts the assistant's answer from a Mistral chat completion response.
 *
 * Input: chatResponse (the full API response)
 * Output: answer (the content string from choices[0].message.content)
 */
public class MistralExtractAnswerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mistral_extract_answer";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> chatResponse =
                (Map<String, Object>) task.getInputData().get("chatResponse");

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) chatResponse.get("choices");

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        String content = (String) message.get("content");

        System.out.println("  [mistral_extract_answer] Extracted answer (" + content.length() + " chars).");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", content);
        return result;
    }
}
