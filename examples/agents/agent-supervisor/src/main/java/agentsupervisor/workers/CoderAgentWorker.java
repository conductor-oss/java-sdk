package agentsupervisor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that acts as a coder agent implementing a feature.
 *
 * <p>Given a feature description, generates a real Java class skeleton with
 * proper package, class name, method signatures, and boilerplate based on
 * the input. Produces Controller, Service, and Repository classes following
 * standard layered-architecture conventions.
 *
 * <p>Input fields:
 * <ul>
 *   <li>{@code task} -- description of what to implement</li>
 *   <li>{@code feature} -- feature name (e.g., "user-authentication")</li>
 * </ul>
 *
 * <p>Output: a {@code result} map containing generated file names, code
 * content, line count, language, status, and notes.
 */
public class CoderAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sup_coder_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String codingTask = (String) task.getInputData().get("task");
        String feature = (String) task.getInputData().get("feature");

        if (feature == null || feature.isBlank()) feature = "user-authentication";

        // Derive class name from feature: "user-authentication" -> "UserAuthentication"
        String className = toClassName(feature);
        String packageName = toPackageName(feature);

        // Generate three class skeletons
        String controllerCode = generateController(packageName, className, feature);
        String serviceCode = generateService(packageName, className, feature);
        String repositoryCode = generateRepository(packageName, className, feature);

        String controllerPath = "src/" + packageName + "/" + className + "Controller.java";
        String servicePath = "src/" + packageName + "/" + className + "Service.java";
        String repositoryPath = "src/" + packageName + "/" + className + "Repository.java";

        int totalLines = countLines(controllerCode) + countLines(serviceCode) + countLines(repositoryCode);

        Map<String, Object> codeResult = new LinkedHashMap<>();
        codeResult.put("filesCreated", List.of(controllerPath, servicePath, repositoryPath));
        codeResult.put("linesOfCode", totalLines);
        codeResult.put("language", "Java");
        codeResult.put("status", "implemented");
        codeResult.put("code", Map.of(
                controllerPath, controllerCode,
                servicePath, serviceCode,
                repositoryPath, repositoryCode
        ));
        codeResult.put("notes", "Implemented " + feature + " with controller endpoints, "
                + "service business logic, and repository data access layer");

        System.out.println("  [coder] Completed coding for '" + feature + "': 3 files, " + totalLines + " lines");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", codeResult);
        return result;
    }

    // ---- code generation -------------------------------------------------------

    private String generateController(String pkg, String className, String feature) {
        List<String> methods = deriveMethodNames(feature);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n");
        sb.append(" * REST controller for ").append(feature).append(" operations.\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append("Controller {\n\n");
        sb.append("    private final ").append(className).append("Service service;\n\n");
        sb.append("    public ").append(className).append("Controller(").append(className).append("Service service) {\n");
        sb.append("        this.service = service;\n");
        sb.append("    }\n");

        for (String method : methods) {
            sb.append("\n");
            sb.append("    /**\n");
            sb.append("     * Handles ").append(method).append(" request.\n");
            sb.append("     *\n");
            sb.append("     * @param request the incoming request data\n");
            sb.append("     * @return response map with operation result\n");
            sb.append("     */\n");
            sb.append("    public Map<String, Object> ").append(method).append("(Map<String, Object> request) {\n");
            sb.append("        return service.").append(method).append("(request);\n");
            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String generateService(String pkg, String className, String feature) {
        List<String> methods = deriveMethodNames(feature);
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import java.util.LinkedHashMap;\n");
        sb.append("import java.util.Map;\n\n");
        sb.append("/**\n");
        sb.append(" * Service layer for ").append(feature).append(" business logic.\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append("Service {\n\n");
        sb.append("    private final ").append(className).append("Repository repository;\n\n");
        sb.append("    public ").append(className).append("Service(").append(className).append("Repository repository) {\n");
        sb.append("        this.repository = repository;\n");
        sb.append("    }\n");

        for (String method : methods) {
            sb.append("\n");
            sb.append("    /**\n");
            sb.append("     * Business logic for ").append(method).append(".\n");
            sb.append("     *\n");
            sb.append("     * @param data input parameters\n");
            sb.append("     * @return result of the operation\n");
            sb.append("     */\n");
            sb.append("    public Map<String, Object> ").append(method).append("(Map<String, Object> data) {\n");
            sb.append("        // TODO: implement business logic\n");
            sb.append("        Map<String, Object> result = new LinkedHashMap<>();\n");
            sb.append("        result.put(\"status\", \"success\");\n");
            sb.append("        result.put(\"operation\", \"").append(method).append("\");\n");
            sb.append("        return result;\n");
            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String generateRepository(String pkg, String className, String feature) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.Optional;\n");
        sb.append("import java.util.concurrent.ConcurrentHashMap;\n\n");
        sb.append("/**\n");
        sb.append(" * Repository layer for ").append(feature).append(" data access.\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append("Repository {\n\n");
        sb.append("    private final ConcurrentHashMap<String, Map<String, Object>> store = new ConcurrentHashMap<>();\n\n");
        sb.append("    /**\n");
        sb.append("     * Saves an entity to the store.\n");
        sb.append("     *\n");
        sb.append("     * @param id the entity identifier\n");
        sb.append("     * @param entity the entity data\n");
        sb.append("     */\n");
        sb.append("    public void save(String id, Map<String, Object> entity) {\n");
        sb.append("        store.put(id, entity);\n");
        sb.append("    }\n\n");
        sb.append("    /**\n");
        sb.append("     * Finds an entity by its identifier.\n");
        sb.append("     *\n");
        sb.append("     * @param id the entity identifier\n");
        sb.append("     * @return an Optional containing the entity if found\n");
        sb.append("     */\n");
        sb.append("    public Optional<Map<String, Object>> findById(String id) {\n");
        sb.append("        return Optional.ofNullable(store.get(id));\n");
        sb.append("    }\n\n");
        sb.append("    /**\n");
        sb.append("     * Deletes an entity by its identifier.\n");
        sb.append("     *\n");
        sb.append("     * @param id the entity identifier\n");
        sb.append("     * @return true if the entity was removed\n");
        sb.append("     */\n");
        sb.append("    public boolean deleteById(String id) {\n");
        sb.append("        return store.remove(id) != null;\n");
        sb.append("    }\n\n");
        sb.append("    /**\n");
        sb.append("     * Returns the total number of stored entities.\n");
        sb.append("     *\n");
        sb.append("     * @return the count\n");
        sb.append("     */\n");
        sb.append("    public int count() {\n");
        sb.append("        return store.size();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---- helpers ---------------------------------------------------------------

    /**
     * Derives method names from the feature name. For example,
     * "user-authentication" produces create, get, update, delete operations
     * relevant to that domain.
     */
    private List<String> deriveMethodNames(String feature) {
        String entity = toCamelCase(feature);
        // First character lowercase for method names
        String methodEntity = Character.toLowerCase(entity.charAt(0)) + entity.substring(1);
        return List.of(
                "create" + entity,
                "get" + entity,
                "update" + entity,
                "delete" + entity
        );
    }

    /** "user-authentication" -> "UserAuthentication" */
    private String toClassName(String feature) {
        return toCamelCase(feature);
    }

    /** "user-authentication" -> "userauthentication" */
    private String toPackageName(String feature) {
        return feature.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    /** "user-authentication" -> "UserAuthentication" */
    private String toCamelCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (String part : input.split("[\\-_\\s]+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.isEmpty() ? "Default" : sb.toString();
    }

    private int countLines(String code) {
        return (int) code.lines().count();
    }
}
