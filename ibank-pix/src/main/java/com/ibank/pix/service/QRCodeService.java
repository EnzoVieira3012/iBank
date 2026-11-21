package com.ibank.pix.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.ibank.core.util.Money;
import com.ibank.pix.model.QRCode;
import com.ibank.pix.model.QRCodeType;
import com.ibank.pix.repository.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {

    private final QRCodeRepository qrCodeRepository;

    @Value("${app.pix.qrcode-expiration-seconds:3600}")
    private long qrCodeExpirationSeconds;

    @Value("${app.pix.ispb:12345678}")
    private String ispb;

    public QRCode generateStaticQRCode(UUID accountId, UUID pixKeyId, String holderName, 
                                        String holderCpf, String holderAccountType,
                                        String branch, String accountNumber) {
        String payload = buildStaticPayload(holderName, holderCpf, holderAccountType, 
                                             branch, accountNumber);
        String emv = buildEMV(payload);

        QRCode qrCode = QRCode.builder()
            .emv(emv)
            .payload(payload)
            .qrCodeType(QRCodeType.STATIC)
            .accountId(accountId)
            .pixKeyId(pixKeyId)
            .maxUsage(Integer.MAX_VALUE)
            .expiresAt(OffsetDateTime.now().plusYears(10))
            .imageBase64(generateQRCodeImage(emv))
            .build();

        return qrCodeRepository.save(qrCode);
    }

    public QRCode generateDynamicQRCode(UUID accountId, UUID pixKeyId, Money amount, 
                                         String description, int maxUsage) {
        String txId = UUID.randomUUID().toString().replace("-", "").substring(0, 25).toUpperCase();
        String payload = buildDynamicPayload(txId, amount, description);
        String emv = buildEMV(payload);

        QRCode qrCode = QRCode.builder()
            .emv(emv)
            .payload(payload)
            .qrCodeType(QRCodeType.DYNAMIC)
            .amount(amount)
            .description(description)
            .accountId(accountId)
            .pixKeyId(pixKeyId)
            .maxUsage(maxUsage)
            .expiresAt(OffsetDateTime.now().plusSeconds(qrCodeExpirationSeconds))
            .imageBase64(generateQRCodeImage(emv))
            .build();

        return qrCodeRepository.save(qrCode);
    }

    public Optional<QRCode> getById(UUID qrCodeId) {
        return qrCodeRepository.findById(qrCodeId);
    }

    public Optional<QRCode> getByEmv(String emv) {
        return qrCodeRepository.findByEmv(emv);
    }

    public Page<QRCode> listByAccount(UUID accountId, Pageable pageable) {
        return qrCodeRepository.findByAccountId(accountId, pageable);
    }

    private String buildStaticPayload(String holderName, String holderCpf, 
                                       String holderAccountType, String branch, String accountNumber) {
        StringBuilder payload = new StringBuilder();
        
        payload.append("000201");
        payload.append("26");
        appendTLV(payload, "00", "BR.GOV.BCB.PIX");
        appendTLV(payload, "01", ispb);
        appendTLV(payload, "02", sanitize(holderName, 25));
        appendTLV(payload, "03", sanitize(holderCpf.replaceAll("[^0-9]", ""), 14));
        appendTLV(payload, "04", sanitize(branch, 8));
        appendTLV(payload, "05", sanitize(accountNumber, 15));
        appendTLV(payload, "06", holderAccountType);
        closeTLV(payload, "26");
        
        payload.append("52040000");
        payload.append("5303986");
        payload.append("5802BR");
        payload.append("59").append(String.format("%02d", holderName.length())).append(sanitize(holderName, 25));
        payload.append("60").append(String.format("%02d", "SAO PAULO".length())).append("SAO PAULO");
        payload.append("62");
        appendTLV(payload, "05", "***");
        closeTLV(payload, "62");
        
        String crcPayload = payload + "6304";
        String crc = calculateCRC16(crcPayload);
        return crcPayload + crc;
    }

    private String buildDynamicPayload(String txId, Money amount, String description) {
        StringBuilder payload = new StringBuilder();
        
        payload.append("000201");
        payload.append("26");
        appendTLV(payload, "00", "BR.GOV.BCB.PIX");
        appendTLV(payload, "01", ispb);
        appendTLV(payload, "25", txId);
        closeTLV(payload, "26");
        
        payload.append("52040000");
        payload.append("5303986");
        payload.append("54").append(String.format("%02d", amount.toPlainString().length())).append(amount.toPlainString());
        payload.append("5802BR");
        payload.append("59").append(String.format("%02d", "iBank".length())).append("iBank");
        payload.append("60").append(String.format("%02d", "SAO PAULO".length())).append("SAO PAULO");
        payload.append("62");
        appendTLV(payload, "05", sanitize(description, 25));
        closeTLV(payload, "62");
        
        String crcPayload = payload + "6304";
        String crc = calculateCRC16(crcPayload);
        return crcPayload + crc;
    }

    private void appendTLV(StringBuilder sb, String tag, String value) {
        if (value == null || value.isEmpty()) return;
        sb.append(tag).append(String.format("%02d", value.length())).append(value);
    }

    private void closeTLV(StringBuilder sb, String tag) {
    }

    private String buildEMV(String payload) {
        return payload;
    }

    private String calculateCRC16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes()) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private String generateQRCodeImage(String payload) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate QR code image", e);
            return null;
        }
    }
}