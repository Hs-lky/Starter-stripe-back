package com.example.springsaas.invoice.service;

import com.example.springsaas.invoice.entity.Invoice;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public byte[] generateInvoicePdf(Invoice invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add header
            Paragraph header = new Paragraph("INVOICE")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20);
            document.add(header);

            // Add invoice details
            document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
            document.add(new Paragraph("Date: " + invoice.getCreatedAt().format(DATE_FORMATTER)));
            document.add(new Paragraph("Due Date: " + invoice.getDueDate().format(DATE_FORMATTER)));
            document.add(new Paragraph("\n"));

            // Add company and customer details
            document.add(new Paragraph("Bill To:"));
            document.add(new Paragraph(invoice.getUser().getFirstName() + " " + invoice.getUser().getLastName()));
            document.add(new Paragraph(invoice.getUser().getEmail()));
            document.add(new Paragraph("\n"));

            // Create table for invoice items
            Table table = new Table(UnitValue.createPercentArray(new float[]{15, 55, 15, 15}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Add table headers
            table.addHeaderCell("Item");
            table.addHeaderCell("Description");
            table.addHeaderCell("Amount");
            table.addHeaderCell("Total");

            // Add subscription details
            table.addCell("Subscription");
            table.addCell("Monthly Subscription - " + invoice.getPayment().getSubscription().getPlan());
            table.addCell(formatCurrency(invoice.getAmount(), invoice.getCurrency()));
            table.addCell(formatCurrency(invoice.getAmount(), invoice.getCurrency()));

            document.add(table);
            document.add(new Paragraph("\n"));

            // Add total
            Paragraph total = new Paragraph(
                    "Total Amount: " + formatCurrency(invoice.getAmount(), invoice.getCurrency()))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold();
            document.add(total);

            // Add payment status
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Status: " + invoice.getStatus())
                    .setTextAlignment(TextAlignment.RIGHT));

            // Add footer
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Thank you for your business!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String formatCurrency(java.math.BigDecimal amount, String currency) {
        return currency + " " + amount.toString();
    }
} 