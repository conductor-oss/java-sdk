package cicdpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Performs a real security scan of the build directory. Scans files for
 * common security issues:
 *   - Hardcoded secrets (passwords, API keys, tokens in source files)
 *   - Sensitive file patterns (.env, credentials, private keys)
 *   - Known insecure patterns (HTTP URLs where HTTPS should be used)
 *
 * Input:
 *   - buildId (String, required): build identifier
 *   - buildDir (String, required): path to source code to scan
 *
 * Output:
 *   - vulnerabilities (int): total findings count
 *   - critical (int): critical severity findings
 *   - high (int): high severity findings
 *   - medium (int): medium severity findings
 *   - low (int): low severity findings
 *   - findings (List): detailed vulnerability descriptions
 *   - durationMs (long): scan duration
 *   - blockDeploy (boolean): true if critical > 0
 */
public class SecurityScan implements Worker {

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|secret|api[_-]?key|token|credential)\\s*[=:]\\s*[\"']?[A-Za-z0-9_+/=\\-]{8,}",
            Pattern.MULTILINE
    );

    private static final Pattern INSECURE_HTTP_PATTERN = Pattern.compile(
            "http://(?!localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0|\\[::1\\])",
            Pattern.MULTILINE
    );

    private static final String[] SENSITIVE_FILENAMES = {
            ".env", ".env.local", ".env.production", "credentials.json",
            "id_rsa", "id_ed25519", ".pem", "secret", "keystore.jks"
    };

    @Override
    public String getTaskDefName() {
        return "cicd_security_scan";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String buildId = getRequiredString(task, "buildId");
        if (buildId == null || buildId.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: buildId");
            return result;
        }

        String buildDir = getRequiredString(task, "buildDir");

        System.out.println("[cicd_security_scan] Running security scan for build " + buildId);

        Map<String, Object> output = new LinkedHashMap<>();

        long startMs = System.currentTimeMillis();
        List<Map<String, Object>> findings = new ArrayList<>();
        int critical = 0, high = 0, medium = 0, low = 0;

        // Scan source files if build directory is available
        if (buildDir != null && !buildDir.isBlank() && Files.exists(Path.of(buildDir))) {
            Path dir = Path.of(buildDir);
            try (Stream<Path> walk = Files.walk(dir, 10)) {
                List<Path> files = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> !p.toString().contains("/.git/"))
                        .filter(p -> !p.toString().contains("/node_modules/"))
                        .filter(p -> !p.toString().contains("/target/"))
                        .collect(Collectors.toList());

                for (Path file : files) {
                    String filename = file.getFileName().toString();

                    // Check for sensitive file names
                    for (String sensitive : SENSITIVE_FILENAMES) {
                        if (filename.equals(sensitive) || filename.endsWith(sensitive)) {
                            high++;
                            findings.add(Map.of(
                                    "severity", "HIGH",
                                    "type", "sensitive-file",
                                    "file", file.toString(),
                                    "detail", "Sensitive file found: " + filename
                            ));
                        }
                    }

                    // Scan text files for secret patterns
                    if (isTextFile(filename) && Files.size(file) < 1_000_000) {
                        String content = Files.readString(file);

                        if (SECRET_PATTERN.matcher(content).find()) {
                            critical++;
                            findings.add(Map.of(
                                    "severity", "CRITICAL",
                                    "type", "hardcoded-secret",
                                    "file", file.toString(),
                                    "detail", "Potential hardcoded secret detected"
                            ));
                        }

                        if (INSECURE_HTTP_PATTERN.matcher(content).find()) {
                            low++;
                            findings.add(Map.of(
                                    "severity", "LOW",
                                    "type", "insecure-http",
                                    "file", file.toString(),
                                    "detail", "Insecure HTTP URL found (should use HTTPS)"
                            ));
                        }
                    }
                }

                System.out.println("  Scanned " + files.size() + " files in " + buildDir);
            } catch (Exception e) {
                System.out.println("  File scan error: " + e.getMessage());
            }
        }

        // System-level security checks
        try {
            Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
            if (Files.exists(sshDir)) {
                ProcessBuilder pb = new ProcessBuilder("find", sshDir.toString(), "-name", "id_*", "-not", "-name", "*.pub", "-perm", "+044");
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String findOutput;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    findOutput = reader.lines().collect(Collectors.joining("\n")).trim();
                }
                proc.waitFor(5, TimeUnit.SECONDS);

                if (!findOutput.isEmpty()) {
                    medium++;
                    findings.add(Map.of(
                            "severity", "MEDIUM",
                            "type", "ssh-key-permissions",
                            "detail", "SSH private key(s) with loose permissions found"
                    ));
                }
            }
        } catch (Exception e) {
            // Skip if find command not available or fails
        }

        long durationMs = System.currentTimeMillis() - startMs;

        int totalVulnerabilities = critical + high + medium + low;
        boolean blockDeploy = critical > 0;

        System.out.println("  Findings: " + totalVulnerabilities
                + " (critical=" + critical + " high=" + high
                + " medium=" + medium + " low=" + low + ")"
                + (blockDeploy ? " BLOCKING DEPLOY" : ""));

        output.put("vulnerabilities", totalVulnerabilities);
        output.put("critical", critical);
        output.put("high", high);
        output.put("medium", medium);
        output.put("low", low);
        output.put("findings", findings);
        output.put("durationMs", durationMs);
        output.put("buildId", buildId);
        output.put("blockDeploy", blockDeploy);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private boolean isTextFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js")
                || lower.endsWith(".ts") || lower.endsWith(".go") || lower.endsWith(".rb")
                || lower.endsWith(".rs") || lower.endsWith(".xml") || lower.endsWith(".yml")
                || lower.endsWith(".yaml") || lower.endsWith(".json") || lower.endsWith(".properties")
                || lower.endsWith(".cfg") || lower.endsWith(".conf") || lower.endsWith(".txt")
                || lower.endsWith(".md") || lower.endsWith(".sh") || lower.endsWith(".bash")
                || lower.endsWith(".env") || lower.endsWith(".toml") || lower.endsWith(".ini")
                || lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".sql")
                || lower.endsWith(".gradle") || lower.endsWith(".kts") || lower.endsWith(".kt");
    }

    private String getRequiredString(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value == null) return null;
        return value.toString();
    }
}
