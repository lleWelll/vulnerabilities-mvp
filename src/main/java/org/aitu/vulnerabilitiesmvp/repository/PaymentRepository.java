package org.aitu.vulnerabilitiesmvp.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.aitu.vulnerabilitiesmvp.entity.Payment;
import org.aitu.vulnerabilitiesmvp.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"ownerUser", "ownerAccount", "receiverUser", "receiverAccount"})
    Page<Payment> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId, Pageable pageable);

    @EntityGraph(attributePaths = {"ownerUser", "ownerAccount", "receiverUser", "receiverAccount"})
    Optional<Payment> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Override
    @EntityGraph(attributePaths = {"ownerUser", "ownerAccount", "receiverUser", "receiverAccount"})
    Optional<Payment> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from Payment p
        join fetch p.ownerUser
        join fetch p.ownerAccount
        join fetch p.receiverUser
        join fetch p.receiverAccount
        where p.id = :id
        """)
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"ownerUser", "ownerAccount", "receiverUser", "receiverAccount"})
    @Query(
        value = """
            select p from Payment p
            where p.ownerUser.id = :ownerUserId
              and (:status is null or p.status = :status)
              and (:receiverUsername is null or lower(p.receiverUser.username) = :receiverUsername)
            order by p.createdAt desc
            """,
        countQuery = """
            select count(p) from Payment p
            where p.ownerUser.id = :ownerUserId
              and (:status is null or p.status = :status)
              and (:receiverUsername is null or lower(p.receiverUser.username) = :receiverUsername)
            """
    )
    Page<Payment> findHistoryByOwnerAndFilters(
        @Param("ownerUserId") Long ownerUserId,
        @Param("status") PaymentStatus status,
        @Param("receiverUsername") String receiverUsername,
        Pageable pageable
    );

    long countByOwnerUserIdAndCreatedAtAfter(Long ownerUserId, Instant cutoff);
}
