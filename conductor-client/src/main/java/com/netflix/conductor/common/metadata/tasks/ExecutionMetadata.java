package com.netflix.conductor.common.metadata.tasks;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionMetadata {

    private Long serverSendTime;

    private Long clientReceiveTime;

    private Long executionStartTime;

    private Long executionEndTime;

    private Long clientSendTime;

    private Long pollNetworkLatency;

    private Long updateNetworkLatency;

    // Additional context as Map for flexibility
    private Map<String, Object> additionalContext = new HashMap<>();
}