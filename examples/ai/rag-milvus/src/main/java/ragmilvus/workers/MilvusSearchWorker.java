package ragmilvus.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that searches a Milvus collection with an embedding vector.
 * Returns default results for deterministic testing.
 *
 * Real Milvus search would use:
 *   milvusClient.search({
 *     collection_name: "knowledge_docs",
 *     vector: embedding,
 *     output_fields: ["title", "content", "source"],
 *     limit: 3,
 *     metric_type: "IP",
 *     params: { nprobe: 16 },
 *     filter: 'category == "technical"',
 *   });
 */
public class MilvusSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "milvus_search";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object embeddingObj = task.getInputData().get("embedding");
        String collection = (String) task.getInputData().get("collection");
        Object topKObj = task.getInputData().get("topK");
        String metricType = (String) task.getInputData().get("metricType");
        Object searchParamsObj = task.getInputData().get("searchParams");

        if (collection == null || collection.isBlank()) {
            collection = "tech_docs";
        }
        if (metricType == null || metricType.isBlank()) {
            metricType = "IP";
        }
        int topK = 3;
        if (topKObj instanceof Number) {
            topK = ((Number) topKObj).intValue();
        }

        int nprobe = 16;
        if (searchParamsObj instanceof Map<?, ?> searchParams) {
            Object nprobeObj = searchParams.get("nprobe");
            if (nprobeObj instanceof Number) {
                nprobe = ((Number) nprobeObj).intValue();
            }
        }

        System.out.println("  [milvus_search worker] Collection: \"" + collection
                + "\", topK=" + topK + ", metric=" + metricType);
        System.out.println("  [milvus_search worker] Search params: nprobe=" + nprobe);

        // Build fixed results
        List<Map<String, Object>> results = new ArrayList<>();

        Map<String, Object> result1 = new HashMap<>();
        result1.put("id", 100001);
        result1.put("distance", 0.96);
        result1.put("title", "Milvus Architecture");
        result1.put("content", "Milvus is a cloud-native vector database built for scalable similarity search.");
        result1.put("source", "arch.md");
        results.add(result1);

        Map<String, Object> result2 = new HashMap<>();
        result2.put("id", 100042);
        result2.put("distance", 0.92);
        result2.put("title", "Index Types");
        result2.put("content", "Milvus supports IVF_FLAT, IVF_SQ8, HNSW, and ANNOY index types for different workloads.");
        result2.put("source", "indexes.md");
        results.add(result2);

        Map<String, Object> result3 = new HashMap<>();
        result3.put("id", 100089);
        result3.put("distance", 0.87);
        result3.put("title", "Partitions");
        result3.put("content", "Partitions divide collections for efficient filtered search and data management.");
        result3.put("source", "partitions.md");
        results.add(result3);

        // Build collection info
        Map<String, Object> collectionInfo = new HashMap<>();
        collectionInfo.put("name", collection);
        collectionInfo.put("numEntities", 52000);
        collectionInfo.put("indexType", "IVF_FLAT");
        collectionInfo.put("metricType", "IP");
        collectionInfo.put("dim", 1536);

        System.out.println("  [milvus_search worker] " + results.size()
                + " results from " + collectionInfo.get("numEntities") + " entities");

        TaskResult taskResult = new TaskResult(task);
        taskResult.setStatus(TaskResult.Status.COMPLETED);
        taskResult.getOutputData().put("results", results);
        taskResult.getOutputData().put("collectionInfo", collectionInfo);
        return taskResult;
    }
}
