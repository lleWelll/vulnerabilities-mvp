package org.aitu.vulnerabilitiesmvp.repository;

import org.aitu.vulnerabilitiesmvp.entity.FraudFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {

    boolean existsByPaymentIdAndReason(Long paymentId, String reason);

    @Override
    @EntityGraph(attributePaths = {"payment", "payment.ownerUser", "payment.receiverUser", "flaggedBy"})
    Page<FraudFlag> findAll(Pageable pageable);
}
