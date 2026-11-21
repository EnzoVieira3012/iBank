package com.ibank.api.dto.pix;

import com.ibank.pix.model.PixKeyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixKeyLookupResponse {

    private boolean found;
    private String keyValue;
    private PixKeyType keyType;
    private String accountId;
    private String accountType;
    private String branch;
    private String accountNumber;
    private String holderName;
    private String holderCpf;
    private String ispb;
    private String error;
}