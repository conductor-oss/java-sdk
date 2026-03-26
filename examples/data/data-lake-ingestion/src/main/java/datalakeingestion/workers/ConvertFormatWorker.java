package datalakeingestion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvertFormatWorker implements Worker {
    @Override public String getTaskDefName() { return "li_convert_format"; }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partitions = (List<Map<String, Object>>) task.getInputData().get("partitions");
        if (partitions == null) partitions = List.of();
        String format = (String) task.getInputData().get("targetFormat");
        if (format == null || format.isEmpty()) format = "parquet";

        List<Map<String, Object>> files = new ArrayList<>();
        for (Map<String, Object> p : partitions) {
            int count = p.get("count") instanceof Number ? ((Number) p.get("count")).intValue() : 0;
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("path", p.get("path") + "/data." + format);
            file.put("format", format);
            file.put("recordCount", count);
            file.put("sizeBytes", count * 256);
            files.add(file);
        }

        System.out.println("  [convert] Converted " + files.size() + " partitions to " + format.toUpperCase() + " format");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("files", files);
        return result;
    }
}
