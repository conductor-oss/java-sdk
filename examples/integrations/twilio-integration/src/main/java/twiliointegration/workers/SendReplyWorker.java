package twiliointegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Sends a reply SMS.
 * Input: to, from, body
 * Output: messageSid, status
 *
 * Runs in live mode when TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN are set,
 * otherwise falls back to fallback mode.
 */
public class SendReplyWorker implements Worker {

    private final boolean liveMode;

    public SendReplyWorker() {
        String sid = System.getenv("TWILIO_ACCOUNT_SID");
        String token = System.getenv("TWILIO_AUTH_TOKEN");
        this.liveMode = sid != null && !sid.isBlank() && token != null && !token.isBlank();
        if (liveMode) {
            Twilio.init(sid, token);
        }
    }

    @Override
    public String getTaskDefName() {
        return "twl_send_reply";
    }

    @Override
    public TaskResult execute(Task task) {
        String to = (String) task.getInputData().get("to");
        String from = (String) task.getInputData().get("from");
        String body = (String) task.getInputData().get("body");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    body
            ).create();
            result.getOutputData().put("messageSid", message.getSid());
            result.getOutputData().put("status", message.getStatus().toString());
            System.out.println("  [reply] SMS " + message.getSid() + " to " + to + ": \"" + body + "\"");
        } else {
            String messageSid = "SM" + Long.toString(System.currentTimeMillis(), 36) + "r";
            System.out.println("  [reply] SMS " + messageSid + " to " + to + ": \"" + body + "\"");
            result.getOutputData().put("messageSid", messageSid);
            result.getOutputData().put("status", "delivered");
        }

        return result;
    }
}
