package com.example.springsaas.invoice.service;

import com.example.springsaas.email.service.EmailService;
import com.example.springsaas.invoice.entity.Invoice;
import com.example.springsaas.invoice.entity.Invoice.InvoiceStatus;
import com.example.springsaas.invoice.repository.InvoiceRepository;
import com.example.springsaas.payment.entity.Payment;
import com.example.springsaas.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Transactional
    public Invoice createInvoice(Payment payment) {
        Invoice invoice = new Invoice();
        invoice.setUser(payment.getUser());
        invoice.setPayment(payment);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setAmount(payment.getAmount());
        invoice.setCurrency(payment.getCurrency());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setDueDate(LocalDateTime.now().plusDays(30));

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void markInvoiceAsPaid(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void sendInvoice(Invoice invoice) {
        byte[] pdfData = pdfService.generateInvoicePdf(invoice);
        
        emailService.sendInvoiceEmail(
            invoice.getUser().getEmail(),
            invoice.getUser().getFirstName(),
            invoice.getInvoiceNumber(),
            pdfData
        );

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setEmailSent(true);
        invoice.setEmailSentAt(LocalDateTime.now());
        invoice.setPdfUrl("invoices/" + invoice.getInvoiceNumber() + ".pdf"); // You would store this in a file storage service
        
        invoiceRepository.save(invoice);
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
    @Transactional
    public void checkOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository
                .findAllByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDateTime.now());
        
        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            // You could send overdue notification emails here
        }
    }

    @Scheduled(cron = "0 0 2 1 * ?") // Run at 2 AM on the first day of each month
    @Transactional
    public void generateMonthlyInvoices() {
        LocalDateTime startOfLastMonth = LocalDateTime.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
        
        LocalDateTime endOfLastMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .minusSeconds(1);

        List<Payment> payments = paymentRepository.findAllByStatusAndCreatedAtBetween(
                Payment.PaymentStatus.SUCCEEDED,
                startOfLastMonth,
                endOfLastMonth
        );

        for (Payment payment : payments) {
            Invoice invoice = createInvoice(payment);
            sendInvoice(invoice);
        }
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 