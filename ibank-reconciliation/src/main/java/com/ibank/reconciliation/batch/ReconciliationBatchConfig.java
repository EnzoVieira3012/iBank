package com.ibank.reconciliation.batch;

import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.repository.ReconciliationJobRepository;
import com.ibank.reconciliation.service.MatchingEngine;
import com.ibank.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ReconciliationBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ReconciliationService reconciliationService;
    private final MatchingEngine matchingEngine;
    private final ReconciliationJobRepository jobRepository;

    @Value("${ibank.reconciliation.daily-job-type:DAILY_SETTLEMENT}")
    private String dailyJobType;

    @Value("${ibank.reconciliation.source-system:core-banking}")
    private String sourceSystem;

    @Value("${ibank.reconciliation.target-system:general-ledger}")
    private String targetSystem;

    @Bean
    public Job dailyReconciliationJob() {
        return new JobBuilder("dailyReconciliationJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(reconciliationStep())
            .build();
    }

    @Bean
    public Step reconciliationStep() {
        return new StepBuilder("reconciliationStep", jobRepository)
            .<ReconciliationJob, ReconciliationJob>chunk(1, transactionManager)
            .reader(jobReader())
            .processor(jobProcessor())
            .writer(jobWriter())
            .build();
    }

    @Bean
    public ItemReader<ReconciliationJob> jobReader() {
        return () -> {
            Optional<ReconciliationJob> jobOpt = reconciliationService.getLatestJob(dailyJobType);
            if (jobOpt.isPresent() && jobOpt.get().getStatus().equals(ReconciliationJob.Status.PENDING.name())) {
                return jobOpt.get();
            }
            return null;
        };
    }

    @Bean
    public ItemProcessor<ReconciliationJob, ReconciliationJob> jobProcessor() {
        return job -> {
            log.info("Processing reconciliation job: {}", job.getId());
            reconciliationService.startJob(job.getId());

            List<Map<String, Object>> sourceData = fetchSourceData(job);
            List<Map<String, Object>> targetData = fetchTargetData(job);

            MatchingEngine.MatchResult result = matchingEngine.executeMatch(job, sourceData, targetData);

            saveBreaks(job, result.breaks());

            reconciliationService.completeJob(
                job.getId(),
                sourceData.size() + targetData.size(),
                result.matched().size(),
                result.missingInSource().size() + result.missingInTarget().size(),
                result.missingInSource().size(),
                result.missingInTarget().size(),
                result.breaks().size()
            );

            return jobRepository.findById(job.getId()).orElse(job);
        };
    }

    @Bean
    public ItemWriter<ReconciliationJob> jobWriter() {
        RepositoryItemWriter<ReconciliationJob> writer = new RepositoryItemWriterBuilder<ReconciliationJob>()
            .repository(jobRepository)
            .methodName("save")
            .build();
        return writer;
    }

    @Scheduled(cron = "${ibank.reconciliation.daily-cron:0 0 2 * * ?}")
    public void scheduleDailyReconciliation() {
        log.info("Scheduling daily reconciliation job");
        ReconciliationJob job = reconciliationService.createJob(
            dailyJobType,
            sourceSystem,
            targetSystem,
            "DAILY-" + Instant.now().toString(),
            Map.of("scheduled", true)
        );
        try {
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to schedule reconciliation job", e);
        }
    }

    private List<Map<String, Object>> fetchSourceData(ReconciliationJob job) {
        return List.of();
    }

    private List<Map<String, Object>> fetchTargetData(ReconciliationJob job) {
        return List.of();
    }

    private void saveBreaks(ReconciliationJob job, List<ReconciliationBreak> breaks) {
        // Breaks are saved during break creation in ReconciliationService
    }
}