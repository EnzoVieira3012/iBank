package com.ibank.antifraud.repository;

import com.ibank.antifraud.model.DeviceFingerprint;
import com.ibank.antifraud.model.DeviceTrustLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, UUID> {

    Optional<DeviceFingerprint> findByDeviceHash(String deviceHash);

    List<DeviceFingerprint> findByCustomerId(UUID customerId);

    List<DeviceFingerprint> findByCustomerIdAndTrustLevel(UUID customerId, DeviceTrustLevel trustLevel);

    List<DeviceFingerprint> findByTrustLevel(DeviceTrustLevel trustLevel);

    List<DeviceFingerprint> findByIpAddress(String ipAddress);

    List<DeviceFingerprint> findByLastSeenAtBefore(LocalDateTime date);

    @Query("SELECT d FROM DeviceFingerprint d WHERE d.customerId = :customerId ORDER BY d.lastSeenAt DESC")
    List<DeviceFingerprint> findRecentByCustomerId(@Param("customerId") UUID customerId);

    boolean existsByCustomerIdAndDeviceHash(UUID customerId, String deviceHash);

    long countByCustomerId(UUID customerId);

    @Query("SELECT d FROM DeviceFingerprint d WHERE d.deviceHash IN :hashes")
    List<DeviceFingerprint> findByDeviceHashes(@Param("hashes") List<String> hashes);
}