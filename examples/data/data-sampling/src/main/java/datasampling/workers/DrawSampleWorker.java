package datasampling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws a deterministic sample from the dataset.
 * Input: records (list), sampleRate (double 0-1)
 * Output: sample (list of sampled records), sampleSize (int)
 *
 * Sampling logic: computes step = floor(1/sampleRate), then takes every Nth record
 * (indices 0, step, 2*step, ...). If sampleRate >= 1.0, returns all records.
 * If sampleRate <= 0, returns empty sample.
 */
public class DrawSampleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_draw_sample";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        double sampleRate = toDouble(task.getInputData().get("sampleRate"), 1.0);

        System.out.println("  [sm_draw_sample] Drawing sample at rate " + sampleRate + " from " + records.size() + " records");

        List<Map<String, Object>> sample = new ArrayList<>();

        if (sampleRate > 0 && !records.isEmpty()) {
            if (sampleRate >= 1.0) {
                sample.addAll(records);
            } else {
                int step = (int) Math.floor(1.0 / sampleRate);
                if (step < 1) step = 1;
                for (int i = 0; i < records.size(); i += step) {
                    sample.add(records.get(i));
                }
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sample", sample);
        result.getOutputData().put("sampleSize", sample.size());
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
