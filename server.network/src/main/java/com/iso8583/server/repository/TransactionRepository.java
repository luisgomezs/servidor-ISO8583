package com.iso8583.server.repository;

import com.iso8583.server.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    // Spring Data JPA provee automáticamente métodos como save(), findAll(), findById(), etc.
}