package kafkaconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Receives a Kafka message and wraps it in a raw message envelope with headers and timestamp.
 */
public class ReceiveMessage implements Worker {

    @Override
    public String getTaskDefName() {
        return "kc_receive_message";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        Object partition = task.getInputData().get("partition");
        Object offset = task.getInputData().get("offset");
        String messageKey = (String) task.getInputData().get("messageKey");
        Object messageValue = task.getInputData().get("messageValue");

        System.out.println("[kc_receive_message] Received message from topic=" + topic
                + " partition=" + partition + " offset=" + offset);

        TaskResult result = new TaskResult(task);

        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("content-type", "application/json");
        headers.put("correlation-id", "corr-88221");

        Map<String, Object> rawMessage = new LinkedHashMap<>();
        rawMessage.put("key", messageKey);
        rawMessage.put("value", messageValue);
        rawMessage.put("headers", headers);
        rawMessage.put("timestamp", 1710900000000L);

        result.getOutputData().put("rawMessage", rawMessage);
        result.getOutputData().put("format", "json");

        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
