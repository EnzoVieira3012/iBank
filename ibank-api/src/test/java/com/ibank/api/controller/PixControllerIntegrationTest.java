package com.ibank.api.controller;

import com.ibank.api.dto.pix.*;
import com.ibank.core.model.AccountType;
import com.ibank.core.util.Money;
import com.ibank.pix.model.PixKeyType;
import com.ibank.pix.model.QRCodeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PixControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createPixKey_ShouldReturn201_WhenValidCpf() throws Exception {
        UUID accountId = createTestAccount();

        CreatePixKeyRequest request = CreatePixKeyRequest.builder()
            .accountId(accountId)
            .keyType(PixKeyType.CPF)
            .keyValue("123.456.789-00")
            .build();

        MvcResult result = mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.keyValue").value("123.456.789-00"))
            .andExpect(jsonPath("$.keyType").value("CPF"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("Created PIX key: " + response);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createPixKey_ShouldReturn201_WhenRandomKey() throws Exception {
        UUID accountId = createTestAccount();

        MvcResult result = mockMvc.perform(post("/api/v1/pix/keys/random")
                .param("accountId", accountId.toString())
                .header("Idempotency-Key", UUID.randomUUID().toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.keyType").value("RANDOM"))
            .andExpect(jsonPath("$.keyValue").exists())
            .andReturn();
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createPixKey_ShouldReturn400_WhenInvalidFormat() throws Exception {
        UUID accountId = createTestAccount();

        CreatePixKeyRequest request = CreatePixKeyRequest.builder()
            .accountId(accountId)
            .keyType(PixKeyType.CPF)
            .keyValue("123")  // Invalid CPF
            .build();

        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_KEY_FORMAT"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void sendPix_ShouldReturn201_WhenValid() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        // Create PIX key for payee
        String payeeKey = "555.666.777-88";
        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(payeeAccountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue(payeeKey)
                    .build())))
            .andExpect(status().isCreated());

        // Send PIX
        SendPixRequest request = SendPixRequest.builder()
            .payerAccountId(payerAccountId)
            .payerKeyValue("111.222.333-44")
            .payerKeyType("CPF")
            .payeeKeyValue(payeeKey)
            .amount(Money.of(100.00))
            .description("Teste PIX")
            .build();

        mockMvc.perform(post("/api/v1/pix/pay")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.amount").value("100.00"))
            .andExpect(jsonPath("$.endToEndId").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void generateQRCode_ShouldReturn201_WhenStatic() throws Exception {
        UUID accountId = createTestAccount();
        
        // Create PIX key first
        MvcResult keyResult = mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(accountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue("123.456.789-00")
                    .build())))
            .andExpect(status().isCreated())
            .andReturn();

        String keyResponse = keyResult.getResponse().getContentAsString();
        String keyId = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(keyResponse).get("id").asText();

        // Generate static QR Code
        GenerateQRCodeRequest request = GenerateQRCodeRequest.builder()
            .accountId(accountId)
            .pixKeyId(UUID.fromString(keyId))
            .qrCodeType(QRCodeType.STATIC)
            .build();

        mockMvc.perform(post("/api/v1/pix/qrcode")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.qrCodeType").value("STATIC"))
            .andExpect(jsonPath("$.emv").exists())
            .andExpect(jsonPath("$.payload").exists())
            .andExpect(jsonPath("$.imageBase64").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void generateQRCode_ShouldReturn201_WhenDynamic() throws Exception {
        UUID accountId = createTestAccount();

        GenerateQRCodeRequest request = GenerateQRCodeRequest.builder()
            .accountId(accountId)
            .qrCodeType(QRCodeType.DYNAMIC)
            .amount(Money.of(50.00))
            .description("Pagamento produto")
            .maxUsage(1)
            .build();

        mockMvc.perform(post("/api/v1/pix/qrcode")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.qrCodeType").value("DYNAMIC"))
            .andExpect(jsonPath("$.amount").value("50.00"))
            .andExpect(jsonPath("$.emv").exists())
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void payQRCode_ShouldReturn201_WhenValid() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        // Create dynamic QR Code for payee
        GenerateQRCodeRequest qrRequest = GenerateQRCodeRequest.builder()
            .accountId(payeeAccountId)
            .qrCodeType(QRCodeType.DYNAMIC)
            .amount(Money.of(75.00))
            .description("Pagamento via QR")
            .maxUsage(1)
            .build();

        MvcResult qrResult = mockMvc.perform(post("/api/v1/pix/qrcode")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(qrRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String qrResponse = qrResult.getResponse().getContentAsString();
        String emv = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(qrResponse).get("emv").asText();

        // Pay QR Code
        PayQRCodeRequest request = PayQRCodeRequest.builder()
            .payerAccountId(payerAccountId)
            .payerKeyValue("111.222.333-44")
            .payerKeyType("CPF")
            .emv(emv)
            .amount(Money.of(75.00))
            .description("Pagamento QR")
            .build();

        mockMvc.perform(post("/api/v1/pix/pay/qrcode")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.amount").value("75.00"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void refundPix_ShouldReturn201_WhenValid() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        // Create PIX key for payee
        String payeeKey = "555.666.777-88";
        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(payeeAccountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue(payeeKey)
                    .build())))
            .andExpect(status().isCreated());

        // Send PIX
        SendPixRequest sendRequest = SendPixRequest.builder()
            .payerAccountId(payerAccountId)
            .payerKeyValue("111.222.333-44")
            .payerKeyType("CPF")
            .payeeKeyValue(payeeKey)
            .amount(Money.of(200.00))
            .description("Pagamento para devolução")
            .build();

        MvcResult sendResult = mockMvc.perform(post("/api/v1/pix/pay")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(sendRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String sendResponse = sendResult.getResponse().getContentAsString();
        String endToEndId = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(sendResponse).get("endToEndId").asText();

        // Refund PIX
        RefundPixRequest refundRequest = RefundPixRequest.builder()
            .originalEndToEndId(endToEndId)
            .amount(Money.of(100.00)) // Partial refund
            .reason("Devolução parcial - produto defeituoso")
            .build();

        mockMvc.perform(post("/api/v1/pix/refund")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(refundRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.refundedAmount").value("100.00"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void dictLookup_ShouldReturnKeyInfo() throws Exception {
        UUID accountId = createTestAccount();
        
        // Create PIX key first
        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(accountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue("123.456.789-00")
                    .build())))
            .andExpect(status().isCreated());

        // Lookup in DICT
        mockMvc.perform(get("/api/v1/pix/dict/lookup")
                .param("keyValue", "123.456.789-00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.keyValue").value("123.456.789-00"))
            .andExpect(jsonPath("$.keyType").value("CPF"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void listKeys_ShouldReturnKeys() throws Exception {
        UUID accountId = createTestAccount();

        // Create multiple keys
        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(accountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue("123.456.789-00")
                    .build())))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(accountId)
                    .keyType(PixKeyType.EMAIL)
                    .keyValue("test@email.com")
                    .build())))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/pix/keys")
                .param("accountId", accountId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void blockUnblockKey_ShouldWork() throws Exception {
        UUID accountId = createTestAccount();

        MvcResult keyResult = mockMvc.perform(post("/api/v1/pix/keys")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreatePixKeyRequest.builder()
                    .accountId(accountId)
                    .keyType(PixKeyType.CPF)
                    .keyValue("123.456.789-00")
                    .build())))
            .andExpect(status().isCreated())
            .andReturn();

        String keyResponse = keyResult.getResponse().getContentAsString();
        String keyId = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(keyResponse).get("id").asText();

        // Block key
        mockMvc.perform(post("/api/v1/pix/keys/{keyId}/block", keyId)
                .param("reason", "Suspeita de fraude"))
            .andExpect(status().isNoContent());

        // Verify blocked
        mockMvc.perform(get("/api/v1/pix/keys/{keyId}", keyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"));

        // Unblock key
        mockMvc.perform(post("/api/v1/pix/keys/{keyId}/unblock", keyId))
            .andExpect(status().isNoContent());

        // Verify active
        mockMvc.perform(get("/api/v1/pix/keys/{keyId}", keyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private UUID createTestAccount() {
        return createTestAccount("999.888.777-66", AccountType.CURRENT);
    }

    private UUID createTestAccount(String cpf, AccountType type) {
        try {
            var request = com.ibank.api.dto.account.OpenAccountRequest.builder()
                .accountType(type)
                .holderName("Test User")
                .holderCpf(cpf)
                .holderEmail("test@email.com")
                .holderPhone("+5511999999999")
                .holderBirthDate(LocalDate.of(1990, 1, 1))
                .branch("0001")
                .initialDeposit(BigDecimal.valueOf(1000))
                .build();

            MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andReturn();

            String response = result.getResponse().getContentAsString();
            String idStr = com.fasterxml.jackson.databind.ObjectMapper
                .valueToTree(response).get("id").asText();
            return UUID.fromString(idStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}