package com.ibank.api.mapper;

import com.ibank.api.dto.pix.*;
import com.ibank.pix.model.PixKey;
import com.ibank.pix.model.PixTransaction;
import com.ibank.pix.model.QRCode;
import com.ibank.pix.service.PixKeyService;
import com.ibank.pix.service.PixTransactionService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PixMapper {

    PixMapper INSTANCE = Mappers.getMapper(PixMapper.class);

    PixKeyResponse toResponse(PixKey pixKey);

    PixTransactionResponse toResponse(PixTransaction transaction);

    QRCodeResponse toResponse(QRCode qrCode);

    PixKeyService.CreatePixKeyCommand toCreateCommand(CreatePixKeyRequest request);

    PixTransactionService.SendPixCommand toSendCommand(SendPixRequest request);

    PixTransactionService.PayQRCodeCommand toPayQRCommand(PayQRCodeRequest request);

    PixTransactionService.RefundPixCommand toRefundCommand(RefundPixRequest request);

    PixKeyLookupResponse toLookupResponse(com.ibank.pix.client.MockDictClient.DictKeyInfo info);

    @Mapping(target = "found", constant = "false")
    @Mapping(target = "error", source = "error")
    PixKeyLookupResponse toLookupResponse(com.ibank.pix.client.MockDictClient.DictLookupResult result);
}