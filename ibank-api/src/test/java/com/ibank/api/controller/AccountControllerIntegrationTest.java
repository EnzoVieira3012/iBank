package com.ibank.api.controller;

import com.ibank.api.dto.account.*;
import com.ibank.core.model.AccountType;
import com.ibank.core.model.AccountStatus;
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
class AccountControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void openAccount_ShouldReturn201_WhenValidRequest() throws Exception {
        OpenAccountRequest request = OpenAccountRequest.builder()
            .accountType(AccountType.CURRENT)
            .holderName("João Silva")
            .holderCpf("123.456.789-00")
            .holderEmail("joao@email.com")
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
            .andExpect(jsonPath("$.accountNumber").exists())
            .andExpect(jsonPath("$.holderName").value("João Silva"))
            .andExpect(jsonPath("$.holderCpf").value("123.456.789-00"))
            .andExpect(jsonPath("$.accountType").value("CURRENT"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("Created account: " + response);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void openAccount_ShouldReturn400_WhenInvalidCpf() throws Exception {
        OpenAccountRequest request = OpenAccountRequest.builder()
            .accountType(AccountType.CURRENT)
            .holderName("João Silva")
            .holderCpf("123")  // Invalid CPF
            .holderEmail("joao@email.com")
            .build();

        mockMvc.perform(post("/api/v1/accounts")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getAccount_ShouldReturn200_WhenAccountExists() throws Exception {
        // First create an account as OPERATOR
        UUID accountId = createTestAccount();

        // Then access as CLIENT
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("client").roles("CLIENT")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(accountId.toString()));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void listAccounts_ShouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getBalance_ShouldReturnDetailedBalance() throws Exception {
        UUID accountId = createTestAccount();

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(accountId.toString()))
            .andExpect(jsonPath("$.balance").exists())
            .andExpect(jsonPath("$.availableBalance").exists())
            .andExpect(jsonPath("$.effectiveAvailableBalance").exists());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void updateAccount_ShouldUpdateFields() throws Exception {
        UUID accountId = createTestAccount();

        UpdateAccountRequest request = UpdateAccountRequest.builder()
            .holderName("João Santos")
            .holderEmail("joao.santos@email.com")
            .pixEnabled(false)
            .debitCardEnabled(true)
            .dailyWithdrawalLimit(BigDecimal.valueOf(3000))
            .build();

        mockMvc.perform(put("/api/v1/accounts/{id}", accountId)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.holderName").value("João Santos"))
            .andExpect(jsonPath("$.holderEmail").value("joao.santos@email.com"))
            .andExpect(jsonPath("$.pixEnabled").value(false))
            .andExpect(jsonPath("$.debitCardEnabled").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void blockAccount_ShouldBlockAccount() throws Exception {
        UUID accountId = createTestAccount();

        BlockAccountRequest request = BlockAccountRequest.builder()
            .reason("Suspeita de fraude - transações atípicas")
            .build();

        mockMvc.perform(post("/api/v1/accounts/{id}/block", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isNoContent());

        // Verify account is blocked
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"))
            .andExpect(jsonPath("$.blockedReason").value("Suspeita de fraude - transações atípicas"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void unblockAccount_ShouldUnblockAccount() throws Exception {
        UUID accountId = createTestAccount();

        // First block
        mockMvc.perform(post("/api/v1/accounts/{id}/block", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(BlockAccountRequest.builder().reason("Teste").build())))
            .andExpect(status().isNoContent());

        // Then unblock
        mockMvc.perform(post("/api/v1/accounts/{id}/unblock", accountId))
            .andExpect(status().isNoContent());

        // Verify account is active
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void closeAccount_ShouldCloseAccount() throws Exception {
        UUID accountId = createTestAccount();

        CloseAccountRequest request = CloseAccountRequest.builder()
            .reason("Solicitação do cliente")
            .build();

        mockMvc.perform(post("/api/v1/accounts/{id}/close", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.toJson(request)))
            .andExpect(status().isNoContent());

        // Verify account is closed
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getAccountsByCpf_ShouldReturnAllAccounts() throws Exception {
        String cpf = "987.654.321-00";
        createTestAccount(cpf, AccountType.CURRENT);
        createTestAccount(cpf, AccountType.SAVINGS);

        mockMvc.perform(get("/api/v1/accounts/by-cpf/{cpf}", cpf))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getAccountByNumber_ShouldReturnAccount() throws Exception {
        UUID accountId = createTestAccount();
        var account = mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
            .andReturn().getResponse().getContentAsString();
        
        // Extract account number from response
        String accountNumber = com.fasterxml.jackson.databind.ObjectMapper
            .valueToTree(account).get("accountNumber").asText();

        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}", accountNumber))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value(accountNumber));
    }

    private UUID createTestAccount() {
        return createTestAccount("123.456.789-00", AccountType.CURRENT);
    }

    private UUID createTestAccount(String cpf, AccountType type) {
        try {
            OpenAccountRequest request = OpenAccountRequest.builder()
                .accountType(type)
                .holderName("Test User")
                .holderCpf(cpf)
                .holderEmail("test@email.com")
                .holderPhone("+5511999999999")
                .holderBirthDate(LocalDate.of(1990, 1, 1))
                .branch("0001")
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