package com.example;

import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class Main {
    public static void main(String[] args) {
        TaskResult taskResult = new TaskResult();
        for(int i = 0; i < 10; i++){
            taskResult.log("log " + i);
        }
        System.out.println("TaskResult created and logged successfully! Logs so far: " + taskResult.getLogs().size());

        for(var log : taskResult.getLogs()){
            System.out.println(" * " + log.getLog());
        }
    }
}