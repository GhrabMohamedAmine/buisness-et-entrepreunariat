package utils;

import entities.Resource;
import entities.User;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RequestPdfGenerator {

    private static final Color ACCENT = new Color(124, 58, 237);
    private static final Color TEXT = new Color(17, 24, 39);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color BG = new Color(245, 246, 250);
    private static final Color BORDER = new Color(229, 231, 235);

    public static void generateReceipt(
            File outFile,
            int assignmentId,
            User user,
            String projectName,
            String projectCode,
            Resource resource,
            int qty,
            double totalCost,
            String status
    ) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float W = page.getMediaBox().getWidth();
            float H = page.getMediaBox().getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // background
                fill(cs, 0, 0, W, H, BG);

                float margin = 42;
                float cardX = margin, cardY = margin;
                float cardW = W - margin * 2, cardH = H - margin * 2;

                // card
                fill(cs, cardX, cardY, cardW, cardH, Color.WHITE);
                stroke(cs, cardX, cardY, cardW, cardH, BORDER, 1);

                float x = cardX + 28;
                float y = cardY + cardH - 32;

                // top accent
                fill(cs, cardX, y + 8, cardW, 4, ACCENT);

                text(cs, "Resource Request Receipt", x, y, PDType1Font.HELVETICA_BOLD, 20, TEXT);

                y -= 18;
                text(cs, "Your request has been submitted successfully.", x, y, PDType1Font.HELVETICA, 11, MUTED);

                // meta right
                String rid = "REQ-" + assignmentId;
                String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                float right = cardX + cardW - 28;
                textRight(cs, "Request ID", right, cardY + cardH - 40, PDType1Font.HELVETICA, 10, MUTED);
                textRight(cs, rid, right, cardY + cardH - 56, PDType1Font.HELVETICA_BOLD, 12, TEXT);

                textRight(cs, "Date", right, cardY + cardH - 78, PDType1Font.HELVETICA, 10, MUTED);
                textRight(cs, dt, right, cardY + cardH - 94, PDType1Font.HELVETICA_BOLD, 12, TEXT);

                y -= 26;
                line(cs, cardX + 20, y, cardX + cardW - 20, y, BORDER, 1);

                // client
                y -= 22;
                text(cs, "Client", x, y, PDType1Font.HELVETICA_BOLD, 12, TEXT);
                y -= 14;
                text(cs, "Name: " + safe(user == null ? null : user.getFullName()), x, y, PDType1Font.HELVETICA, 11, TEXT);
                y -= 14;
                text(cs, "Email: " + safe(user == null ? null : user.getEmail()), x, y, PDType1Font.HELVETICA, 11, TEXT);

                // details
                y -= 26;
                text(cs, "Request Details", x, y, PDType1Font.HELVETICA_BOLD, 12, TEXT);
                y -= 18;

                y = row(cs, x, y, cardW - 56, "Project", safe(projectName) + " (ID: " + safe(projectCode) + ")");
                y = row(cs, x, y, cardW - 56, "Resource", safe(resource == null ? null : resource.getName()));
                y = row(cs, x, y, cardW - 56, "Quantity", String.valueOf(qty));
                y = row(cs, x, y, cardW - 56, "Total Cost", String.format("%.2f", totalCost));
                y = row(cs, x, y, cardW - 56, "Status", safe(status));

                // footer
                float fy = cardY + 26;
                line(cs, cardX + 20, fy + 18, cardX + cardW - 20, fy + 18, BORDER, 1);
                text(cs, "Thank you. This PDF can be used as proof of request submission.", x, fy, PDType1Font.HELVETICA, 10, MUTED);
            }

            doc.save(outFile);
        }
    }

    private static float row(PDPageContentStream cs, float x, float y, float w, String k, String v) throws IOException {
        stroke(cs, x, y - 22, w, 24, BORDER, 1);
        text(cs, k, x + 10, y - 16, PDType1Font.HELVETICA_BOLD, 11, MUTED);
        text(cs, v, x + w * 0.34f, y - 16, PDType1Font.HELVETICA, 11, TEXT);
        return y - 26;
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private static void text(PDPageContentStream cs, String t, float x, float y,
                             PDType1Font f, float size, Color c) throws IOException {
        cs.beginText();
        cs.setFont(f, size);
        cs.setNonStrokingColor(c);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private static void textRight(PDPageContentStream cs, String t, float rightX, float y,
                                  PDType1Font f, float size, Color c) throws IOException {
        float w = f.getStringWidth(t) / 1000f * size;
        text(cs, t, rightX - w, y, f, size, c);
    }

    private static void fill(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void stroke(PDPageContentStream cs, float x, float y, float w, float h, Color c, float lw) throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(lw);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void line(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color c, float lw) throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(lw);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }
}