package com.ibank.credit.service;

import com.ibank.account.model.Account;
import com.ibank.account.model.AccountStatus;
import com.ibank.account.repository.AccountRepository;
import com.ibank.account.service.AccountService;
import com.ibank.core.exception.BusinessException;
import com.ibank.core.exception.NotFoundException;
import com.ibank.core.util.Money;
import com.ibank.credit.calculator.AmortizationCalculator;
import com.ibank.credit.calculator.CalculatorFactory;
import com.ibank.credit.calculator.InstallmentSchedule;
import com.ibank.credit.model.AmortizationType;
import com.ibank.credit.model.Collateral;
import com.ibank.credit.model.CollateralType;
import com.ibank.credit.model.Installment;
import com.ibank.credit.model.InstallmentStatus;
import com.ibank.credit.model.Loan;
import com.ibank.credit.model.LoanSimulation;
import com.ibank.credit.model.LoanStatus;
import com.ibank.credit.model.LoanType;
import com.ibank.credit.repository.CollateralRepository;
import com.ibank.credit.repository.InstallmentRepository;
import com.ibank.credit.repository.LoanRepository;
import com.ibank.credit.repository.LoanSimulationRepository;
import com.ibank.credit.event.LoanContractedEvent;
import com.ibank.credit.event.InstallmentPaidEvent;
import com.ibank.credit.event.LoanSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final LoanSimulationRepository simulationRepository;
    private final CollateralRepository collateralRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final CalculatorFactory calculatorFactory;
    private final ApplicationEventPublisher eventPublisher;

    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("0.25"); // 25% a.m.
    private static final BigDecimal MIN_INTEREST_RATE = new BigDecimal("0.005"); // 0.5% a.m.

    // ==================== SIMULATION ====================

    public LoanSimulation simulate(SimulateLoanCommand command) {
        Account account = accountRepository.findById(command.accountId())
            .orElseThrow(() -> new NotFoundException("Account not found: " + command.accountId()));

        validateAccountForLoan(account);

        AmortizationCalculator calculator = calculatorFactory.getCalculator(command.amortizationType());
        
        // Convert annual rate to monthly
        BigDecimal monthlyRate = command.annualRate()
            .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        List<InstallmentSchedule> schedule = calculator.calculate(
            command.amount(),
            monthlyRate,
            command.termMonths(),
            command.firstDueDate()
        );

        Money totalAmount = Money.ZERO;
        Money totalInterest = Money.ZERO;
        for (InstallmentSchedule s : schedule) {
            totalAmount = totalAmount.add(s.totalAmount());
            totalInterest = totalInterest.add(s.interestAmount());
        }

        // Calculate CET (Custo Efetivo Total) - simplified
        BigDecimal cet = calculateCET(command.amount(), totalAmount, command.termMonths());

        Money originationFee = calculateOriginationFee(command.amount(), command.loanType());
        Money insuranceAmount = calculateInsurance(command.amount(), command.termMonths());

        LoanSimulation simulation = LoanSimulation.builder()
            .accountId(account.getId())
            .loanType(command.loanType())
            .amortizationType(command.amortizationType())
            .requestedAmount(command.amount())
            .interestRate(command.annualRate())
            .termMonths(command.termMonths())
            .installmentAmount(schedule.get(0).totalAmount())
            .totalAmount(totalAmount)
            .totalInterest(totalInterest)
            .originationFee(originationFee)
            .insuranceAmount(insuranceAmount)
            .cet(cet)
            .firstDueDate(command.firstDueDate())
            .build();

        return simulationRepository.save(simulation);
    }

    public LoanSimulation simulateWithCollateral(SimulateLoanWithCollateralCommand command) {
        LoanSimulation simulation = simulate(new SimulateLoanCommand(
            command.accountId(),
            command.loanType(),
            command.amortizationType(),
            command.amount(),
            command.annualRate(),
            command.termMonths(),
            command.firstDueDate()
        ));

        // Collateral reduces rate
        BigDecimal collateralDiscount = calculateCollateralDiscount(command.collateralType(), command.collateralValue());
        BigDecimal adjustedRate = simulation.getInterestRate().subtract(collateralDiscount);
        
        simulation.setInterestRate(adjustedRate.max(MIN_INTEREST_RATE));
        
        // Recalculate with adjusted rate
        return simulate(new SimulateLoanCommand(
            command.accountId(),
            command.loanType(),
            command.amortizationType(),
            command.amount(),
            simulation.getInterestRate(),
            command.termMonths(),
            command.firstDueDate()
        ));
    }

    // ==================== CONTRACTING ====================

    public Loan contractLoan(ContractLoanCommand command, String idempotencyKey) {
        if (loanRepository.existsByContractNumber(command.contractNumber())) {
            throw BusinessException.conflict("CONTRACT_EXISTS", "Contract number already exists");
        }

        Account account = accountRepository.findById(command.accountId())
            .orElseThrow(() -> new NotFoundException("Account not found: " + command.accountId()));

        validateAccountForLoan(account);

        // Verify simulation exists and matches
        LoanSimulation simulation = simulationRepository.findById(command.simulationId())
            .orElseThrow(() -> new NotFoundException("Simulation not found: " + command.simulationId()));

        if (!simulation.getAccountId().equals(account.getId())) {
            throw BusinessException.badRequest("SIMULATION_MISMATCH", "Simulation does not belong to this account");
        }

        Loan loan = Loan.builder()
            .contractNumber(command.contractNumber())
            .loanType(simulation.getLoanType())
            .amortizationType(simulation.getAmortizationType())
            .status(LoanStatus.PENDING_APPROVAL)
            .principalAmount(simulation.getRequestedAmount())
            .totalAmount(simulation.getTotalAmount())
            .totalInterest(simulation.getTotalInterest())
            .outstandingBalance(simulation.getTotalAmount())
            .interestRate(simulation.getInterestRate())
            .termMonths(simulation.getTermMonths())
            .installmentAmount(simulation.getInstallmentAmount())
            .firstDueDate(simulation.getFirstDueDate())
            .lastDueDate(simulation.getFirstDueDate().plusMonths(simulation.getTermMonths() - 1))
            .accountId(account.getId())
            .holderName(account.getHolderName())
            .holderCpf(account.getHolderCpf())
            .collateralType(simulation.getCollateralType())
            .collateralValue(simulation.getCollateralValue())
            .originationFee(simulation.getOriginationFee())
            .insuranceAmount(simulation.getInsuranceAmount())
            .build();

        Loan saved = loanRepository.save(loan);

        // Create installments
        createInstallments(saved, simulation);

        log.info("Loan contracted: {} for account {}", saved.getContractNumber(), account.getAccountNumber());
        return saved;
    }

    public Loan approveLoan(UUID loanId, ApproveLoanCommand command) {
        Loan loan = getLoan(loanId);

        if (loan.getStatus() != LoanStatus.PENDING_APPROVAL) {
            throw BusinessException.conflict("INVALID_STATUS", "Loan is not pending approval");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(OffsetDateTime.now());
        loan.setApprovedBy(command.approvedBy());
        
        // Apply origination fee
        if (loan.getOriginationFee() != null && loan.getOriginationFee().isPositive()) {
            accountService.debit(loan.getAccountId(), loan.getOriginationFee(), 
                "Taxa de abertura de crédito - Contrato " + loan.getContractNumber());
        }

        loanRepository.save(loan);
        log.info("Loan approved: {}", loan.getContractNumber());
        return loan;
    }

    public Loan rejectLoan(UUID loanId, RejectLoanCommand command) {
        Loan loan = getLoan(loanId);

        if (loan.getStatus() != LoanStatus.PENDING_APPROVAL) {
            throw BusinessException.conflict("INVALID_STATUS", "Loan is not pending approval");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectedAt(OffsetDateTime.now());
        loan.setRejectionReason(command.reason());
        loanRepository.save(loan);
        
        log.info("Loan rejected: {} - reason: {}", loan.getContractNumber(), command.reason());
        return loan;
    }

    public Loan disburseLoan(UUID loanId) {
        Loan loan = getLoan(loanId);

        if (!loan.canDisburse()) {
            throw BusinessException.conflict("INVALID_STATUS", "Loan cannot be disbursed. Status: " + loan.getStatus());
        }

        loan.disburse();
        loanRepository.save(loan);

        // Credit the account
        accountService.credit(loan.getAccountId(), loan.getPrincipalAmount(),
            "Liberação de empréstimo - Contrato " + loan.getContractNumber());

        loan.activate();
        loanRepository.save(loan);

        // Create installments if not created
        if (installmentRepository.findByLoanId(loan.getId()).isEmpty()) {
            createInstallmentsFromLoan(loan);
        }

        eventPublisher.publishEvent(new LoanContractedEvent(
            loan.getId(), loan.getContractNumber(), loan.getLoanType(),
            loan.getAmortizationType(), loan.getAccountId(), loan.getHolderCpf(),
            loan.getPrincipalAmount(), loan.getInstallmentAmount(), loan.getTermMonths(),
            loan.getFirstDueDate(), loan.getInterestRate()
        ));

        log.info("Loan disbursed: {}", loan.getContractNumber());
        return loan;
    }

    // ==================== INSTALLMENT MANAGEMENT ====================

    public Installment payInstallment(UUID installmentId, PayInstallmentCommand command) {
        Installment installment = installmentRepository.findById(installmentId)
            .orElseThrow(() -> new NotFoundException("Installment not found: " + installmentId));

        if (!installment.getStatus().isOpen()) {
            throw BusinessException.conflict("INSTALLMENT_CLOSED", "Installment is already paid or waived");
        }

        Loan loan = getLoan(installment.getLoanId());
        if (!loan.isActive()) {
            throw BusinessException.conflict("LOAN_NOT_ACTIVE", "Loan is not active");
        }

        Account account = accountRepository.findById(loan.getAccountId())
            .orElseThrow(() -> new NotFoundException("Account not found"));

        Money totalToPay = installment.getRemainingTotal();
        if (command.amount() != null && command.amount().isLessThan(totalToPay)) {
            totalToPay = command.amount();
        }

        // Debit from account
        accountService.debit(account.getId(), totalToPay,
            "Pagamento parcela " + installment.getInstallmentNumber() + " - Contrato " + loan.getContractNumber());

        // Calculate principal/interest split
        Money remainingPrincipal = installment.getRemainingPrincipal();
        Money principalPaid = totalToPay.min(remainingPrincipal);
        Money interestPaid = totalToPay.subtract(principalPaid);

        installment.markPaid(principalPaid, interestPaid, command.transactionId());
        installmentRepository.save(installment);

        // Update loan
        loan.payInstallment(totalToPay);
        loanRepository.save(loan);

        eventPublisher.publishEvent(new InstallmentPaidEvent(
            installment.getId(), loan.getId(), installment.getInstallmentNumber(),
            principalPaid, interestPaid, totalToPay, installment.getDueDate(),
            command.transactionId()
        ));

        // Check if loan is settled
        if (loan.getStatus() == LoanStatus.SETTLED) {
            handleLoanSettlement(loan);
        }

        log.info("Installment paid: {} of loan {}", installment.getInstallmentNumber(), loan.getContractNumber());
        return installment;
    }

    public Loan settleLoan(UUID loanId, SettleLoanCommand command) {
        Loan loan = getLoan(loanId);

        if (!loan.canSettle()) {
            throw BusinessException.conflict("CANNOT_SETTLE", "Loan cannot be settled. Status: " + loan.getStatus());
        }

        Account account = accountRepository.findById(loan.getAccountId())
            .orElseThrow(() -> new NotFoundException("Account not found"));

        // Get all open installments
        List<Installment> openInstallments = installmentRepository.findOpenInstallmentsByLoan(loan.getId());
        Money totalOutstanding = loan.getOutstandingBalance().add(loan.getOverdueAmount());

        // Apply early settlement discount if applicable
        Money discount = Money.ZERO;
        if (command.applyDiscount() && loan.getStatus() != LoanStatus.OVERDUE) {
            discount = calculateEarlySettlementDiscount(loan, openInstallments);
            totalOutstanding = totalOutstanding.subtract(discount);
        }

        if (totalOutstanding.isPositive()) {
            accountService.debit(account.getId(), totalOutstanding,
                "Quitação antecipada - Contrato " + loan.getContractNumber());
        }

        // Mark all installments as paid
        for (Installment inst : openInstallments) {
            if (inst.getStatus().isOpen()) {
                Money remaining = inst.getRemainingTotal();
                inst.markPaid(inst.getRemainingPrincipal(), inst.getRemainingInterest(), command.transactionId());
                installmentRepository.save(inst);
            }
        }

        loan.setStatus(LoanStatus.SETTLED);
        loan.setSettledAt(OffsetDateTime.now());
        loan.setOutstandingBalance(Money.ZERO);
        loan.setOverdueAmount(Money.ZERO);
        loanRepository.save(loan);

        handleLoanSettlement(loan);

        log.info("Loan settled: {} - early: {}, discount: {}", loan.getContractNumber(), command.applyDiscount(), discount);
        return loan;
    }

    public Loan renegotiateLoan(UUID loanId, RenegotiateLoanCommand command) {
        Loan loan = getLoan(loanId);

        if (!loan.canRenegotiate()) {
            throw BusinessException.conflict("CANNOT_RENEGOTIATE", "Loan cannot be renegotiated. Status: " + loan.getStatus());
        }

        // Create new installments with new terms
        List<Installment> oldInstallments = installmentRepository.findOpenInstallmentsByLoan(loan.getId());
        Money totalOutstanding = loan.getOutstandingBalance().add(loan.getOverdueAmount());

        // Mark old installments as renegotiated
        for (Installment inst : oldInstallments) {
            inst.setStatus(InstallmentStatus.RENEGOTIATED);
            inst.setRenegotiatedAt(OffsetDateTime.now());
            installmentRepository.save(inst);
        }

        // Create new loan with new terms (simplified - creates new loan)
        Loan newLoan = Loan.builder()
            .contractNumber(generateContractNumber())
            .loanType(loan.getLoanType())
            .amortizationType(command.newAmortizationType() != null ? command.newAmortizationType() : loan.getAmortizationType())
            .status(LoanStatus.RENEGOTIATED)
            .principalAmount(totalOutstanding)
            .totalAmount(totalOutstanding)
            .outstandingBalance(totalOutstanding)
            .interestRate(command.newInterestRate() != null ? command.newInterestRate() : loan.getInterestRate())
            .termMonths(command.newTermMonths() != null ? command.newTermMonths() : loan.getTermMonths())
            .firstDueDate(command.newFirstDueDate() != null ? command.newFirstDueDate() : LocalDate.now().plusMonths(1))
            .accountId(loan.getAccountId())
            .holderName(loan.getHolderName())
            .holderCpf(loan.getHolderCpf())
            .build();

        newLoan = loanRepository.save(newLoan);
        createInstallmentsFromLoan(newLoan);

        loan.setStatus(LoanStatus.RENEGOTIATED);
        loan.renegotiate();
        loanRepository.save(loan);

        log.info("Loan renegotiated: {} -> {}", loan.getContractNumber(), newLoan.getContractNumber());
        return newLoan;
    }

    // ==================== QUERIES ====================

    @Transactional(readOnly = true)
    public Loan getLoan(UUID loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
    }

    @Transactional(readOnly = true)
    public Loan getLoanByContractNumber(String contractNumber) {
        return loanRepository.findByContractNumber(contractNumber)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + contractNumber));
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByAccount(UUID accountId) {
        return loanRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Loan> getActiveLoansByAccount(UUID accountId) {
        return loanRepository.findActiveLoansByAccount(accountId);
    }

    @Transactional(readOnly = true)
    public List<Installment> getInstallmentsByLoan(UUID loanId) {
        return installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
    }

    @Transactional(readOnly = true)
    public List<Installment> getOpenInstallmentsByLoan(UUID loanId) {
        return installmentRepository.findOpenInstallmentsByLoan(loanId);
    }

    @Transactional(readOnly = true)
    public LoanSimulation getSimulation(UUID simulationId) {
        return simulationRepository.findById(simulationId)
            .orElseThrow(() -> new NotFoundException("Simulation not found: " + simulationId));
    }

    // ==================== PRIVATE METHODS ====================

    private void validateAccountForLoan(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw BusinessException.conflict("ACCOUNT_NOT_ACTIVE", "Account must be active for loan");
        }
    }

    private void createInstallments(Loan loan, LoanSimulation simulation) {
        AmortizationCalculator calculator = calculatorFactory.getCalculator(simulation.getAmortizationType());
        BigDecimal monthlyRate = simulation.getInterestRate()
            .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        List<InstallmentSchedule> schedule = calculator.calculate(
            simulation.getRequestedAmount(),
            monthlyRate,
            simulation.getTermMonths(),
            simulation.getFirstDueDate()
        );

        for (InstallmentSchedule s : schedule) {
            Installment installment = Installment.builder()
                .loanId(loan.getId())
                .installmentNumber(s.installmentNumber())
                .status(InstallmentStatus.PENDING)
                .principalAmount(s.principalAmount())
                .interestAmount(s.interestAmount())
                .totalAmount(s.totalAmount())
                .dueDate(s.dueDate())
                .build();
            installmentRepository.save(installment);
        }
    }

    private void createInstallmentsFromLoan(Loan loan) {
        AmortizationCalculator calculator = calculatorFactory.getCalculator(loan.getAmortizationType());
        BigDecimal monthlyRate = loan.getInterestRate()
            .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        List<InstallmentSchedule> schedule = calculator.calculate(
            loan.getPrincipalAmount(),
            monthlyRate,
            loan.getTermMonths(),
            loan.getFirstDueDate()
        );

        for (InstallmentSchedule s : schedule) {
            Installment installment = Installment.builder()
                .loanId(loan.getId())
                .installmentNumber(s.installmentNumber())
                .status(InstallmentStatus.PENDING)
                .principalAmount(s.principalAmount())
                .interestAmount(s.interestAmount())
                .totalAmount(s.totalAmount())
                .dueDate(s.dueDate())
                .build();
            installmentRepository.save(installment);
        }
    }

    private void handleLoanSettlement(Loan loan) {
        List<Installment> allInstallments = installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        Money totalPaid = Money.ZERO;
        Money totalInterestPaid = Money.ZERO;
        for (Installment inst : allInstallments) {
            totalPaid = totalPaid.add(inst.getPaidTotal());
            totalInterestPaid = totalInterestPaid.add(inst.getPaidInterest());
        }

        eventPublisher.publishEvent(new LoanSettledEvent(
            loan.getId(), loan.getContractNumber(), loan.getAccountId(),
            totalPaid, totalInterestPaid, allInstallments.size(),
            loan.getStatus() == LoanStatus.SETTLED && loan.getTermMonths() > allInstallments.size(),
            Money.ZERO
        ));
    }

    private BigDecimal calculateCET(Money principal, Money totalAmount, int termMonths) {
        // Simplified CET calculation
        BigDecimal totalCost = totalAmount.toBigDecimal().subtract(principal.toBigDecimal());
        return totalCost.divide(principal.toBigDecimal(), 6, RoundingMode.HALF_EVEN)
            .divide(BigDecimal.valueOf(termMonths), 6, RoundingMode.HALF_EVEN)
            .multiply(BigDecimal.valueOf(12));
    }

    private Money calculateOriginationFee(Money principal, LoanType loanType) {
        BigDecimal rate = switch (loanType) {
            case PERSONAL, CONSIGNMENT -> new BigDecimal("0.02");
            case VEHICLE -> new BigDecimal("0.015");
            case REAL_ESTATE -> new BigDecimal("0.01");
            case WORKING_CAPITAL -> new BigDecimal("0.025");
            case STUDENT -> new BigDecimal("0.01");
        };
        return Money.of(principal.toBigDecimal().multiply(rate).setScale(2, RoundingMode.HALF_EVEN));
    }

    private Money calculateInsurance(Money principal, int termMonths) {
        // Simplified: 0.05% per month on outstanding balance
        BigDecimal monthlyRate = new BigDecimal("0.0005");
        BigDecimal total = principal.toBigDecimal().multiply(monthlyRate).multiply(BigDecimal.valueOf(termMonths));
        return Money.of(total.setScale(2, RoundingMode.HALF_EVEN));
    }

    private BigDecimal calculateCollateralDiscount(CollateralType type, Money value) {
        return switch (type) {
            case REAL_ESTATE -> new BigDecimal("0.005");
            case VEHICLE -> new BigDecimal("0.003");
            case INVESTMENT -> new BigDecimal("0.002");
            default -> BigDecimal.ZERO;
        };
    }

    private Money calculateEarlySettlementDiscount(Loan loan, List<Installment> openInstallments) {
        // Simplified: 50% of remaining interest on future installments
        Money futureInterest = Money.ZERO;
        for (Installment inst : openInstallments) {
            futureInterest = futureInterest.add(inst.getRemainingInterest());
        }
        return Money.of(futureInterest.toBigDecimal().multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_EVEN));
    }

    private String generateContractNumber() {
        return "CTR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // ==================== COMMANDS ====================

    public record SimulateLoanCommand(
        UUID accountId,
        LoanType loanType,
        AmortizationType amortizationType,
        Money amount,
        BigDecimal annualRate,
        int termMonths,
        LocalDate firstDueDate
    ) {}

    public record SimulateLoanWithCollateralCommand(
        UUID accountId,
        LoanType loanType,
        AmortizationType amortizationType,
        Money amount,
        BigDecimal annualRate,
        int termMonths,
        LocalDate firstDueDate,
        CollateralType collateralType,
        Money collateralValue
    ) {}

    public record ContractLoanCommand(
        UUID accountId,
        UUID simulationId,
        String contractNumber
    ) {}

    public record ApproveLoanCommand(String approvedBy) {}

    public record RejectLoanCommand(String reason) {}

    public record PayInstallmentCommand(
        Money amount,
        UUID transactionId
    ) {}

    public record SettleLoanCommand(
        boolean applyDiscount,
        UUID transactionId
    ) {}

    public record RenegotiateLoanCommand(
        AmortizationType newAmortizationType,
        BigDecimal newInterestRate,
        Integer newTermMonths,
        LocalDate newFirstDueDate
    ) {}
}