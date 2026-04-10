package org.aitu.vulnerabilitiesmvp.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.aitu.vulnerabilitiesmvp.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIdAndOwnerId(Long id, Long ownerId);

    List<Account> findAllByOwnerIdOrderByIdAsc(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.owner where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
