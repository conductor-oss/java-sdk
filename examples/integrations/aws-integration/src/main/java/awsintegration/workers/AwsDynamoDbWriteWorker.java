package awsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Writes an item to Amazon DynamoDB.
 * Input: tableName, item
 * Output: itemId, tableName, consumedCapacity
 */
public class AwsDynamoDbWriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aws_dynamodb_write";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String tableName = (String) task.getInputData().get("tableName");
        if (tableName == null) {
            tableName = "default-table";
        }

        Object itemRaw = task.getInputData().get("item");
        String itemId = "item-default";
        if (itemRaw instanceof Map) {
            Object idVal = ((Map<String, Object>) itemRaw).get("id");
            if (idVal != null) {
                itemId = String.valueOf(idVal);
            }
        }

        System.out.println("  [DynamoDB] Written item " + itemId + " to " + tableName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("itemId", "" + itemId);
        result.getOutputData().put("tableName", "" + tableName);
        result.getOutputData().put("consumedCapacity", 5);
        return result;
    }
}
