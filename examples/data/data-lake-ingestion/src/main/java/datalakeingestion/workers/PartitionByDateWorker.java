package datalakeingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PartitionByDateWorker implements Worker {
    @Override public String getTaskDefName() { return "li_partition_by_date"; }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) records = List.of();
        String lakePath = (String) task.getInputData().get("lakePath");
        if (lakePath == null) lakePath = "s3://data-lake";

        Map<String, List<Map<String, Object>>> partitions = new LinkedHashMap<>();
        for (Map<String, Object> r : records) {
            String eventDate = String.valueOf(r.get("event_date"));
            String date = eventDate.substring(0, 10);
            partitions.computeIfAbsent(date, k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> partitionList = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : partitions.entrySet()) {
            String date = entry.getKey();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("path", lakePath + "/year=" + date.substring(0, 4) + "/month=" + date.substring(5, 7) + "/day=" + date.substring(8, 10));
            p.put("date", date);
            p.put("records", entry.getValue());
            p.put("count", entry.getValue().size());
            partitionList.add(p);
        }

        System.out.println("  [partition] Created " + partitionList.size() + " date partitions");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("partitions", partitionList);
        result.getOutputData().put("partitionCount", partitionList.size());
        return result;
    }
}
