package com.ibank.api.dto.pix;

import com.ibank.pix.model.PixKeyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePixKeyRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Key type is required")
    private PixKeyType keyType;

    @NotBlank(message = "Key value is required")
    private String keyValue;
}