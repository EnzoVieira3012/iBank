package com.ibank.api.controller;

import com.ibank.api.dto.transfer.*;
import com.ibank.core.model.AccountType;
import com.ibank.core.util.Money;
import com.ibank.transfer.model.TransferType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TransferControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createInternalTransfer_ShouldReturn201_WhenValid() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        CreateInternalTransferRequest request = CreateInternalTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .amount(Money.of(100.00))
            .description("Teste transferência interna")
            .feeAmount(Money.ZERO)
            .build();

        MvcResult result = mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferType").value("INTERNAL"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.amount").value("100.00"))
            .andExpect(jsonPath("$.endToEndId").exists())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("Internal transfer: " + response);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createInternalTransfer_ShouldReturn409_WhenDuplicateIdempotency() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);
        String idempotencyKey = UUID.randomUUID().toString();

        CreateInternalTransferRequest request = CreateInternalTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .amount(Money.of(50.00))
            .description("Teste 1")
            .build();

        // First request
        mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated());

        // Second request with same idempotency key
        mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void createTEDTransfer_ShouldReturn201_WhenBeforeCutoff() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);

        CreateTEDTransferRequest request = CreateTEDTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeName("Maria Santos")
            .payeeCpf("987.654.321-00")
            .payeeBranch("0001")
            .payeeAccountNumber("87654321-0")
            .payeeBankIspb("12345678")
            .payeeBankName("Banco do Brasil")
            .amount(Money.of(250.00))
            .description("Pagamento fornecedor")
            .feeAmount(Money.of(8.50))
            .build();

        mockMvc.perform(post("/api/v1/transfers/ted")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferType").value("TED"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.amount").value("250.00"))
            .andExpect(jsonPath("$.feeAmount").value("8.50"))
            .andExpect(jsonPath("$.payeeBankName").value("Banco do Brasil"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void scheduleTransfer_ShouldReturn201_WhenFutureDate() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        ScheduleTransferRequest request = ScheduleTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .payeeName("Maria Santos")
            .payeeCpf("555.666.777-88")
            .payeeBranch("0001")
            .payeeAccountNumber("87654321-0")
            .transferType(TransferType.INTERNAL)
            .amount(Money.of(300.00))
            .description("Pagamento agendado")
            .scheduledAt(OffsetDateTime.now().plusDays(2))
            .build();

        mockMvc.perform(post("/api/v1/transfers/schedule")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferType").value("INTERNAL"))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.scheduledAt").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void scheduleTransfer_ShouldReturn400_WhenPastDate() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);

        ScheduleTransferRequest request = ScheduleTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(UUID.randomUUID())
            .payeeName("Maria Santos")
            .transferType(TransferType.INTERNAL)
            .amount(Money.of(100.00))
            .scheduledAt(OffsetDateTime.now().minusDays(1)) // Past date
            .build();

        mockMvc.perform(post("/api/v1/transfers/schedule")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_SCHEDULE"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void cancelScheduledTransfer_ShouldReturn204() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        // Schedule a transfer first
        ScheduleTransferRequest scheduleRequest = ScheduleTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .payeeName("Maria Santos")
            .payeeCpf("555.666.777-88")
            .transferType(TransferType.INTERNAL)
            .amount(Money.of(200.00))
            .scheduledAt(OffsetDateTime.now().plusDays(1))
            .build();

        MvcResult scheduleResult = mockMvc.perform(post("/api/v1/transfers/schedule")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(scheduleRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String scheduleResponse = scheduleResult.getResponse().getContentAsString();
        String transferId = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(scheduleResponse).get("id").asText();

        // Cancel the scheduled transfer
        CancelTransferRequest cancelRequest = CancelTransferRequest.builder()
            .reason("Cliente desistiu")
            .build();

        mockMvc.perform(post("/api/v1/transfers/{transferId}/cancel", transferId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(cancelRequest)))
            .andExpect(status().isNoContent());

        // Verify cancelled
        mockMvc.perform(get("/api/v1/transfers/{transferId}", transferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getTransferByIdempotencyKey_ShouldReturnTransfer() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);
        String idempotencyKey = UUID.randomUUID().toString();

        CreateInternalTransferRequest request = CreateInternalTransferRequest.builder()
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .amount(Money.of(75.00))
            .description("Teste idempotency")
            .build();

        mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isCreated());

        // Get by idempotency key
        mockMvc.perform(get("/api/v1/transfers/idempotency/{idempotencyKey}", idempotencyKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idempotencyKey").value(idempotencyKey))
            .andExpect(jsonPath("$.amount").value("75.00"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void listTransfers_ShouldReturnFilteredResults() throws Exception {
        UUID payerAccountId = createTestAccount("111.222.333-44", AccountType.CURRENT);
        UUID payeeAccountId = createTestAccount("555.666.777-88", AccountType.CURRENT);

        // Create a few transfers
        mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreateInternalTransferRequest.builder()
                    .payerAccountId(payerAccountId)
                    .payeeAccountId(payeeAccountId)
                    .amount(Money.of(100.00))
                    .build())))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transfers/internal")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(CreateInternalTransferRequest.builder()
                    .payerAccountId(payerAccountId)
                    .payeeAccountId(payeeAccountId)
                    .amount(Money.of(200.00))
                    .build())))
            .andExpect(status().isCreated());

        // List transfers
        mockMvc.perform(get("/api/v1/transfers")
                .param("accountId", payerAccountId.toString())
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(2));
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