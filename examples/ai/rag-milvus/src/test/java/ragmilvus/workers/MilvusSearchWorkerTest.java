package ragmilvus.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MilvusSearchWorkerTest {

    private final MilvusSearchWorker worker = new MilvusSearchWorker();

    @Test
    void taskDefName() {
        assertEquals("milvus_search", worker.getTaskDefName());
    }

    @Test
    void returnsThreeResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.021, -0.034, 0.078),
                "collection", "tech_docs",
                "topK", 3,
                "metricType", "IP",
                "searchParams", new HashMap<>(Map.of("nprobe", 16))
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void firstResultHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, 0.2),
                "collection", "tech_docs",
                "topK", 3,
                "metricType", "IP",
                "searchParams", new HashMap<>(Map.of("nprobe", 16))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        Map<String, Object> first = results.get(0);

        assertEquals(100001, first.get("id"));
        assertEquals(0.96, first.get("distance"));
        assertEquals("Milvus Architecture", first.get("title"));
        assertEquals("Milvus is a cloud-native vector database built for scalable similarity search.",
                first.get("content"));
        assertEquals("arch.md", first.get("source"));
    }

    @Test
    void secondResultHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "collection", "tech_docs",
                "topK", 3,
                "metricType", "IP",
                "searchParams", new HashMap<>(Map.of("nprobe", 16))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        Map<String, Object> second = results.get(1);

        assertEquals(100042, second.get("id"));
        assertEquals(0.92, second.get("distance"));
        assertEquals("Index Types", second.get("title"));
        assertEquals("indexes.md", second.get("source"));
    }

    @Test
    void thirdResultHasCorrectFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "collection", "tech_docs",
                "topK", 3,
                "metricType", "IP",
                "searchParams", new HashMap<>(Map.of("nprobe", 16))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        Map<String, Object> third = results.get(2);

        assertEquals(100089, third.get("id"));
        assertEquals(0.87, third.get("distance"));
        assertEquals("Partitions", third.get("title"));
        assertEquals("partitions.md", third.get("source"));
    }

    @Test
    void returnsCollectionInfo() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "collection", "my_collection",
                "topK", 3,
                "metricType", "IP",
                "searchParams", new HashMap<>(Map.of("nprobe", 16))
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertNotNull(collectionInfo);
        assertEquals("my_collection", collectionInfo.get("name"));
        assertEquals(52000, collectionInfo.get("numEntities"));
        assertEquals("IVF_FLAT", collectionInfo.get("indexType"));
        assertEquals("IP", collectionInfo.get("metricType"));
        assertEquals(1536, collectionInfo.get("dim"));
    }

    @Test
    void handlesDefaultCollection() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> collectionInfo = (Map<String, Object>) result.getOutputData().get("collectionInfo");
        assertEquals("tech_docs", collectionInfo.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
