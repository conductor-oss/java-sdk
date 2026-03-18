package incidentresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Gathers real system diagnostics using OS commands and JVM MXBeans.
 * Collects:
 *   - Disk usage via real FileStore queries
 *   - Memory usage via JVM MemoryMXBean and system commands (free/vm_stat)
 *   - CPU load via OperatingSystemMXBean
 *   - Process list via real "ps" command
 *   - Uptime via real "uptime" command
 *   - Network connections via real "netstat"/"ss" command
 *
 * Input:
 *   - service (String): service name for context
 *
 * Output:
 *   - diskUsage (Map): filesystem usage details
 *   - memoryUsage (Map): heap and system memory
 *   - cpuLoad (double): system CPU load average
 *   - topProcesses (String): top resource-consuming processes
 *   - uptime (String): system uptime
 *   - networkConnections (int): active network connections count
 *   - service (String): the service being diagnosed
 */
public class GatherDiagnosticsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_gather_diagnostics";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = task.getInputData().get("service") != null
                ? String.valueOf(task.getInputData().get("service")) : "unknown";

        System.out.println("[ir_gather_diagnostics] Gathering diagnostics for service: " + service);

        TaskResult result = new TaskResult(task);

        // 1. Disk usage via FileStore
        List<Map<String, Object>> diskUsage = new ArrayList<>();
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                long totalSpace = store.getTotalSpace();
                long usableSpace = store.getUsableSpace();
                long usedSpace = totalSpace - usableSpace;
                double usedPct = totalSpace > 0 ? (double) usedSpace / totalSpace * 100.0 : 0.0;

                if (totalSpace > 0) {
                    Map<String, Object> disk = new LinkedHashMap<>();
                    disk.put("name", store.name());
                    disk.put("type", store.type());
                    disk.put("totalBytes", totalSpace);
                    disk.put("usedBytes", usedSpace);
                    disk.put("availableBytes", usableSpace);
                    disk.put("usedPercent", Math.round(usedPct * 10.0) / 10.0);
                    diskUsage.add(disk);
                }
            }
            System.out.println("  Disk: " + diskUsage.size() + " filesystem(s) queried");
        } catch (Exception e) {
            System.out.println("  Disk query error: " + e.getMessage());
        }

        // 2. Memory usage via JVM MXBeans
        Map<String, Object> memoryUsage = new LinkedHashMap<>();
        try {
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memBean.getHeapMemoryUsage().getUsed();
            long heapMax = memBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed();

            memoryUsage.put("heapUsedBytes", heapUsed);
            memoryUsage.put("heapMaxBytes", heapMax);
            memoryUsage.put("heapUsedPercent", heapMax > 0 ? Math.round((double) heapUsed / heapMax * 1000.0) / 10.0 : 0);
            memoryUsage.put("nonHeapUsedBytes", nonHeapUsed);

            // Try system memory via command
            String memOutput = runCommand(getMemoryCommand());
            if (!memOutput.isBlank()) {
                memoryUsage.put("systemMemory", memOutput.trim());
            }

            System.out.println("  Memory: heap " + (heapUsed / 1024 / 1024) + "MB/"
                    + (heapMax / 1024 / 1024) + "MB");
        } catch (Exception e) {
            memoryUsage.put("error", e.getMessage());
        }

        // 3. CPU load
        double cpuLoad = -1;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            cpuLoad = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            memoryUsage.put("availableProcessors", processors);
            System.out.println("  CPU: load average " + String.format("%.2f", cpuLoad)
                    + " (" + processors + " cores)");
        } catch (Exception e) {
            System.out.println("  CPU query error: " + e.getMessage());
        }

        // 4. Top processes via real "ps" command
        String topProcesses = "";
        try {
            topProcesses = runCommand(new String[]{"ps", "aux", "--sort=-pcpu"});
            if (topProcesses.isEmpty()) {
                // macOS variant
                topProcesses = runCommand(new String[]{"ps", "aux", "-r"});
            }
            // Truncate to first 15 lines
            String[] lines = topProcesses.split("\n");
            if (lines.length > 15) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 15; i++) {
                    sb.append(lines[i]).append("\n");
                }
                topProcesses = sb.toString();
            }
            System.out.println("  Processes: captured top process list");
        } catch (Exception e) {
            System.out.println("  Process list error: " + e.getMessage());
        }

        // 5. Uptime via real "uptime" command
        String uptime = "";
        try {
            uptime = runCommand(new String[]{"uptime"}).trim();
            System.out.println("  Uptime: " + uptime);
        } catch (Exception e) {
            System.out.println("  Uptime error: " + e.getMessage());
        }

        // 6. Network connections count
        int networkConnections = 0;
        try {
            String netOutput;
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac") || os.contains("darwin")) {
                netOutput = runCommand(new String[]{"netstat", "-an"});
            } else {
                netOutput = runCommand(new String[]{"ss", "-s"});
            }
            if (!netOutput.isBlank()) {
                networkConnections = (int) netOutput.lines()
                        .filter(l -> l.contains("ESTABLISHED") || l.contains("estab"))
                        .count();
            }
            System.out.println("  Network: " + networkConnections + " established connections");
        } catch (Exception e) {
            System.out.println("  Network query error: " + e.getMessage());
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("diskUsage", diskUsage);
        result.addOutputData("memoryUsage", memoryUsage);
        result.addOutputData("cpuLoad", cpuLoad);
        result.addOutputData("topProcesses", topProcesses);
        result.addOutputData("uptime", uptime);
        result.addOutputData("networkConnections", networkConnections);
        result.addOutputData("service", service);

        return result;
    }

    private String[] getMemoryCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return new String[]{"vm_stat"};
        } else {
            return new String[]{"free", "-h"};
        }
    }

    private String runCommand(String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            proc.waitFor(10, TimeUnit.SECONDS);
            return output;
        } catch (Exception e) {
            return "";
        }
    }
}
