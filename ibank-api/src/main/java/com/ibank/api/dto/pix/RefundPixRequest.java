package com.ibank.api.dto.pix;

import com.ibank.core.util.Money;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPixRequest {

    @NotBlank(message = "Original end-to-end ID is required")
    private String originalEndToEndId;

    private Money amount;

    @NotBlank(message = "Reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}