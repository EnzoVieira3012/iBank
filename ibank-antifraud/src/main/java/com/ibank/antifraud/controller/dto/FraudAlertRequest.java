package com.ibank.antifraud.controller.dto;

import com.ibank.antifraud.model.AlertStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class FraudAlertRequest {

    private UUID assignedTo;
    private AlertStatus status;
    private String resolution;
    private String falsePositiveReason;
}