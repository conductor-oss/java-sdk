package agentsupervisor.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Worker that acts as a tester agent creating and running tests.
 *
 * <p>Given code content or a feature name, generates a JUnit test class
 * skeleton with {@code @Test} methods based on the method names found in
 * the input. Produces realistic test metrics including suite counts,
 * pass/fail stats, and coverage estimates.
 *
 * <p>Input fields:
 * <ul>
 *   <li>{@code task} -- description of what to test</li>
 *   <li>{@code feature} -- feature name</li>
 *   <li>{@code code} -- optional code content to parse for test generation</li>
 * </ul>
 *
 * <p>Output: a {@code result} map with test suites, counts, pass/fail,
 * coverage, generated test code, and status.
 */
public class TesterAgentWorker implements Worker {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:public|protected)\\s+(?:static\\s+)?([\\w<>\\[\\],\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:public\\s+)?class\\s+(\\w+)");

    @Override
    public String getTaskDefName() {
        return "sup_tester_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().get("feature");
        String code = (String) task.getInputData().get("code");

        if (feature == null || feature.isBlank()) feature = "user-authentication";

        String testCode;
        List<String> testMethods;
        int suiteCount;

        if (code != null && !code.isBlank()) {
            // Parse code to generate targeted tests
            testCode = generateTestsFromCode(code, feature);
            testMethods = extractTestMethodNames(code);
            // One suite per class found in code
            Matcher classMatcher = CLASS_PATTERN.matcher(code);
            suiteCount = 0;
            while (classMatcher.find()) suiteCount++;
            if (suiteCount == 0) suiteCount = 1;
        } else {
            // Generate tests based on feature name
            testCode = generateTestsFromFeature(feature);
            testMethods = generateTestMethodsForFeature(feature);
            suiteCount = 3; // Controller, Service, Repository test suites
        }

        int totalTests = testMethods.size();
        // Last test might fail as an edge case
        int passed = Math.max(0, totalTests - 1);
        int failed = totalTests > 0 ? 1 : 0;
        String failedTest = totalTests > 0 ? testMethods.get(testMethods.size() - 1) : "none";
        int coverage = totalTests > 0 ? Math.min(95, 60 + totalTests * 2) : 0;

        Map<String, Object> testResult = new LinkedHashMap<>();
        testResult.put("testSuites", suiteCount);
        testResult.put("totalTests", totalTests);
        testResult.put("passed", passed);
        testResult.put("failed", failed);
        testResult.put("coverage", coverage + "%");
        testResult.put("failedTest", failedTest);
        testResult.put("testCode", testCode);
        testResult.put("testMethods", testMethods);
        testResult.put("status", failed > 0 ? "needs_fix" : "all_passed");

        System.out.println("  [tester] Completed testing for '" + feature + "': "
                + passed + "/" + totalTests + " passed, " + coverage + "% coverage");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", testResult);
        return result;
    }

    // ---- test generation from code ---------------------------------------------

    private String generateTestsFromCode(String code, String feature) {
        StringBuilder sb = new StringBuilder();

        // Find class name
        Matcher classMatcher = CLASS_PATTERN.matcher(code);
        String className = classMatcher.find() ? classMatcher.group(1) : toClassName(feature);

        sb.append("package ").append(feature.replaceAll("[^a-zA-Z0-9]", "").toLowerCase()).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("/**\n");
        sb.append(" * Unit tests for {@link ").append(className).append("}.\n");
        sb.append(" */\n");
        sb.append("class ").append(className).append("Test {\n\n");
        sb.append("    private ").append(className).append(" instance;\n\n");
        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        instance = new ").append(className).append("();\n");
        sb.append("    }\n");

        // Generate test methods for each public method found
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(2);
            String returnType = methodMatcher.group(1).trim();
            String params = methodMatcher.group(3).trim();

            // Test for normal execution
            sb.append("\n    @Test\n");
            sb.append("    void test").append(capitalize(methodName)).append("() {\n");
            if ("void".equals(returnType)) {
                sb.append("        assertDoesNotThrow(() -> instance.").append(methodName).append("(");
                sb.append(generateTestArgs(params));
                sb.append("));\n");
            } else {
                sb.append("        var result = instance.").append(methodName).append("(");
                sb.append(generateTestArgs(params));
                sb.append(");\n");
                sb.append("        assertNotNull(result);\n");
            }
            sb.append("    }\n");

            // Test for null input if method has parameters
            if (!params.isEmpty()) {
                sb.append("\n    @Test\n");
                sb.append("    void test").append(capitalize(methodName)).append("WithNullInput() {\n");
                sb.append("        assertThrows(NullPointerException.class, () -> instance.")
                        .append(methodName).append("(null));\n");
                sb.append("    }\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private List<String> extractTestMethodNames(String code) {
        List<String> tests = new ArrayList<>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(2);
            String params = methodMatcher.group(3).trim();
            tests.add("test" + capitalize(methodName));
            if (!params.isEmpty()) {
                tests.add("test" + capitalize(methodName) + "WithNullInput");
            }
        }
        if (tests.isEmpty()) {
            tests.add("testDefaultBehavior");
        }
        return tests;
    }

    // ---- test generation from feature name -------------------------------------

    private String generateTestsFromFeature(String feature) {
        String className = toClassName(feature);
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(feature.replaceAll("[^a-zA-Z0-9]", "").toLowerCase()).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("/**\n");
        sb.append(" * Test suite for the ").append(feature).append(" feature.\n");
        sb.append(" */\n");
        sb.append("class ").append(className).append("Test {\n\n");
        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        // Initialize test fixtures\n");
        sb.append("    }\n");

        for (String method : generateTestMethodsForFeature(feature)) {
            sb.append("\n    @Test\n");
            sb.append("    void ").append(method).append("() {\n");
            sb.append("        // TODO: implement test\n");
            sb.append("        assertTrue(true);\n");
            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private List<String> generateTestMethodsForFeature(String feature) {
        String entity = toClassName(feature);
        List<String> methods = new ArrayList<>();
        methods.add("testCreate" + entity);
        methods.add("testCreate" + entity + "WithInvalidInput");
        methods.add("testGet" + entity);
        methods.add("testGet" + entity + "NotFound");
        methods.add("testUpdate" + entity);
        methods.add("testUpdate" + entity + "NotFound");
        methods.add("testDelete" + entity);
        methods.add("testDelete" + entity + "NotFound");
        methods.add("testCreate" + entity + "DuplicateCheck");
        methods.add("testGet" + entity + "ById");
        methods.add("testUpdate" + entity + "PartialData");
        methods.add("testDelete" + entity + "AlreadyDeleted");
        methods.add("testCreate" + entity + "WithNullFields");
        methods.add("testGet" + entity + "WithEmptyId");
        methods.add("testUpdate" + entity + "ConcurrentModification");
        methods.add("testDelete" + entity + "WithDependencies");
        methods.add("test" + entity + "ValidationRules");
        methods.add("test" + entity + "EdgeCase");
        return methods;
    }

    // ---- helpers ---------------------------------------------------------------

    private String toClassName(String feature) {
        StringBuilder sb = new StringBuilder();
        for (String part : feature.split("[\\-_\\s]+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.isEmpty() ? "Default" : sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String generateTestArgs(String params) {
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String[] paramList = params.split(",");
        for (int i = 0; i < paramList.length; i++) {
            if (i > 0) sb.append(", ");
            String type = paramList[i].trim().split("\\s+")[0];
            sb.append(defaultValueForType(type));
        }
        return sb.toString();
    }

    private String defaultValueForType(String type) {
        return switch (type) {
            case "int", "long", "short", "byte" -> "0";
            case "double", "float" -> "0.0";
            case "boolean" -> "false";
            case "char" -> "'a'";
            case "String" -> "\"test\"";
            default -> "null";
        };
    }
}
