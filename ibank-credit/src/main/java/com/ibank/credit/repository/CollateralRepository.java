package com.ibank.credit.repository;

import com.ibank.credit.model.Collateral;
import com.ibank.credit.model.CollateralType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollateralRepository extends JpaRepository<Collateral, UUID> {

    List<Collateral> findByLoanId(UUID loanId);

    List<Collateral> findByCollateralType(CollateralType collateralType);
}