package ragmongodb.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that demonstrates MongoDB Atlas $vectorSearch aggregation pipeline stage.
 *
 * In production this would run:
 *   db.collection.aggregate([
 *     { $vectorSearch: { index: "vector_index", path: "embedding",
 *       queryVector: [...], numCandidates: 100, limit: 3 } },
 *     { $project: { title: 1, content: 1, score: { $meta: "vectorSearchScore" } } }
 *   ]);
 */
public class MongoVectorSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mongo_vector_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String database = (String) task.getInputData().get("database");
        String collection = (String) task.getInputData().get("collection");
        Object numCandidatesObj = task.getInputData().get("numCandidates");
        int numCandidates = numCandidatesObj instanceof Number
                ? ((Number) numCandidatesObj).intValue() : 100;

        System.out.println("  [mongodb] Database: \"" + database + "\", Collection: \"" + collection + "\"");
        System.out.println("  [mongodb] $vectorSearch: index=\"vector_index\", path=\"embedding\", numCandidates=" + numCandidates);

        List<Map<String, Object>> documents = new ArrayList<>();

        Map<String, Object> doc1 = new LinkedHashMap<>();
        doc1.put("_id", "64f1a2b3c4d5e6f7a8b9c0d1");
        doc1.put("title", "Atlas Vector Search");
        doc1.put("content", "MongoDB Atlas Vector Search uses $vectorSearch aggregation pipeline stage.");
        doc1.put("score", 0.95);
        documents.add(doc1);

        Map<String, Object> doc2 = new LinkedHashMap<>();
        doc2.put("_id", "64f1a2b3c4d5e6f7a8b9c0d2");
        doc2.put("title", "Vector Indexes");
        doc2.put("content", "Create vector search indexes on fields containing embedding arrays.");
        doc2.put("score", 0.91);
        documents.add(doc2);

        Map<String, Object> doc3 = new LinkedHashMap<>();
        doc3.put("_id", "64f1a2b3c4d5e6f7a8b9c0d3");
        doc3.put("title", "Hybrid Queries");
        doc3.put("content", "Combine $vectorSearch with $match, $sort, and other aggregation stages.");
        doc3.put("score", 0.87);
        documents.add(doc3);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("index", "vector_index");
        meta.put("path", "embedding");
        meta.put("numCandidates", numCandidates);
        meta.put("totalDocs", 24500);

        System.out.println("  [mongodb] " + documents.size() + " documents matched from " + meta.get("totalDocs") + " total");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        result.getOutputData().put("meta", meta);
        return result;
    }
}
