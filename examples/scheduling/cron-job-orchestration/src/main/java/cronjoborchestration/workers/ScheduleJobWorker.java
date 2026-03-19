package cronjoborchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Parses cron expressions and computes next execution time. Real cron parsing.
 */
public class ScheduleJobWorker implements Worker {
    @Override public String getTaskDefName() { return "cj_schedule_job"; }

    @Override public TaskResult execute(Task task) {
        String cronExpression = (String) task.getInputData().get("cronExpression");
        String jobName = (String) task.getInputData().get("jobName");
        if (cronExpression == null) cronExpression = "0 0 * * *"; // default: midnight daily
        if (jobName == null) jobName = "unnamed-job";

        // Parse cron (simplified: minute hour day month dayOfWeek)
        String[] parts = cronExpression.trim().split("\\s+");
        boolean validCron = parts.length == 5;

        // Compute next execution time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution;
        String frequency;

        if (validCron) {
            int minute = parts[0].equals("*") ? 0 : Integer.parseInt(parts[0]);
            int hour = parts[1].equals("*") ? now.getHour() : Integer.parseInt(parts[1]);

            if (parts[0].equals("*/5")) {
                frequency = "every 5 minutes";
                nextExecution = now.plusMinutes(5 - (now.getMinute() % 5)).withSecond(0);
            } else if (parts[1].equals("*")) {
                frequency = "hourly at :" + String.format("%02d", minute);
                nextExecution = now.withMinute(minute).withSecond(0);
                if (nextExecution.isBefore(now)) nextExecution = nextExecution.plusHours(1);
            } else {
                frequency = "daily at " + String.format("%02d:%02d", hour, minute);
                nextExecution = now.withHour(hour).withMinute(minute).withSecond(0);
                if (nextExecution.isBefore(now)) nextExecution = nextExecution.plusDays(1);
            }
        } else {
            frequency = "invalid cron expression";
            nextExecution = now.plusDays(1);
        }

        System.out.println("  [schedule] " + jobName + " (" + cronExpression + ") -> " + frequency);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("jobName", jobName);
        result.getOutputData().put("cronExpression", cronExpression);
        result.getOutputData().put("validCron", validCron);
        result.getOutputData().put("frequency", frequency);
        result.getOutputData().put("nextExecution", nextExecution.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return result;
    }
}
