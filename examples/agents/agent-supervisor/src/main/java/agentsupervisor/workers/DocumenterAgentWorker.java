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
 * Worker that acts as a documenter agent creating Javadoc documentation.
 *
 * <p>Given code content or a feature name, generates documentation including:
 * <ul>
 *   <li>Javadoc with {@code @param}, {@code @return}, and class descriptions
 *       based on parsing the input code</li>
 *   <li>API reference, setup guide, and examples documents</li>
 *   <li>Structured sections covering the feature comprehensively</li>
 * </ul>
 *
 * <p>Input fields:
 * <ul>
 *   <li>{@code task} -- description of what to document</li>
 *   <li>{@code feature} -- feature name</li>
 *   <li>{@code code} -- optional code content to parse for documentation</li>
 * </ul>
 *
 * <p>Output: a {@code result} map with documents created, sections, word
 * count, generated documentation text, and status.
 */
public class DocumenterAgentWorker implements Worker {

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:public\\s+)?class\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?:public|protected|private)\\s+(?:static\\s+)?([\\w<>\\[\\],\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)");

    @Override
    public String getTaskDefName() {
        return "sup_documenter_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().get("feature");
        String code = (String) task.getInputData().get("code");

        if (feature == null || feature.isBlank()) feature = "user-authentication";

        // Generate documentation based on code content if available, otherwise from feature name
        String documentation;
        List<String> sections;
        if (code != null && !code.isBlank()) {
            documentation = generateDocFromCode(code, feature);
            sections = extractSectionsFromCode(code, feature);
        } else {
            documentation = generateDocFromFeature(feature);
            sections = generateSectionsForFeature(feature);
        }

        int wordCount = countWords(documentation);

        Map<String, Object> docResult = new LinkedHashMap<>();
        docResult.put("documentsCreated", List.of(
                "API_REFERENCE.md",
                "SETUP_GUIDE.md",
                "EXAMPLES.md"
        ));
        docResult.put("sections", sections);
        docResult.put("wordCount", wordCount);
        docResult.put("documentation", documentation);
        docResult.put("status", "complete");

        System.out.println("  [documenter] Completed documentation for '" + feature
                + "': 3 documents, " + wordCount + " words");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", docResult);
        return result;
    }

    // ---- documentation generation from code ------------------------------------

    private String generateDocFromCode(String code, String feature) {
        StringBuilder doc = new StringBuilder();

        // Extract class names
        Matcher classMatcher = CLASS_PATTERN.matcher(code);
        List<String> classes = new ArrayList<>();
        while (classMatcher.find()) {
            classes.add(classMatcher.group(1));
        }

        // Extract methods
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        List<MethodInfo> methods = new ArrayList<>();
        while (methodMatcher.find()) {
            methods.add(new MethodInfo(
                    methodMatcher.group(2),
                    methodMatcher.group(1).trim(),
                    methodMatcher.group(3).trim()
            ));
        }

        doc.append("# ").append(formatFeatureName(feature)).append(" Documentation\n\n");
        doc.append("## Overview\n\n");
        doc.append("This module implements the ").append(feature)
                .append(" feature. It consists of ").append(classes.size())
                .append(" class(es) with ").append(methods.size()).append(" public method(s).\n\n");

        // Generate class documentation
        for (String cls : classes) {
            doc.append("## Class: ").append(cls).append("\n\n");
            doc.append("/**\n");
            doc.append(" * ").append(cls).append(" provides functionality for ").append(feature).append(".\n");
            doc.append(" *\n");
            doc.append(" * @see ").append(feature).append("\n");
            doc.append(" */\n\n");
        }

        // Generate method documentation
        if (!methods.isEmpty()) {
            doc.append("## Methods\n\n");
            for (MethodInfo mi : methods) {
                doc.append("### ").append(mi.name).append("\n\n");
                doc.append("/**\n");
                doc.append(" * ").append(describeMethod(mi.name)).append("\n");
                doc.append(" *\n");
                if (!mi.params.isEmpty()) {
                    for (String param : parseParams(mi.params)) {
                        doc.append(" * @param ").append(param).append(" the ").append(param).append(" parameter\n");
                    }
                }
                if (!"void".equals(mi.returnType)) {
                    doc.append(" * @return ").append(mi.returnType).append(" the result\n");
                }
                doc.append(" */\n\n");
            }
        }

        doc.append("## Configuration\n\n");
        doc.append("Configure the ").append(feature).append(" module using environment variables or properties file.\n\n");
        doc.append("## Troubleshooting\n\n");
        doc.append("Common issues and solutions for the ").append(feature).append(" module.\n");

        return doc.toString();
    }

    private List<String> extractSectionsFromCode(String code, String feature) {
        List<String> sections = new ArrayList<>();
        sections.add("Overview");

        Matcher classMatcher = CLASS_PATTERN.matcher(code);
        while (classMatcher.find()) {
            sections.add("Class: " + classMatcher.group(1));
        }

        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        if (methodMatcher.find()) {
            sections.add("Methods");
        }

        sections.add("Configuration");
        sections.add("Troubleshooting");
        return sections;
    }

    // ---- documentation generation from feature name ----------------------------

    private String generateDocFromFeature(String feature) {
        String formatted = formatFeatureName(feature);
        StringBuilder doc = new StringBuilder();

        doc.append("# ").append(formatted).append(" Documentation\n\n");
        doc.append("## Overview\n\n");
        doc.append("The ").append(formatted).append(" feature provides a complete solution for ")
                .append(feature.replace("-", " ")).append(" operations in the application.\n\n");

        doc.append("## ").append(formatted).append(" Flow\n\n");
        doc.append("1. Client sends request to the ").append(formatted).append(" endpoint\n");
        doc.append("2. Controller validates and routes the request\n");
        doc.append("3. Service layer applies business logic\n");
        doc.append("4. Repository layer handles data persistence\n");
        doc.append("5. Response is returned to the client\n\n");

        doc.append("## API Endpoints\n\n");
        doc.append("| Method | Path | Description |\n");
        doc.append("|--------|------|-------------|\n");
        doc.append("| POST | /api/").append(feature).append(" | Create a new ").append(feature.replace("-", " ")).append(" |\n");
        doc.append("| GET | /api/").append(feature).append("/{id} | Retrieve by ID |\n");
        doc.append("| PUT | /api/").append(feature).append("/{id} | Update by ID |\n");
        doc.append("| DELETE | /api/").append(feature).append("/{id} | Delete by ID |\n\n");

        doc.append("## Configuration\n\n");
        doc.append("Set the following environment variables:\n\n");
        doc.append("```properties\n");
        doc.append(feature).append(".enabled=true\n");
        doc.append(feature).append(".timeout=30000\n");
        doc.append("```\n\n");

        doc.append("## Troubleshooting\n\n");
        doc.append("- Verify that the service is properly configured\n");
        doc.append("- Check logs for detailed error messages\n");
        doc.append("- Ensure database connectivity\n");

        return doc.toString();
    }

    private List<String> generateSectionsForFeature(String feature) {
        String formatted = formatFeatureName(feature);
        return List.of(
                "Overview",
                formatted + " Flow",
                "API Endpoints",
                "Configuration",
                "Troubleshooting"
        );
    }

    // ---- helpers ---------------------------------------------------------------

    private String formatFeatureName(String feature) {
        StringBuilder sb = new StringBuilder();
        for (String part : feature.split("[\\-_\\s]+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String describeMethod(String methodName) {
        if (methodName.startsWith("get")) return "Retrieves " + methodName.substring(3) + " data.";
        if (methodName.startsWith("set")) return "Sets " + methodName.substring(3) + " value.";
        if (methodName.startsWith("create")) return "Creates a new " + methodName.substring(6) + " instance.";
        if (methodName.startsWith("update")) return "Updates an existing " + methodName.substring(6) + ".";
        if (methodName.startsWith("delete")) return "Deletes the specified " + methodName.substring(6) + ".";
        if (methodName.startsWith("find")) return "Finds " + methodName.substring(4) + " by criteria.";
        if (methodName.startsWith("is") || methodName.startsWith("has")) return "Checks " + methodName + " condition.";
        return "Performs the " + methodName + " operation.";
    }

    private List<String> parseParams(String paramString) {
        List<String> params = new ArrayList<>();
        if (paramString.isEmpty()) return params;
        for (String param : paramString.split(",")) {
            String[] parts = param.trim().split("\\s+");
            if (parts.length >= 2) {
                params.add(parts[parts.length - 1]);
            }
        }
        return params;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.split("\\s+").length;
    }

    private record MethodInfo(String name, String returnType, String params) {}
}
