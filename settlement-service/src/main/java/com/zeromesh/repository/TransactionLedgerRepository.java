package com.zeromesh.repository;

import com.zeromesh.model.TransactionLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLedgerRepository extends JpaRepository<TransactionLedger, Long> {

    Optional<TransactionLedger> findByPacketId(String packetId);

    boolean existsByPacketId(String packetId);
}
