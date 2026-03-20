package streamprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WindowEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_window_events";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) events = List.of();

        Object windowSizeObj = task.getInputData().get("windowSizeMs");
        int windowSize = 5000;
        if (windowSizeObj instanceof Number) {
            windowSize = ((Number) windowSizeObj).intValue();
        }

        Map<Long, List<Map<String, Object>>> windowMap = new LinkedHashMap<>();
        for (Map<String, Object> e : events) {
            Object tsObj = e.get("ts");
            long ts = 0;
            if (tsObj instanceof Number) ts = ((Number) tsObj).longValue();
            long windowKey = (ts / windowSize) * windowSize;
            windowMap.computeIfAbsent(windowKey, k -> new ArrayList<>()).add(e);
        }

        List<Map<String, Object>> windowList = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : windowMap.entrySet()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("windowStart", entry.getKey());
            w.put("windowEnd", entry.getKey() + windowSize);
            w.put("events", entry.getValue());
            w.put("count", entry.getValue().size());
            windowList.add(w);
        }

        System.out.println("  [window] Grouped into " + windowList.size() + " windows (" + windowSize + "ms each)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("windows", windowList);
        result.getOutputData().put("windowCount", windowList.size());
        return result;
    }
}
