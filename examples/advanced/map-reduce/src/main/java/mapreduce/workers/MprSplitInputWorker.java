package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits input documents (list of text strings) into 3 partitions for parallel
 * map processing. Uses round-robin distribution to balance the load.
 *
 * Input: documents (list of strings)
 * Output: partitions (list of 3 lists of strings)
 */
public class MprSplitInputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mpr_split_input";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object docsObj = task.getInputData().get("documents");

        List<String> documents = new ArrayList<>();
        if (docsObj instanceof List) {
            for (Object item : (List<?>) docsObj) {
                documents.add(item != null ? item.toString() : "");
            }
        }

        // Split into 3 partitions using round-robin
        List<List<String>> partitions = new ArrayList<>();
        partitions.add(new ArrayList<>());
        partitions.add(new ArrayList<>());
        partitions.add(new ArrayList<>());

        for (int i = 0; i < documents.size(); i++) {
            partitions.get(i % 3).add(documents.get(i));
        }

        System.out.println("  [split] Split " + documents.size() + " documents into 3 partitions: "
                + partitions.get(0).size() + "/" + partitions.get(1).size() + "/" + partitions.get(2).size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("partitions", partitions);
        result.getOutputData().put("totalDocuments", documents.size());
        return result;
    }
}
