package com.ibank.antifraud.mapper;

import com.ibank.antifraud.controller.dto.DeviceFingerprintResponse;
import com.ibank.antifraud.model.DeviceFingerprint;
import com.ibank.antifraud.model.DeviceTrustLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface DeviceFingerprintMapper {

    DeviceFingerprintMapper INSTANCE = Mappers.getMapper(DeviceFingerprintMapper.class);

    @Mapping(target = "trustLevel", source = "trustLevel")
    DeviceFingerprintResponse toResponse(DeviceFingerprint device);
}