package com.ibank.account.repository;

import com.ibank.account.model.Account;
import com.ibank.core.model.AccountStatus;
import com.ibank.core.model.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByHolderCpfAndAccountType(String cpf, AccountType accountType);

    List<Account> findByHolderCpf(String cpf);

    List<Account> findByStatus(AccountStatus status);

    Page<Account> findByHolderCpf(String cpf, Pageable pageable);

    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE " +
           "(:cpf IS NULL OR a.holderCpf = :cpf) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:accountType IS NULL OR a.accountType = :accountType) AND " +
           "(:branch IS NULL OR a.branch = :branch)")
    Page<Account> findWithFilters(
            @Param("cpf") String cpf,
            @Param("status") AccountStatus status,
            @Param("accountType") AccountType accountType,
            @Param("branch") String branch,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    long countByStatus(AccountStatus status);

    @Query("SELECT SUM(a.balance.amount) FROM Account a WHERE a.status = :status")
    BigDecimal sumBalanceByStatus(AccountStatus status);

    boolean existsByHolderCpfAndAccountType(String cpf, AccountType accountType);

    Optional<Account> findByIdAndStatus(UUID id, AccountStatus status);
}