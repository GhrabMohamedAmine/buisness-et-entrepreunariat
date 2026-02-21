package services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import entities.Project;
import entities.Task;
import entities.User;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateTimeFormatter REPORT_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    private static final DeviceRgb COLOR_PURPLE = new DeviceRgb(124, 58, 237);
    private static final DeviceRgb COLOR_TEXT = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb COLOR_MUTED = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb COLOR_BORDER = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb COLOR_SOFT_BG = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb COLOR_GREEN = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb COLOR_RED = new DeviceRgb(220, 38, 38);

    public void exportProjectPerformanceReport(Project project,
                                               List<Task> tasks,
                                               List<User> members,
                                               Path outputFile) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            int totalTasks = tasks == null ? 0 : tasks.size();
            int completedTasks = countCompleted(tasks);
            int overdueTasks = countOverdue(tasks);
            double successRate = totalTasks == 0 ? 0.0 : (completedTasks * 100.0) / totalTasks;

            document.setMargins(28, 28, 28, 28);

            addHeader(document, project);
            addKpiRow(document, successRate, completedTasks, totalTasks, overdueTasks);

            addSectionTitle(document, "Membres de l'équipe");
            if (members == null || members.isEmpty()) {
                document.add(new Paragraph("Aucun membre assigné")
                        .setFontSize(11)
                        .setFontColor(COLOR_MUTED)
                        .setMarginTop(4)
                        .setMarginBottom(12));
            } else {
                com.itextpdf.layout.element.List memberList = new com.itextpdf.layout.element.List()
                        .setSymbolIndent(8)
                        .setListSymbol("\u2022")
                        .setFontSize(11)
                        .setFontColor(COLOR_TEXT)
                        .setMarginTop(4)
                        .setMarginBottom(14);
                for (User member : members) {
                    memberList.add(new ListItem(safe(member.getFullName())));
                }
                document.add(memberList);
            }

            addSectionTitle(document, "Tâches en retard");
            List<Task> overdueList = overdueTaskList(tasks);
            if (overdueList.isEmpty()) {
                document.add(new Paragraph("Aucune tâche en retard")
                        .setFontSize(11)
                        .setFontColor(COLOR_GREEN)
                        .setMarginTop(4)
                        .setMarginBottom(12));
            } else {
                document.add(buildOverdueTable(overdueList));
            }

            addSectionTitle(document, "Tâches Done");
            document.add(buildTaskStatusTable(filterByStatus(tasks, "DONE"), "Done"));

            addSectionTitle(document, "Tâches Undone (To Do)");
            document.add(buildTaskStatusTable(filterByStatus(tasks, "TODO"), "To Do"));

            addSectionTitle(document, "Tâches In Progress");
            document.add(buildTaskStatusTable(filterByStatus(tasks, "IN_PROGRESS"), "In Progress"));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Fin du rapport")
                    .setFontSize(9)
                    .setFontColor(COLOR_MUTED)
                    .setTextAlignment(TextAlignment.RIGHT));
        }
    }

    private void addHeader(Document document, Project project) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{72, 28}))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        Cell left = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Nexum")
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(COLOR_PURPLE)
                        .setMarginBottom(4))
                .add(new Paragraph("Rapport de Performance Projet")
                        .setFontSize(20)
                        .setBold()
                        .setFontColor(COLOR_TEXT)
                        .setMarginBottom(2))
                .add(new Paragraph("Projet: " + safe(project.getName()))
                        .setFontSize(12)
                        .setFontColor(COLOR_MUTED));

        Cell right = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Généré le")
                        .setFontSize(9)
                        .setFontColor(COLOR_MUTED)
                        .setMarginBottom(2))
                .add(new Paragraph(LocalDateTime.now().format(REPORT_TS_FORMAT))
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(COLOR_TEXT));

        header.addCell(left);
        header.addCell(right);
        document.add(header);
        document.add(new Paragraph("")
                .setBorderBottom(new SolidBorder(COLOR_BORDER, 1))
                .setMarginBottom(14));
    }

    private void addKpiRow(Document document, double successRate, int completedTasks, int totalTasks, int overdueTasks) {
        Table kpiTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        kpiTable.addCell(buildKpiCell("Taux de réussite", String.format(Locale.ENGLISH, "%.0f%%", successRate), COLOR_PURPLE));
        kpiTable.addCell(buildKpiCell("Tâches complétées", completedTasks + " / " + totalTasks, COLOR_GREEN));
        kpiTable.addCell(buildKpiCell("Tâches en retard", String.valueOf(overdueTasks), overdueTasks > 0 ? COLOR_RED : COLOR_GREEN));

        document.add(kpiTable);
    }

    private Cell buildKpiCell(String title, String value, DeviceRgb valueColor) {
        return new Cell()
                .setBackgroundColor(COLOR_SOFT_BG)
                .setBorder(new SolidBorder(COLOR_BORDER, 1))
                .setPadding(10)
                .add(new Paragraph(title)
                        .setFontSize(10)
                        .setFontColor(COLOR_MUTED)
                        .setMarginBottom(4))
                .add(new Paragraph(value)
                        .setFontSize(18)
                        .setBold()
                        .setFontColor(valueColor)
                        .setMarginTop(0));
    }

    private void addSectionTitle(Document document, String title) {
        document.add(new Paragraph(title)
                .setFontSize(13)
                .setBold()
                .setFontColor(COLOR_TEXT)
                .setMarginTop(4)
                .setMarginBottom(6));
    }

    private Table buildOverdueTable(List<Task> overdueList) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{58, 22, 20}))
                .useAllAvailableWidth()
                .setMarginTop(4)
                .setMarginBottom(10);

        table.addHeaderCell(headerCell("Tâche"));
        table.addHeaderCell(headerCell("Date d'échéance"));
        table.addHeaderCell(headerCell("Statut"));

        for (Task task : overdueList) {
            table.addCell(bodyCell(safe(task.getTitle())));
            table.addCell(bodyCell(safe(task.getDueDate())));
            table.addCell(bodyCell("Overdue").setFontColor(COLOR_RED).setBold());
        }
        return table;
    }

    private Table buildTaskStatusTable(List<Task> tasks, String statusLabel) {
        if (tasks == null || tasks.isEmpty()) {
            Table emptyTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setMarginTop(4)
                    .setMarginBottom(10);
            emptyTable.addCell(new Cell()
                    .setBorder(new SolidBorder(COLOR_BORDER, 1))
                    .setPadding(8)
                    .add(new Paragraph("Aucune tâche")
                            .setFontSize(10)
                            .setFontColor(COLOR_MUTED)));
            return emptyTable;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{55, 25, 20}))
                .useAllAvailableWidth()
                .setMarginTop(4)
                .setMarginBottom(10);

        table.addHeaderCell(headerCell("Tâche"));
        table.addHeaderCell(headerCell("Date d'échéance"));
        table.addHeaderCell(headerCell("Statut"));

        for (Task task : tasks) {
            table.addCell(bodyCell(safe(task.getTitle())));
            table.addCell(bodyCell(safe(task.getDueDate())));
            table.addCell(bodyCell(statusLabel));
        }
        return table;
    }

    private Cell headerCell(String text) {
        return new Cell()
                .setBackgroundColor(COLOR_PURPLE)
                .setBorder(new SolidBorder(COLOR_PURPLE, 1))
                .setPadding(8)
                .add(new Paragraph(text)
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(ColorConstants.WHITE))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    }

    private Cell bodyCell(String text) {
        return new Cell()
                .setBorder(new SolidBorder(COLOR_BORDER, 1))
                .setPadding(8)
                .add(new Paragraph(text)
                        .setFontSize(10)
                        .setFontColor(COLOR_TEXT))
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    }

    private int countCompleted(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0;
        int count = 0;
        for (Task task : tasks) {
            if (isDone(task)) count++;
        }
        return count;
    }

    private int countOverdue(List<Task> tasks) {
        return overdueTaskList(tasks).size();
    }

    private List<Task> overdueTaskList(List<Task> tasks) {
        List<Task> overdue = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) return overdue;
        LocalDate today = LocalDate.now();
        for (Task task : tasks) {
            LocalDate due = parseDate(task.getDueDate());
            if (due != null && due.isBefore(today) && !isDone(task)) {
                overdue.add(task);
            }
        }
        return overdue;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDone(Task task) {
        if (task == null || task.getStatus() == null) return false;
        String status = task.getStatus().trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return "DONE".equals(status);
    }

    private List<Task> filterByStatus(List<Task> tasks, String targetStatus) {
        List<Task> filtered = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) return filtered;
        String normalizedTarget = targetStatus == null ? "" : targetStatus.trim().toUpperCase(Locale.ROOT);

        for (Task task : tasks) {
            if (task == null || task.getStatus() == null) continue;
            String normalized = task.getStatus().trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            if ("TODO".equals(normalizedTarget) && ("TODO".equals(normalized) || "TO_DO".equals(normalized))) {
                filtered.add(task);
                continue;
            }
            if (normalizedTarget.equals(normalized)) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
