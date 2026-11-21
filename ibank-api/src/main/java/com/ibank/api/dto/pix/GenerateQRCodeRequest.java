package com.ibank.api.dto.pix;

import com.ibank.core.util.Money;
import com.ibank.pix.model.QRCodeType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQRCodeRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    private UUID pixKeyId;

    @NotNull(message = "QR Code type is required")
    private QRCodeType qrCodeType;

    private Money amount;

    @Size(max = 140, message = "Description max 140 characters")
    private String description;

    @Min(value = 1, message = "Max usage must be at least 1")
    @Max(value = 1000, message = "Max usage cannot exceed 1000")
    private Integer maxUsage;
}