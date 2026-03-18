package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MprSplitInputWorkerTest {

    private final MprSplitInputWorker worker = new MprSplitInputWorker();

    @Test
    void taskDefName() {
        assertEquals("mpr_split_input", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void splitsDocumentsIntoThreePartitions() {
        Task task = taskWith(Map.of("documents", List.of("doc1", "doc2", "doc3", "doc4", "doc5", "doc6")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<List<String>> partitions = (List<List<String>>) result.getOutputData().get("partitions");
        assertEquals(3, partitions.size());
        assertEquals(2, partitions.get(0).size());
        assertEquals(2, partitions.get(1).size());
        assertEquals(2, partitions.get(2).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesUnevenSplit() {
        Task task = taskWith(Map.of("documents", List.of("a", "b", "c", "d", "e")));
        TaskResult result = worker.execute(task);

        List<List<String>> partitions = (List<List<String>>) result.getOutputData().get("partitions");
        int total = partitions.get(0).size() + partitions.get(1).size() + partitions.get(2).size();
        assertEquals(5, total, "All documents should be distributed");
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesEmptyDocumentList() {
        Task task = taskWith(Map.of("documents", List.of()));
        TaskResult result = worker.execute(task);

        List<List<String>> partitions = (List<List<String>>) result.getOutputData().get("partitions");
        assertEquals(3, partitions.size());
        assertEquals(0, partitions.get(0).size());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("partitions"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
