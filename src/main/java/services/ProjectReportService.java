package services;

import entities.Project;
import entities.Task;
import entities.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateTimeFormatter REPORT_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
    private static final int LINES_PER_PAGE = 52;
    private static final int LEFT_MARGIN = 40;
    private static final int START_Y = 805;
    private static final int LINE_STEP = 14;

    public void exportProjectPerformanceReport(Project project,
                                               List<Task> tasks,
                                               List<User> members,
                                               Path outputFile) throws IOException {
        int totalTasks = tasks == null ? 0 : tasks.size();
        int completedTasks = countCompleted(tasks);
        int overdueTasks = countOverdue(tasks);
        double successRate = totalTasks == 0 ? 0.0 : (completedTasks * 100.0) / totalTasks;

        List<String> lines = new ArrayList<>();
        addLine(lines, "NEXUM");
        addLine(lines, "Project Performance Report");
        addLine(lines, "Project: " + safeProjectName(project));
        addLine(lines, "Generated: " + LocalDateTime.now().format(REPORT_TS_FORMAT));
        addLine(lines, repeat('-', 74));
        addLine(lines, String.format(Locale.ENGLISH, "Success Rate: %.0f%%", successRate));
        addLine(lines, "Completed Tasks: " + completedTasks + " / " + totalTasks);
        addLine(lines, "Overdue Tasks: " + overdueTasks);
        addLine(lines, "");

        addLine(lines, "Team Members");
        if (members == null || members.isEmpty()) {
            addLine(lines, "- None assigned");
        } else {
            for (User member : members) {
                addLine(lines, "- " + safe(member == null ? null : member.getFullName()));
            }
        }
        addLine(lines, "");

        addLine(lines, "Overdue Tasks");
        addTaskSection(lines, overdueTaskList(tasks), "Overdue");
        addLine(lines, "");

        addLine(lines, "Tasks Done");
        addTaskSection(lines, filterByStatus(tasks, "DONE"), "Done");
        addLine(lines, "");

        addLine(lines, "Tasks Undone (To Do)");
        addTaskSection(lines, filterByStatus(tasks, "TODO"), "To Do");
        addLine(lines, "");

        addLine(lines, "Tasks In Progress");
        addTaskSection(lines, filterByStatus(tasks, "IN_PROGRESS"), "In Progress");
        addLine(lines, "");
        addLine(lines, "End of report");

        byte[] pdfData = buildPdfFromLines(lines);
        java.nio.file.Files.write(outputFile, pdfData);
    }

    private void addTaskSection(List<String> lines, List<Task> tasks, String statusLabel) {
        if (tasks == null || tasks.isEmpty()) {
            addLine(lines, "No tasks");
            return;
        }
        addLine(lines, "Title | Due Date | Status");
        addLine(lines, repeat('-', 74));
        for (Task task : tasks) {
            String title = clip(safe(task == null ? null : task.getTitle()), 36);
            String dueDate = clip(safe(task == null ? null : task.getDueDate()), 12);
            addLine(lines, title + " | " + dueDate + " | " + statusLabel);
        }
    }

    private byte[] buildPdfFromLines(List<String> lines) {
        List<List<String>> pages = paginate(lines, LINES_PER_PAGE);
        List<byte[]> objects = new ArrayList<>();

        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>\n"));

        int pageCount = pages.size();
        int firstPageObject = 3;
        int pagesKidsStart = firstPageObject;
        int pagesContentStart = firstPageObject + pageCount;
        StringBuilder pagesNode = new StringBuilder("<< /Type /Pages /Count ")
                .append(pageCount)
                .append(" /Kids [");
        for (int i = 0; i < pageCount; i++) {
            pagesNode.append(' ').append(pagesKidsStart + i).append(" 0 R");
        }
        pagesNode.append(" ] >>\n");
        objects.add(ascii(pagesNode.toString()));

        for (int i = 0; i < pageCount; i++) {
            int contentObject = pagesContentStart + i;
            String pageObject = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 " + (pagesContentStart + pageCount) + " 0 R >> >> "
                    + "/Contents " + contentObject + " 0 R >>\n";
            objects.add(ascii(pageObject));
        }

        for (List<String> pageLines : pages) {
            String stream = buildPageContentStream(pageLines);
            byte[] streamBytes = ascii(stream);
            String header = "<< /Length " + streamBytes.length + " >>\nstream\n";
            String footer = "\nendstream\n";
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            writeAscii(obj, header);
            writeBytes(obj, streamBytes);
            writeAscii(obj, footer);
            objects.add(obj.toByteArray());
        }

        objects.add(ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n"));

        return assemblePdf(objects);
    }

    private String buildPageContentStream(List<String> lines) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 11 Tf\n");
        int y = START_Y;
        for (String line : lines) {
            stream.append("1 0 0 1 ")
                    .append(LEFT_MARGIN)
                    .append(' ')
                    .append(y)
                    .append(" Tm\n(")
                    .append(escapePdfText(sanitizeAscii(line)))
                    .append(") Tj\n");
            y -= LINE_STEP;
        }
        stream.append("ET\n");
        return stream.toString();
    }

    private byte[] assemblePdf(List<byte[]> objects) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

        List<Integer> xrefOffsets = new ArrayList<>();
        xrefOffsets.add(0);

        for (int i = 0; i < objects.size(); i++) {
            xrefOffsets.add(out.size());
            writeAscii(out, (i + 1) + " 0 obj\n");
            writeBytes(out, objects.get(i));
            writeAscii(out, "endobj\n");
        }

        int xrefStart = out.size();
        writeAscii(out, "xref\n0 " + (objects.size() + 1) + "\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (int i = 1; i < xrefOffsets.size(); i++) {
            writeAscii(out, String.format(Locale.ENGLISH, "%010d 00000 n %n", xrefOffsets.get(i)));
        }

        writeAscii(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
        writeAscii(out, "startxref\n" + xrefStart + "\n%%EOF\n");
        return out.toByteArray();
    }

    private List<List<String>> paginate(List<String> lines, int linesPerPage) {
        List<List<String>> pages = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            List<String> single = new ArrayList<>();
            single.add("Empty report");
            pages.add(single);
            return pages;
        }
        int index = 0;
        while (index < lines.size()) {
            int end = Math.min(index + linesPerPage, lines.size());
            pages.add(new ArrayList<>(lines.subList(index, end)));
            index = end;
        }
        return pages;
    }

    private void addLine(List<String> lines, String line) {
        lines.add(safe(line));
    }

    private String safeProjectName(Project project) {
        if (project == null) return "Unknown";
        return safe(project.getName());
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.trim();
    }

    private String clip(String value, int maxLen) {
        if (value == null) return "-";
        if (value.length() <= maxLen) return value;
        if (maxLen < 4) return value.substring(0, maxLen);
        return value.substring(0, maxLen - 3) + "...";
    }

    private String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }

    private String sanitizeAscii(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("\\p{M}+", "");
        return withoutDiacritics.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String escapePdfText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private void writeAscii(ByteArrayOutputStream out, String text) {
        writeBytes(out, ascii(text));
    }

    private void writeBytes(ByteArrayOutputStream out, byte[] data) {
        try {
            out.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected in-memory stream write error", e);
        }
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


}
