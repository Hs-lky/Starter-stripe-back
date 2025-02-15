package com.example.springsaas.invoice.entity;

import com.example.springsaas.authentication.entity.User;
import com.example.springsaas.common.entity.BaseEntity;
import com.example.springsaas.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column
    private LocalDateTime paidAt;

    @Column
    private String pdfUrl;

    @Column
    private boolean emailSent;

    @Column
    private LocalDateTime emailSentAt;

    public enum InvoiceStatus {
        DRAFT,
        SENT,
        PAID,
        VOID,
        OVERDUE
    }
} 