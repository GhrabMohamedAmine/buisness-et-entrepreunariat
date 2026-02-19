package utils;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import entities.Formation;
import entities.Resultat;
import services.FormationService;
import services.UserService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CertificateGenerator {

    private static final DeviceRgb GOLD_COLOR = new DeviceRgb(212, 175, 55);
    private static final DeviceRgb DARK_BLUE = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(245, 245, 245);

    public static File generate(Resultat resultat) throws Exception {

        FormationService formationService = new FormationService();
        Formation formation = formationService.getById(resultat.getFormationId());

        String userName = UserService.getCurrentUser().getName() + " "
                + UserService.getCurrentUser().getFirstName();

        String path = System.getProperty("user.home") + "/pi_kavafx/certificats/";
        new File(path).mkdirs();

        String certificateId = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fileName = certificateId + ".pdf";
        String fullPath = path + fileName;

        PdfWriter writer = new PdfWriter(fullPath);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4.rotate());
        pdf.addNewPage();

        Document document = new Document(pdf);
        document.setMargins(50, 50, 50, 50);

        // ===== Elegant Border with Corner Accents =====
        PdfCanvas canvas = new PdfCanvas(pdf.getFirstPage());

        // Outer border
        canvas.setLineWidth(2f);
        canvas.setStrokeColor(DARK_BLUE);
        canvas.rectangle(25, 25,
                pdf.getDefaultPageSize().getWidth() - 50,
                pdf.getDefaultPageSize().getHeight() - 50);
        canvas.stroke();

        // Inner decorative border
        canvas.setLineWidth(0.5f);
        canvas.setStrokeColor(GOLD_COLOR);
        canvas.rectangle(35, 35,
                pdf.getDefaultPageSize().getWidth() - 70,
                pdf.getDefaultPageSize().getHeight() - 70);
        canvas.stroke();

        // Corner decorations
        float cornerSize = 30;
        float pageWidth = pdf.getDefaultPageSize().getWidth();
        float pageHeight = pdf.getDefaultPageSize().getHeight();

        drawCornerDecoration(canvas, 25, 25, cornerSize, DARK_BLUE);
        drawCornerDecoration(canvas, pageWidth - 25, 25, cornerSize, DARK_BLUE);
        drawCornerDecoration(canvas, 25, pageHeight - 25, cornerSize, DARK_BLUE);
        drawCornerDecoration(canvas, pageWidth - 25, pageHeight - 25, cornerSize, DARK_BLUE);

        // ===== Header with Gold Line =====
        document.add(new Paragraph("\n"));

        // Gold decorative line
        Table headerLine = new Table(1).setWidth(UnitValue.createPercentValue(40));
        headerLine.addCell(new Cell()
                .setHeight(3)
                .setBackgroundColor(GOLD_COLOR)
                .setBorder(Border.NO_BORDER));
        document.add(headerLine.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));

        document.add(new Paragraph("\n"));

        // ===== Title =====
        document.add(new Paragraph("CERTIFICATE OF ACHIEVEMENT")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(36)
                .setBold()
                .setFontColor(DARK_BLUE)
                );

        document.add(new Paragraph("\n"));

        // ===== Subtitle with Elegant Font =====
        document.add(new Paragraph("This certificate is proudly presented to")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setFontColor(ColorConstants.GRAY));

        document.add(new Paragraph("\n"));

        // ===== Student Name with Gold underline =====
        Paragraph namePara = new Paragraph(userName)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(42)
                .setBold()
                .setFontColor(DARK_BLUE);
        document.add(namePara);

        // Gold underline for name
        Table nameUnderline = new Table(1).setWidth(UnitValue.createPercentValue(50));
        nameUnderline.addCell(new Cell()
                .setHeight(2)
                .setBackgroundColor(GOLD_COLOR)
                .setBorder(Border.NO_BORDER));
        document.add(nameUnderline.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));

        document.add(new Paragraph("\n"));

        // ===== Course Information =====
        document.add(new Paragraph("for successfully completing the course")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setFontColor(ColorConstants.GRAY));

        document.add(new Paragraph(formation.getTitre())
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(26)
                .setBold()
                .setFontColor(DARK_BLUE));

        document.add(new Paragraph("\n"));

        // ===== Score with Progress Bar =====
        double percent = (resultat.getScore() * 100.0) / resultat.getTotal();
        String scoreText = String.format("Final Score: %d/%d (%.0f%%)",
                resultat.getScore(), resultat.getTotal(), percent);

        document.add(new Paragraph(scoreText)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18)
                .setFontColor(DARK_BLUE)
                .setBold());

        // Visual score indicator (simulated progress bar)
        Table progressContainer = new Table(2).setWidth(UnitValue.createPercentValue(40));
        progressContainer.addCell(new Cell()
                .setWidth(UnitValue.createPercentValue((float) percent))
                .setHeight(8)
                .setBackgroundColor(GOLD_COLOR)
                .setBorder(Border.NO_BORDER));
        progressContainer.addCell(new Cell()
                .setWidth(UnitValue.createPercentValue(100 - (float) percent))
                .setHeight(8)
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(Border.NO_BORDER));
        document.add(progressContainer.setHorizontalAlignment(
                com.itextpdf.layout.properties.HorizontalAlignment.CENTER));

        document.add(new Paragraph("\n\n"));

        // ===== Date and Certificate ID in Elegant Format =====
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth();

        String date = new SimpleDateFormat("dd MMMM yyyy").format(new Date());

        // Date
        Cell dateCell = new Cell()
                .add(new Paragraph("Date"))
                .add(new Paragraph(date))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        infoTable.addCell(dateCell);

        // Seal/Emblem in center
        Cell emblemCell = new Cell()
                .add(new Paragraph("✦"))
                .setFontSize(24)
                .setFontColor(GOLD_COLOR)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        infoTable.addCell(emblemCell);

        // Certificate ID
        Cell idCell = new Cell()
                .add(new Paragraph("Certificate ID"))
                .add(new Paragraph(certificateId))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        infoTable.addCell(idCell);

        document.add(infoTable);

        document.add(new Paragraph("\n\n\n"));

        // ===== Signatures with Fancy Style =====
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth();

        // Instructor
        signatureTable.addCell(createSignatureCell("Instructor", "_________________________", "Dr. Jean Dupont"));

        // Empty cell for spacing
        signatureTable.addCell(new Cell().setBorder(Border.NO_BORDER));

        // Director
        signatureTable.addCell(createSignatureCell("Director", "_________________________", "Prof. Marie Martin"));

        document.add(signatureTable);

        document.add(new Paragraph("\n"));

        // ===== Footer =====
        document.add(new Paragraph("This certificate is a testament to the hard work and dedication demonstrated.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10)
                .setFontColor(ColorConstants.LIGHT_GRAY));

        document.close();

        return new File(fullPath);
    }

    private static void drawCornerDecoration(PdfCanvas canvas, float x, float y, float size, DeviceRgb color) {
        canvas.saveState();
        canvas.setLineWidth(2f);
        canvas.setStrokeColor(color);

        // Top-left corner
        if (x < 100 && y < 100) {
            canvas.moveTo(x, y + size).lineTo(x, y).lineTo(x + size, y);
        }
        // Top-right corner
        else if (x > 700 && y < 100) {
            canvas.moveTo(x - size, y).lineTo(x, y).lineTo(x, y + size);
        }
        // Bottom-left corner
        else if (x < 100 && y > 500) {
            canvas.moveTo(x, y - size).lineTo(x, y).lineTo(x + size, y);
        }
        // Bottom-right corner
        else if (x > 700 && y > 500) {
            canvas.moveTo(x - size, y).lineTo(x, y).lineTo(x, y - size);
        }

        canvas.stroke();
        canvas.restoreState();
    }

    private static Cell createSignatureCell(String title, String line, String name) {
        return new Cell()
                .add(new Paragraph(title)
                        .setFontSize(12)
                        .setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(line)
                        .setFontSize(16)
                        .setFontColor(DARK_BLUE))
                .add(new Paragraph(name)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.DARK_GRAY))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(10);
    }
}