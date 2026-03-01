package services.ai;

import entities.Resource;
import services.ResourceService;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class AIRecommendationService {

    private final ResourceService resourceService = new ResourceService();

    // Keyword -> (type preference, score boost)
    // You can expand this list to look more "AI"
    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("web", new Rule("SOFTWARE", 30)),
            Map.entry("website", new Rule("SOFTWARE", 30)),
            Map.entry("mobile", new Rule("SOFTWARE", 30)),
            Map.entry("app", new Rule("SOFTWARE", 20)),
            Map.entry("database", new Rule("SOFTWARE", 25)),
            Map.entry("hosting", new Rule("SOFTWARE", 25)),
            Map.entry("server", new Rule("PHYSICAL", 25)),
            Map.entry("iot", new Rule("PHYSICAL", 35)),
            Map.entry("arduino", new Rule("PHYSICAL", 40)),
            Map.entry("raspberry", new Rule("PHYSICAL", 40)),
            Map.entry("robot", new Rule("PHYSICAL", 35)),
            Map.entry("network", new Rule("PHYSICAL", 25)),
            Map.entry("security", new Rule("SOFTWARE", 20)),
            Map.entry("ai", new Rule("SOFTWARE", 25)),
            Map.entry("ml", new Rule("SOFTWARE", 25)),
            Map.entry("training", new Rule("SOFTWARE", 15))
    );

    public List<Recommendation> recommend(String projectDescription, int topK) throws SQLException {
        String text = normalize(projectDescription);

        List<Resource> all = resourceService.getAll();

        List<Recommendation> scored = new ArrayList<>();
        for (Resource r : all) {
            int score = scoreResource(text, r);
            if (score > 0) {
                int suggestedQty = suggestQuantity(text, r);
                scored.add(new Recommendation(r, suggestedQty, score, explain(text, r)));
            }
        }

        // sort by score desc
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));

        return scored.stream().limit(topK).collect(Collectors.toList());
    }

    private int scoreResource(String text, Resource r) {
        int score = 0;

        // Availability score
        if (r.getAvquant() <= 0) return 0; // cannot recommend if none available
        score += Math.min(20, (int) r.getAvquant()); // more available => slightly more score

        // Match resource type with detected domain keywords
        String rType = safeUpper(r.getType());

        for (Map.Entry<String, Rule> entry : RULES.entrySet()) {
            String kw = entry.getKey();
            Rule rule = entry.getValue();

            if (text.contains(kw)) {
                // keyword presence
                score += 10;

                // type preference
                if (rType.equals(rule.preferredType())) {
                    score += rule.boost();
                }
            }
        }

        // Extra: resource name/type matching keywords
        String name = normalize(r.getName());
        String type = normalize(r.getType());

        for (String kw : RULES.keySet()) {
            if (text.contains(kw) && (name.contains(kw) || type.contains(kw))) {
                score += 15;
            }
        }

        // Penalty if very low stock
        if (r.getAvquant() <= 2) score -= 10;

        return Math.max(score, 0);
    }

    private int suggestQuantity(String text, Resource r) {
        // Simple heuristics: team/project hints
        int base = 1;

        if (text.contains("team") || text.contains("group")) base = 3;
        if (text.contains("enterprise") || text.contains("company")) base = 5;

        // For software licenses: often >= team size (we approximate)
        String type = safeUpper(r.getType());
        if (type.contains("SOFTWARE")) {
            return Math.min((int) r.getAvquant(), base);
        }

        // For physical: usually 1-2 unless team/project
        int qty = Math.min((int) r.getAvquant(), Math.max(1, base - 1));
        return qty;
    }

    private List<String> explain(String text, Resource r) {
        List<String> reasons = new ArrayList<>();

        if (r.getAvquant() > 0) reasons.add("available stock: " + (int) r.getAvquant());

        String rType = safeUpper(r.getType());
        for (Map.Entry<String, Rule> entry : RULES.entrySet()) {
            if (text.contains(entry.getKey())) {
                reasons.add("keyword detected: " + entry.getKey());
                if (rType.equals(entry.getValue().preferredType())) {
                    reasons.add("type match: " + rType);
                }
            }
        }

        if (reasons.size() > 5) return reasons.subList(0, 5);
        return reasons;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private record Rule(String preferredType, int boost) {}

    // ✅ You will display these in UI
    public record Recommendation(Resource resource, int suggestedQty, int score, List<String> reasons) {}
}