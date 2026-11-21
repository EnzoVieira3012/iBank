package com.ibank.api.mapper;

import com.ibank.api.dto.transfer.*;
import com.ibank.transfer.model.Transfer;
import com.ibank.transfer.service.TransferService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    TransferMapper INSTANCE = Mappers.getMapper(TransferMapper.class);

    TransferResponse toResponse(Transfer transfer);

    TransferService.CreateInternalTransferCommand toInternalCommand(CreateInternalTransferRequest request);

    TransferService.CreateTEDTransferCommand toTEDCommand(CreateTEDTransferRequest request);

    TransferService.ScheduleTransferCommand toScheduleCommand(ScheduleTransferRequest request);

    TransferService.CancelTransferCommand toCancelCommand(CancelTransferRequest request);
}