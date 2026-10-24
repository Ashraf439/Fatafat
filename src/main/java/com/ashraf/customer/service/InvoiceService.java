package com.ashraf.customer.service;

import com.ashraf.core.entity.User;
import com.ashraf.customer.dto.OrderResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class InvoiceService {
    private final CustomerOrderService orderService;

    public InvoiceService(CustomerOrderService orderService) {
        this.orderService = orderService;
    }

    public byte[] generate(User user, UUID orderId) {
        OrderResponse o = orderService.get(user, orderId);
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 54);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font normal = new Font(Font.HELVETICA, 10);

            doc.add(new Paragraph("Invoice — Order #" + o.orderNumber(), title));
            doc.add(new Paragraph(o.restaurant().name(), normal));
            doc.add(new Paragraph("Placed: " + o.placedAt(), normal));
            doc.add(new Paragraph("Status: " + o.status() + "  |  Payment: " + o.paymentMethod()
                    + " (" + o.paymentStatus() + ")", normal));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1, 2, 2});
            for (String h : new String[]{"Item", "Qty", "Unit price", "Line total"}) {
                PdfPCellHelper.header(table, h, bold);
            }
            for (var item : o.items()) {
                table.addCell(new Phrase(item.dishName(), normal));
                table.addCell(new Phrase(String.valueOf(item.quantity()), normal));
                table.addCell(new Phrase("₹" + item.unitPrice(), normal));
                table.addCell(new Phrase("₹" + item.lineTotal(), normal));
            }
            doc.add(table);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Subtotal: ₹" + o.subtotal(), normal));
            doc.add(new Paragraph("Delivery fee: ₹" + o.deliveryFee(), normal));
            doc.add(new Paragraph("Tax: ₹" + o.taxAmount(), normal));
            if (o.discountAmount() != null) doc.add(new Paragraph("Discount: -₹" + o.discountAmount(), normal));
            doc.add(new Paragraph("Total: ₹" + o.totalAmount(), bold));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate invoice: " + e.getMessage(), e);
        }
    }

    private static final class PdfPCellHelper {
        static void header(PdfPTable table, String text, Font font) {
            var cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(text, font));
            cell.setGrayFill(0.9f);
            table.addCell(cell);
        }
    }
}