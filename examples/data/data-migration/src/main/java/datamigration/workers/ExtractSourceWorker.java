package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts records from the source database.
 * Input: sourceConfig, batchSize
 * Output: records, schema, recordCount
 */
public class ExtractSourceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mi_extract_source";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> config = (Map<String, Object>) task.getInputData().getOrDefault("sourceConfig", Map.of());
        String database = (String) config.getOrDefault("database", "legacy_db");

        // Record 4 has null name to perform invalid data
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new LinkedHashMap<>();
        r1.put("id", 1); r1.put("name", "Alice Johnson"); r1.put("email", "alice@old.com");
        r1.put("dept_id", 10); r1.put("hire_date", "2020-01-15");
        records.add(r1);

        Map<String, Object> r2 = new LinkedHashMap<>();
        r2.put("id", 2); r2.put("name", "Bob Smith"); r2.put("email", "bob@old.com");
        r2.put("dept_id", 20); r2.put("hire_date", "2019-06-01");
        records.add(r2);

        Map<String, Object> r3 = new LinkedHashMap<>();
        r3.put("id", 3); r3.put("name", "Carol Davis"); r3.put("email", "carol@old.com");
        r3.put("dept_id", 10); r3.put("hire_date", "2021-03-20");
        records.add(r3);

        Map<String, Object> r4 = new LinkedHashMap<>();
        r4.put("id", 4); r4.put("name", null); r4.put("email", "invalid");
        r4.put("dept_id", 30); r4.put("hire_date", "2022-11-10");
        records.add(r4);

        Map<String, Object> r5 = new LinkedHashMap<>();
        r5.put("id", 5); r5.put("name", "Eve Wilson"); r5.put("email", "eve@old.com");
        r5.put("dept_id", 20); r5.put("hire_date", "2023-07-05");
        records.add(r5);

        Map<String, Object> schema = Map.of("fields", List.of("id", "name", "email", "dept_id", "hire_date"));

        System.out.println("  [extract] Extracted " + records.size() + " records from " + database);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("schema", schema);
        result.getOutputData().put("recordCount", records.size());
        return result;
    }
}
