package com.ibank.reconciliation.service;

import com.ibank.reconciliation.model.ReconciliationBreak;
import com.ibank.reconciliation.model.ReconciliationJob;
import com.ibank.reconciliation.model.ReconciliationRule;
import com.ibank.reconciliation.repository.ReconciliationBreakRepository;
import com.ibank.reconciliation.repository.ReconciliationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngine {

    private final ReconciliationRuleRepository ruleRepository;
    private final ReconciliationBreakRepository breakRepository;

    public record MatchResult(
        List<MatchedPair> matched,
        List<UnmatchedRecord> missingInSource,
        List<UnmatchedRecord> missingInTarget,
        List<ReconciliationBreak> breaks
    ) {}

    public record MatchedRecord(
        String entityType,
        String entityId,
        Map<String, Object> sourceData,
        Map<String, Object> targetData
    ) {}

    public record MatchedPair(
        MatchedRecord source,
        MatchedRecord target,
        Map<String, Object> differences
    ) {}

    public record UnmatchedRecord(
        String entityType,
        String entityId,
        Map<String, Object> data,
        String reference
    ) {}

    @Transactional(readOnly = true)
    public MatchResult executeMatch(ReconciliationJob job,
                                    List<Map<String, Object>> sourceRecords,
                                    List<Map<String, Object>> targetRecords) {
        List<ReconciliationRule> rules = ruleRepository.findByJobTypeAndActiveTrueOrderByPriorityDesc(job.getJobType());

        Map<String, List<Map<String, Object>>> sourceByEntity = groupByEntity(sourceRecords);
        Map<String, List<Map<String, Object>>> targetByEntity = groupByEntity(targetRecords);

        List<MatchedPair> matched = new ArrayList<>();
        List<UnmatchedRecord> missingInSource = new ArrayList<>();
        List<UnmatchedRecord> missingInTarget = new ArrayList<>();
        List<ReconciliationBreak> breaks = new ArrayList<>();

        Set<String> allEntities = new HashSet<>();
        allEntities.addAll(sourceByEntity.keySet());
        allEntities.addAll(targetByEntity.keySet());

        for (String entityKey : allEntities) {
            List<Map<String, Object>> srcList = sourceByEntity.getOrDefault(entityKey, List.of());
            List<Map<String, Object>> tgtList = targetByEntity.getOrDefault(entityKey, List.of());

            if (srcList.isEmpty() && !tgtList.isEmpty()) {
                for (Map<String, Object> tgt : tgtList) {
                    missingInSource.add(toUnmatched(tgt, "target"));
                    breaks.add(createBreak(job, ReconciliationBreak.BreakType.MISSING_IN_SOURCE, entityKey, tgt, null));
                }
            } else if (!srcList.isEmpty() && tgtList.isEmpty()) {
                for (Map<String, Object> src : srcList) {
                    missingInTarget.add(toUnmatched(src, "source"));
                    breaks.add(createBreak(job, ReconciliationBreak.BreakType.MISSING_IN_TARGET, entityKey, src, null));
                }
            } else {
                for (Map<String, Object> src : srcList) {
                    Optional<Map<String, Object>> match = findBestMatch(src, tgtList, rules);
                    if (match.isPresent()) {
                        Map<String, Object> diffs = compareRecords(src, match.get(), rules);
                        matched.add(new MatchedPair(
                            toMatched(src, "source"),
                            toMatched(match.get(), "target"),
                            diffs
                        ));
                        if (!diffs.isEmpty()) {
                            breaks.add(createBreak(job, inferBreakType(diffs), entityKey, src, match.get(), diffs));
                        }
                    } else {
                        missingInTarget.add(toUnmatched(src, "source"));
                        breaks.add(createBreak(job, ReconciliationBreak.BreakType.MISSING_IN_TARGET, entityKey, src, null));
                    }
                }
                for (Map<String, Object> tgt : tgtList) {
                    boolean matchedFlag = matched.stream()
                        .anyPair(m -> m.target().entityId().equals(getEntityId(tgt)))
                        .orElse(false);
                    if (!matchedFlag) {
                        missingInSource.add(toUnmatched(tgt, "target"));
                        breaks.add(createBreak(job, ReconciliationBreak.BreakType.MISSING_IN_SOURCE, entityKey, null, tgt));
                    }
                }
            }
        }

        return new MatchResult(matched, missingInSource, missingInTarget, breaks);
    }

    private Map<String, List<Map<String, Object>>> groupByEntity(List<Map<String, Object>> records) {
        return records.stream()
            .collect(Collectors.groupingBy(this::getEntityKey));
    }

    private String getEntityKey(Map<String, Object> record) {
        String entityType = (String) record.getOrDefault("entityType", "Unknown");
        String entityId = (String) record.getOrDefault("entityId", record.getOrDefault("id", "unknown"));
        return entityType + ":" + entityId;
    }

    private String getEntityId(Map<String, Object> record) {
        return (String) record.getOrDefault("entityId", record.getOrDefault("id", "unknown"));
    }

    private Optional<Map<String, Object>> findBestMatch(Map<String, Object> source,
                                                        List<Map<String, Object>> targets,
                                                        List<ReconciliationRule> rules) {
        return targets.stream()
            .filter(t -> matchesOnKeys(source, t, rules))
            .findFirst();
    }

    private boolean matchesOnKeys(Map<String, Object> source, Map<String, Object> target,
                                  List<ReconciliationRule> rules) {
        for (ReconciliationRule rule : rules) {
            @SuppressWarnings("unchecked")
            Map<String, Object> matchKeys = rule.getMatchKeys();
            if (matchKeys != null) {
                boolean allMatch = matchKeys.keySet().stream()
                    .allMatch(key -> Objects.equals(source.get(key), target.get(key)));
                if (allMatch) return true;
            }
        }
        String srcId = getEntityId(source);
        String tgtId = getEntityId(target);
        return Objects.equals(srcId, tgtId);
    }

    private Map<String, Object> compareRecords(Map<String, Object> source, Map<String, Object> target,
                                               List<ReconciliationRule> rules) {
        Map<String, Object> diffs = new LinkedHashMap<>();
        for (ReconciliationRule rule : rules) {
            @SuppressWarnings("unchecked")
            Map<String, Object> compareFields = rule.getCompareFields();
            if (compareFields != null) {
                for (String field : compareFields.keySet()) {
                    Object srcVal = source.get(field);
                    Object tgtVal = target.get(field);
                    if (!Objects.equals(srcVal, tgtVal)) {
                        if (isWithinTolerance(srcVal, tgtVal, rule)) continue;
                        diffs.put(field, Map.of("source", srcVal, "target", tgtVal));
                    }
                }
            }
        }
        return diffs;
    }

    private boolean isWithinTolerance(Object srcVal, Object tgtVal, ReconciliationRule rule) {
        if (srcVal == null || tgtVal == null) return false;
        if (!(srcVal instanceof Number) || !(tgtVal instanceof Number)) return false;

        @SuppressWarnings("unchecked")
        Map<String, Object> tolerance = rule.getToleranceConfig();
        if (tolerance == null) return false;

        BigDecimal src = new BigDecimal(srcVal.toString());
        BigDecimal tgt = new BigDecimal(tgtVal.toString());
        BigDecimal diff = src.subtract(tgt).abs();

        BigDecimal absTol = tolerance.containsKey("absolute") ? new BigDecimal(tolerance.get("absolute").toString()) : null;
        BigDecimal pctTol = tolerance.containsKey("percentage") ? new BigDecimal(tolerance.get("percentage").toString()) : null;

        if (absTol != null && diff.compareTo(absTol) <= 0) return true;
        if (pctTol != null) {
            BigDecimal maxVal = src.max(tgt);
            if (maxVal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pctDiff = diff.divide(maxVal, 10, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
                if (pctDiff.compareTo(pctTol) <= 0) return true;
            }
        }
        return false;
    }

    private ReconciliationBreak.BreakType inferBreakType(Map<String, Object> diffs) {
        if (diffs.containsKey("amount") || diffs.containsKey("value")) {
            return ReconciliationBreak.BreakType.AMOUNT_MISMATCH;
        }
        if (diffs.containsKey("currency")) {
            return ReconciliationBreak.BreakType.CURRENCY_MISMATCH;
        }
        if (diffs.containsKey("fee")) {
            return ReconciliationBreak.BreakType.FEE_MISMATCH;
        }
        if (diffs.containsKey("status") || diffs.containsKey("state")) {
            return ReconciliationBreak.BreakType.STATUS_MISMATCH;
        }
        if (diffs.containsKey("timestamp") || diffs.containsKey("date")) {
            return ReconciliationBreak.BreakType.TIMING_DIFFERENCE;
        }
        return ReconciliationBreak.BreakType.REFERENCE_MISMATCH;
    }

    private MatchedRecord toMatched(Map<String, Object> record, String side) {
        return new MatchedRecord(
            (String) record.getOrDefault("entityType", "Unknown"),
            getEntityId(record),
            record
        );
    }

    private UnmatchedRecord toUnmatched(Map<String, Object> record, String side) {
        return new UnmatchedRecord(
            (String) record.getOrDefault("entityType", "Unknown"),
            getEntityId(record),
            record,
            (String) record.getOrDefault("reference", getEntityId(record))
        );
    }

    private ReconciliationBreak createBreak(ReconciliationJob job, ReconciliationBreak.BreakType type,
                                            String entityKey, Map<String, Object> src, Map<String, Object> tgt) {
        return createBreak(job, type, entityKey, src, tgt, Map.of());
    }

    private ReconciliationBreak createBreak(ReconciliationJob job, ReconciliationBreak.BreakType type,
                                            String entityKey, Map<String, Object> src, Map<String, Object> tgt,
                                            Map<String, Object> diffs) {
        String[] parts = entityKey.split(":", 2);
        String entityType = parts.length > 0 ? parts[0] : "Unknown";
        String entityId = parts.length > 1 ? parts[1] : "unknown";

        BigDecimal srcAmt = extractAmount(src);
        BigDecimal tgtAmt = extractAmount(tgt);
        String currency = extractCurrency(src, tgt);

        return ReconciliationBreak.builder()
            .job(job)
            .breakType(type.name())
            .entityType(entityType)
            .entityId(entityId)
            .sourceReference(extractRef(src))
            .targetReference(extractRef(tgt))
            .sourceAmount(srcAmt)
            .targetAmount(tgtAmt)
            .differenceAmount(srcAmt != null && tgtAmt != null ? srcAmt.subtract(tgtAmt).abs() : null)
            .currency(currency)
            .status(ReconciliationBreak.Status.OPEN.name())
            .priority(calculatePriority(type, srcAmt, tgtAmt).name())
            .sourceData(src)
            .targetData(tgt)
            .resolutionData(diffs)
            .build();
    }

    private BigDecimal extractAmount(Map<String, Object> record) {
        if (record == null) return null;
        Object amt = record.get("amount") != null ? record.get("amount") : record.get("value");
        return amt instanceof Number ? new BigDecimal(amt.toString()) : null;
    }

    private String extractCurrency(Map<String, Object> src, Map<String, Object> tgt) {
        if (src != null && src.get("currency") != null) return (String) src.get("currency");
        if (tgt != null && tgt.get("currency") != null) return (String) tgt.get("currency");
        return "BRL";
    }

    private String extractRef(Map<String, Object> record) {
        if (record == null) return null;
        return (String) record.getOrDefault("reference", record.getOrDefault("id", null));
    }

    private ReconciliationBreak.Priority calculatePriority(ReconciliationBreak.BreakType type,
                                                            BigDecimal srcAmt, BigDecimal tgtAmt) {
        if (type == ReconciliationBreak.BreakType.AMOUNT_MISMATCH && srcAmt != null && tgtAmt != null) {
            BigDecimal diff = srcAmt.subtract(tgtAmt).abs();
            if (diff.compareTo(new BigDecimal("100000")) > 0) return ReconciliationBreak.Priority.CRITICAL;
            if (diff.compareTo(new BigDecimal("10000")) > 0) return ReconciliationBreak.Priority.HIGH;
        }
        if (type == ReconciliationBreak.BreakType.MISSING_IN_SOURCE ||
            type == ReconciliationBreak.BreakType.MISSING_IN_TARGET) {
            return ReconciliationBreak.Priority.HIGH;
        }
        return ReconciliationBreak.Priority.MEDIUM;
    }
}