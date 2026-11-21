package com.ibank.api.mapper;

import com.ibank.account.model.Account;
import com.ibank.account.service.AccountService;
import com.ibank.api.dto.account.*;
import com.ibank.core.util.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    @Mappings({
        @Mapping(target = "balance", source = "balance"),
        @Mapping(target = "availableBalance", source = "availableBalance"),
        @Mapping(target = "blockedBalance", source = "blockedBalance"),
        @Mapping(target = "overdraftLimit", source = "overdraftLimit"),
        @Mapping(target = "overdraftUsed", source = "overdraftUsed"),
        @Mapping(target = "effectiveAvailableBalance", expression = "java(account.getEffectiveAvailableBalance())"),
        @Mapping(target = "dailyWithdrawalLimit", source = "dailyWithdrawalLimit"),
        @Mapping(target = "dailyTransferLimit", source = "dailyTransferLimit"),
        @Mapping(target = "monthlyTransferLimit", source = "monthlyTransferLimit"),
        @Mapping(target = "pixLimit", source = "pixLimit"),
        @Mapping(target = "maintenanceFee", source = "maintenanceFee")
    })
    AccountResponse toResponse(Account account);

    AccountSummaryResponse toSummaryResponse(Account account);

    AccountBalanceResponse toBalanceResponse(AccountService.AccountBalanceDTO dto);

    @Mappings({
        @Mapping(target = "holderName", source = "holderName"),
        @Mapping(target = "holderEmail", source = "holderEmail"),
        @Mapping(target = "holderPhone", source = "holderPhone"),
        @Mapping(target = "pixEnabled", source = "pixEnabled"),
        @Mapping(target = "debitCardEnabled", source = "debitCardEnabled"),
        @Mapping(target = "creditCardEnabled", source = "creditCardEnabled"),
        @Mapping(target = "dailyWithdrawalLimit", source = "dailyWithdrawalLimit"),
        @Mapping(target = "dailyTransferLimit", source = "dailyTransferLimit"),
        @Mapping(target = "monthlyTransferLimit", source = "monthlyTransferLimit"),
        @Mapping(target = "pixLimit", source = "pixLimit"),
        @Mapping(target = "overdraftLimit", source = "overdraftLimit")
    })
    void updateAccountFromRequest(UpdateAccountRequest request, @MappingTarget Account account);

    AccountService.OpenAccountCommand toOpenCommand(OpenAccountRequest request);

    AccountService.BlockAccountCommand toBlockCommand(BlockAccountRequest request);

    AccountService.CloseAccountCommand toCloseCommand(CloseAccountRequest request);

    AccountService.UpdateAccountCommand toUpdateCommand(UpdateAccountRequest request);
}