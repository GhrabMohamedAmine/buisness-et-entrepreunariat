package controllers;

import entities.Project;
import entities.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import services.ProjectService;
import services.TaskService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarController {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final String[] DAY_HEADERS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final Pattern HOLIDAY_OBJECT_PATTERN = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);

    @FXML private GridPane calendarGrid;
    @FXML private Label monthLabel;

    private final ProjectService projectService = new ProjectService();
    private final TaskService taskService = new TaskService();
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final Map<Integer, List<HolidayEntry>> holidaysByYear = new HashMap<>();

    private YearMonth currentMonth = YearMonth.now();
    private String countryCode = "TN";

    private static class CalendarEntry {
        private final String label;
        private final String color;

        private CalendarEntry(String label, String color) {
            this.label = label;
            this.color = color;
        }
    }

    private static class HolidayEntry {
        private final LocalDate date;
        private final String name;

        private HolidayEntry(LocalDate date, String name) {
            this.date = date;
            this.name = name;
        }
    }

    @FXML
    private void initialize() {
        countryCode = "TN";
        configureGridColumns();
        renderMonth();
    }

    @FXML
    private void onPreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderMonth();
    }

    @FXML
    private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderMonth();
    }

    private void configureGridColumns() {
        calendarGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }
    }

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        if (monthLabel != null) {
            monthLabel.setText(currentMonth.format(MONTH_FORMATTER));
        }

        for (int col = 0; col < DAY_HEADERS.length; col++) {
            Label header = new Label(DAY_HEADERS[col]);
            header.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
            header.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(header, Priority.ALWAYS);
            calendarGrid.add(header, col, 0);
        }

        Map<LocalDate, List<CalendarEntry>> entriesByDate = loadEntriesForMonth(currentMonth);
        LocalDate firstDay = currentMonth.atDay(1);
        int shift = firstDay.getDayOfWeek().getValue() - 1;
        LocalDate cursor = firstDay.minusDays(shift);

        for (int row = 1; row <= 6; row++) {
            for (int col = 0; col < 7; col++) {
                VBox dayCell = buildDayCell(cursor, entriesByDate.getOrDefault(cursor, List.of()));
                calendarGrid.add(dayCell, col, row);
                cursor = cursor.plusDays(1);
            }
        }
    }

    private VBox buildDayCell(LocalDate date, List<CalendarEntry> entries) {
        VBox dayCell = new VBox(4);
        dayCell.setPadding(new Insets(8));
        dayCell.setMinHeight(110);
        dayCell.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        String dayColor = date.getMonth() == currentMonth.getMonth() ? "#111827" : "#9ca3af";
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + dayColor + ";");
        dayCell.getChildren().add(dayLabel);

        int maxVisible = Math.min(entries.size(), 3);
        for (int i = 0; i < maxVisible; i++) {
            CalendarEntry entry = entries.get(i);
            Label chip = new Label(entry.label);
            chip.setWrapText(true);
            chip.setStyle("-fx-font-size: 10; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 10; -fx-background-color: " + entry.color + ";");
            dayCell.getChildren().add(chip);
        }

        if (entries.size() > 3) {
            Label more = new Label("+" + (entries.size() - 3) + " more");
            more.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280;");
            dayCell.getChildren().add(more);
        }
        return dayCell;
    }

    private Map<LocalDate, List<CalendarEntry>> loadEntriesForMonth(YearMonth month) {
        Map<LocalDate, List<CalendarEntry>> entries = new HashMap<>();

        List<Project> projects = projectService.getAllProjects();
        for (Project project : projects) {
            addEntry(entries, parseIsoDate(project.getStartDate()), "Project start: " + project.getName(), "#4f46e5");
            addEntry(entries, parseIsoDate(project.getEndDate()), "Project due: " + project.getName(), "#7c3aed");

            List<Task> tasks = taskService.getTasksByProject(project.getId());
            for (Task task : tasks) {
                LocalDate start = parseIsoDate(task.getStartDate());
                LocalDate due = parseIsoDate(task.getDueDate());
                addEntry(entries, start, "Task start: " + task.getTitle(), "#0ea5e9");
                if (due != null && !due.equals(start)) {
                    addEntry(entries, due, "Task due: " + task.getTitle(), "#f97316");
                }
            }
        }

        for (HolidayEntry holiday : fetchHolidays(month.getYear(), countryCode)) {
            addEntry(entries, holiday.date, "Holiday: " + holiday.name, "#16a34a");
        }
        return entries;
    }

    private void addEntry(Map<LocalDate, List<CalendarEntry>> entries, LocalDate date, String label, String color) {
        if (date == null || label == null || label.isBlank()) {
            return;
        }
        entries.computeIfAbsent(date, d -> new ArrayList<>()).add(new CalendarEntry(label, color));
    }

    private LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<HolidayEntry> fetchHolidays(int year, String country) {
        if (holidaysByYear.containsKey(year)) {
            return holidaysByYear.get(year);
        }
        List<HolidayEntry> holidays = new ArrayList<>();
        try {
            String url = "https://date.nager.at/api/v3/PublicHolidays/2026/TN" ;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                holidays = parseHolidayJson(response.body());
            }
        } catch (Exception ignored) {
            holidays = new ArrayList<>();
        }
        holidaysByYear.put(year, holidays);
        return holidays;
    }

    private List<HolidayEntry> parseHolidayJson(String json) {
        List<HolidayEntry> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        Matcher objectMatcher = HOLIDAY_OBJECT_PATTERN.matcher(json);
        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);
            String dateValue = extractJsonField(object, "date");
            String localName = extractJsonField(object, "localName");
            String name = extractJsonField(object, "name");
            LocalDate date = parseIsoDate(dateValue);
            String label = (localName != null && !localName.isBlank()) ? localName : name;
            if (date != null && label != null && !label.isBlank()) {
                result.add(new HolidayEntry(date, label));
            }
        }
        return result;
    }

    private String extractJsonField(String object, String key) {
        Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = fieldPattern.matcher(object);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\"", "\"");
    }
}
