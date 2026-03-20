package twiliointegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes an SMS response.
 * Input: responseBody, originalMessage
 * Output: replyMessage, intent
 */
public class ProcessResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "twl_process_response";
    }

    @Override
    public TaskResult execute(Task task) {
        String responseBody = (String) task.getInputData().get("responseBody");
        String replyMessage = "YES".equals(responseBody)
                ? "Thank you for confirming! Your appointment is set."
                : "We understand. Please call us to reschedule.";
        String intent = "YES".equals(responseBody) ? "confirm" : "decline";
        System.out.println("  [process] Response \"" + responseBody + "\" -> reply: \"" + replyMessage + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("replyMessage", replyMessage);
        result.getOutputData().put("intent", intent);
        return result;
    }
}
