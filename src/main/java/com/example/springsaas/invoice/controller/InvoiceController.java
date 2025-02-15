package com.example.springsaas.invoice.controller;

import com.example.springsaas.invoice.entity.Invoice;
import com.example.springsaas.invoice.service.InvoiceService;
import com.example.springsaas.invoice.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfService pdfService;

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoice(id);
        byte[] pdfData = pdfService.generateInvoicePdf(invoice);

        ByteArrayResource resource = new ByteArrayResource(pdfData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfData.length)
                .body(resource);
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendInvoice(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoice(id);
        invoiceService.sendInvoice(invoice);
        return ResponseEntity.ok().build();
    }
} 