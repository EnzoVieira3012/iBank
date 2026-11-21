package com.ibank.reconciliation;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.repository.ReconciliationBreakRepository;
import com.ibank.reconciliation.repository.ReconciliationJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReconciliationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReconciliationJobRepository jobRepository;

    @Autowired
    private ReconciliationBreakRepository breakRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJob_shouldReturnJob() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation/jobs")
                .param("jobType", "DAILY_SETTLEMENT")
                .param("sourceSystem", "core")
                .param("targetSystem", "gl")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobType").value("DAILY_SETTLEMENT"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void runJob_withMatchingRecords_shouldCompleteWithNoBreaks() throws Exception {
        var job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("PENDING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        String request = """
            {
              "sourceRecords": [
                {"entityType": "Transfer", "entityId": "txn-1", "reference": "ref-1", "amount": 100.00, "currency": "BRL"}
              ],
              "targetRecords": [
                {"entityType": "Transfer", "entityId": "txn-1", "reference": "ref-1", "amount": 100.00, "currency": "BRL"}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/reconciliation/jobs/{jobId}/run", job.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.breaksCount").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void runJob_withAmountMismatch_shouldCreateBreak() throws Exception {
        var job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("PENDING")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        String request = """
            {
              "sourceRecords": [
                {"entityType": "Transfer", "entityId": "txn-1", "reference": "ref-1", "amount": 100.00, "currency": "BRL"}
              ],
              "targetRecords": [
                {"entityType": "Transfer", "entityId": "txn-1", "reference": "ref-1", "amount": 101.00, "currency": "BRL"}
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/reconciliation/jobs/{jobId}/run", job.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED_WITH_BREAKS"))
            .andExpect(jsonPath("$.breaksCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getJobBreaks_shouldReturnBreaks() throws Exception {
        var job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("COMPLETED")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        var brk = ReconciliationBreak.builder()
            .job(job)
            .breakType("AMOUNT_MISMATCH")
            .entityType("Transfer")
            .entityId("txn-1")
            .sourceAmount(new BigDecimal("100"))
            .targetAmount(new BigDecimal("101"))
            .currency("BRL")
            .status("OPEN")
            .priority("MEDIUM")
            .build();
        breakRepository.save(brk);

        mockMvc.perform(get("/api/v1/reconciliation/jobs/{jobId}/breaks", job.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].breakType").value("AMOUNT_MISMATCH"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resolveBreak_shouldUpdateStatus() throws Exception {
        var job = ReconciliationJob.builder()
            .jobType("DAILY_SETTLEMENT")
            .status("COMPLETED")
            .sourceSystem("core")
            .targetSystem("gl")
            .build();
        job = jobRepository.save(job);

        var brk = ReconciliationBreak.builder()
            .job(job)
            .breakType("AMOUNT_MISMATCH")
            .entityType("Transfer")
            .entityId("txn-1")
            .sourceAmount(new BigDecimal("100"))
            .targetAmount(new BigDecimal("101"))
            .currency("BRL")
            .status("OPEN")
            .priority("MEDIUM")
            .build();
        brk = breakRepository.save(brk);

        mockMvc.perform(post("/api/v1/reconciliation/breaks/{breakId}/resolve", brk.getId())
                .param("resolvedBy", "user-1")
                .param("notes", "Fixed")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.resolvedBy").value("user-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRules_shouldReturnDefaultRules() throws Exception {
        mockMvc.perform(get("/api/v1/reconciliation/rules")
                .param("jobType", "DAILY_SETTLEMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").exists());
    }
}