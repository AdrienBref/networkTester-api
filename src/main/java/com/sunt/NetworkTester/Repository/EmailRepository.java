package com.sunt.NetworkTester.Repository;

import com.sunt.NetworkTester.Entity.EmailEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<EmailEntity, UUID> {
    // Trae solo los emails (más eficiente para comparar)
    @Query("select e.email from EmailEntity e")
    List<String> findAllEmails();

    // Borrado en bulk por lista (método derivado estándar JPA)
    @Transactional
    void deleteAllByEmailIn(Collection<String> emails);
}

