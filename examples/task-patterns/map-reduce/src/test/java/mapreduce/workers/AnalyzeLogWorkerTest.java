package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeLogWorkerTest {

    private final AnalyzeLogWorker worker = new AnalyzeLogWorker();

    @Test
    void taskDefName() {
        assertEquals("mr_analyze_log", worker.getTaskDefName());
    }

    @Test
    void analyzesDeterministicErrorCounts() {
        Task task = taskWith(Map.of(
                "logFile", Map.of("name", "api-server.log", "lineCount", 50000),
                "index", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("api-server.log", result.getOutputData().get("fileName"));
        // errorCount = 50000 / 1000 = 50
        assertEquals(50, result.getOutputData().get("errorCount"));
        // warningCount = 50000 / 250 = 200
        assertEquals(200, result.getOutputData().get("warningCount"));
        assertEquals(50000, result.getOutputData().get("lineCount"));
        assertEquals("NullPointerException in RequestHandler", result.getOutputData().get("topError"));
    }

    @Test
    void analyzesDifferentLogFile() {
        Task task = taskWith(Map.of(
                "logFile", Map.of("name", "payment-gateway.log", "lineCount", 75000),
                "index", 2
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("payment-gateway.log", result.getOutputData().get("fileName"));
        // errorCount = 75000 / 1000 = 75
        assertEquals(75, result.getOutputData().get("errorCount"));
        // warningCount = 75000 / 250 = 300
        assertEquals(300, result.getOutputData().get("warningCount"));
        assertEquals(75000, result.getOutputData().get("lineCount"));
        // index 2 % 5 = 2 -> "AuthenticationFailedException in TokenValidator"
        assertEquals("AuthenticationFailedException in TokenValidator", result.getOutputData().get("topError"));
    }

    @Test
    void topErrorCyclesThroughOptions() {
        // index 0 -> NullPointerException
        TaskResult r0 = worker.execute(taskWith(Map.of(
                "logFile", Map.of("name", "a.log", "lineCount", 1000), "index", 0)));
        assertEquals("NullPointerException in RequestHandler", r0.getOutputData().get("topError"));

        // index 1 -> ConnectionTimeoutException
        TaskResult r1 = worker.execute(taskWith(Map.of(
                "logFile", Map.of("name", "b.log", "lineCount", 1000), "index", 1)));
        assertEquals("ConnectionTimeoutException in DbPool", r1.getOutputData().get("topError"));

        // index 3 -> OutOfMemoryError
        TaskResult r3 = worker.execute(taskWith(Map.of(
                "logFile", Map.of("name", "d.log", "lineCount", 1000), "index", 3)));
        assertEquals("OutOfMemoryError in CacheManager", r3.getOutputData().get("topError"));

        // index 4 -> FileNotFoundException
        TaskResult r4 = worker.execute(taskWith(Map.of(
                "logFile", Map.of("name", "e.log", "lineCount", 1000), "index", 4)));
        assertEquals("FileNotFoundException in ConfigLoader", r4.getOutputData().get("topError"));

        // index 5 wraps around -> NullPointerException
        TaskResult r5 = worker.execute(taskWith(Map.of(
                "logFile", Map.of("name", "f.log", "lineCount", 1000), "index", 5)));
        assertEquals("NullPointerException in RequestHandler", r5.getOutputData().get("topError"));
    }

    @Test
    void outputIsDeterministic() {
        Task task1 = taskWith(Map.of(
                "logFile", Map.of("name", "test.log", "lineCount", 40000), "index", 1));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of(
                "logFile", Map.of("name", "test.log", "lineCount", 40000), "index", 1));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("errorCount"), result2.getOutputData().get("errorCount"));
        assertEquals(result1.getOutputData().get("warningCount"), result2.getOutputData().get("warningCount"));
        assertEquals(result1.getOutputData().get("topError"), result2.getOutputData().get("topError"));
    }

    @Test
    void handlesNullLogFile() {
        Task task = taskWith(Map.of("index", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown.log", result.getOutputData().get("fileName"));
        assertEquals(0, result.getOutputData().get("errorCount"));
        assertEquals(0, result.getOutputData().get("warningCount"));
        assertEquals(0, result.getOutputData().get("lineCount"));
    }

    @Test
    void handlesMissingIndex() {
        Task task = taskWith(Map.of(
                "logFile", Map.of("name", "test.log", "lineCount", 10000)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // index defaults to 0
        assertEquals(10, result.getOutputData().get("errorCount"));
        assertEquals(40, result.getOutputData().get("warningCount"));
        assertEquals("NullPointerException in RequestHandler", result.getOutputData().get("topError"));
    }

    @Test
    void handlesSmallLineCount() {
        Task task = taskWith(Map.of(
                "logFile", Map.of("name", "tiny.log", "lineCount", 100),
                "index", 0
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // errorCount = 100 / 1000 = 0 (integer division)
        assertEquals(0, result.getOutputData().get("errorCount"));
        // warningCount = 100 / 250 = 0
        assertEquals(0, result.getOutputData().get("warningCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
