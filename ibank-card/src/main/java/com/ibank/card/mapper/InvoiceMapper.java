package com.ibank.card.mapper;

import com.ibank.card.controller.dto.InvoiceResponse;
import com.ibank.card.controller.dto.InvoiceInstallmentResponse;
import com.ibank.card.model.Invoice;
import com.ibank.card.model.InvoiceInstallment;
import com.ibank.card.model.InvoiceStatus;
import com.ibank.card.model.InstallmentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    InvoiceMapper INSTANCE = Mappers.getMapper(InvoiceMapper.class);

    @Mapping(target = "status", source = "status")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "status", source = "status")
    InvoiceInstallmentResponse toResponse(InvoiceInstallment installment);
}