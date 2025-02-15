package com.example.springsaas.invoice.repository;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.invoice.entity.Invoice;
import com.example.springsaas.invoice.entity.Invoice.InvoiceStatus;
import com.example.springsaas.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByUser(User user);
    List<Invoice> findAllByUserAndStatus(User user, InvoiceStatus status);
    Optional<Invoice> findByPayment(Payment payment);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findAllByStatusAndDueDateBefore(InvoiceStatus status, LocalDateTime date);
    List<Invoice> findAllByEmailSentFalseAndStatus(InvoiceStatus status);
} 