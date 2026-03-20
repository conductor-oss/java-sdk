package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes a single log file and returns error/warning counts.
 *
 * Takes { logFile: {name, lineCount}, index } and returns deterministic results:
 * - errorCount = lineCount / 1000
 * - warningCount = lineCount / 250
 * - topError is deterministic based on index
 */
public class AnalyzeLogWorker implements Worker {

    private static final String[] TOP_ERRORS = {
            "NullPointerException in RequestHandler",
            "ConnectionTimeoutException in DbPool",
            "AuthenticationFailedException in TokenValidator",
            "OutOfMemoryError in CacheManager",
            "FileNotFoundException in ConfigLoader"
    };

    @Override
    public String getTaskDefName() {
        return "mr_analyze_log";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> logFile = (Map<String, Object>) task.getInputData().get("logFile");
        Object indexObj = task.getInputData().get("index");
        int index = indexObj instanceof Number ? ((Number) indexObj).intValue() : 0;

        String fileName = "unknown.log";
        int lineCount = 0;

        if (logFile != null) {
            Object nameObj = logFile.get("name");
            if (nameObj != null) {
                fileName = nameObj.toString();
            }
            Object lineCountObj = logFile.get("lineCount");
            if (lineCountObj instanceof Number) {
                lineCount = ((Number) lineCountObj).intValue();
            }
        }

        // Deterministic error and warning counts based on lineCount
        int errorCount = lineCount / 1000;
        int warningCount = lineCount / 250;
        String topError = TOP_ERRORS[index % TOP_ERRORS.length];

        System.out.println("  [mr_analyze_log] Analyzing " + fileName
                + " (" + lineCount + " lines): " + errorCount + " errors, " + warningCount + " warnings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fileName", fileName);
        result.getOutputData().put("errorCount", errorCount);
        result.getOutputData().put("warningCount", warningCount);
        result.getOutputData().put("lineCount", lineCount);
        result.getOutputData().put("topError", topError);
        return result;
    }
}
