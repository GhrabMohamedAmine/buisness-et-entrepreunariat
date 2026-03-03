package services;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class EmojiRepo {

    public static final class Meta {
        public String name;
        public String slug;
        public String unicode_version;
        public String emoji_version;
        public boolean skin_tone_support;
        public String group;   // filled from by-group
    }

    private static volatile EmojiRepo INSTANCE;

    private final Map<String, Meta> metaByEmoji = new LinkedHashMap<>();      // stable iteration
    private final Map<String, List<String>> emojisByGroup = new LinkedHashMap<>();

    private EmojiRepo() {}

    public static EmojiRepo load() {
        // singleton cache
        EmojiRepo inst = INSTANCE;
        if (inst != null) return inst;

        synchronized (EmojiRepo.class) {
            if (INSTANCE != null) return INSTANCE;

            EmojiRepo r = new EmojiRepo();
            Gson gson = new Gson();

            // ---------- 1) Load meta (by-emoji) ----------
            JsonElement emojiRoot = readJson("/assets/emoji/data-by-emoji.json");

            if (emojiRoot.isJsonObject()) {
                // Format A: { "😀": {meta}, ... }
                JsonObject byEmoji = emojiRoot.getAsJsonObject();
                for (var e : byEmoji.entrySet()) {
                    String emoji = e.getKey();
                    Meta meta = gson.fromJson(e.getValue(), Meta.class);
                    r.metaByEmoji.put(emoji, meta);
                }
            } else if (emojiRoot.isJsonArray()) {
                // Format B: [ { "emoji":"😀", ... }, ... ]
                JsonArray arr = emojiRoot.getAsJsonArray();
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (!o.has("emoji")) continue;

                    String emoji = o.get("emoji").getAsString();
                    Meta meta = gson.fromJson(o, Meta.class);
                    r.metaByEmoji.put(emoji, meta);
                }
            } else {
                throw new IllegalStateException("data-by-emoji.json root must be JSON object or array");
            }

            // ---------- 2) Load grouping (by-group) ----------
            JsonElement groupRoot = readJson("/assets/emoji/data-by-group.json");

            if (groupRoot.isJsonArray()) {
                // Your format: [ { name, slug, emojis:[{emoji, ...}] }, ... ]
                JsonArray groups = groupRoot.getAsJsonArray();

                for (JsonElement ge : groups) {
                    if (!ge.isJsonObject()) continue;
                    JsonObject g = ge.getAsJsonObject();

                    String groupName = g.has("name") ? g.get("name").getAsString() : "Unknown";
                    JsonArray emojisArr = g.has("emojis") && g.get("emojis").isJsonArray()
                            ? g.getAsJsonArray("emojis")
                            : new JsonArray();

                    List<String> list = new ArrayList<>(emojisArr.size());

                    for (JsonElement ee : emojisArr) {
                        if (!ee.isJsonObject()) continue;
                        JsonObject eo = ee.getAsJsonObject();
                        if (!eo.has("emoji")) continue;

                        String emoji = eo.get("emoji").getAsString();
                        list.add(emoji);

                        // set group on existing meta; if missing, build minimal meta from group entry
                        Meta meta = r.metaByEmoji.get(emoji);
                        if (meta != null) {
                            meta.group = groupName;
                        } else {
                            Meta m = gson.fromJson(eo, Meta.class);
                            m.group = groupName;
                            r.metaByEmoji.put(emoji, m);
                        }
                    }

                    r.emojisByGroup.put(groupName, Collections.unmodifiableList(list));
                }

            } else if (groupRoot.isJsonObject()) {
                // Other format
                JsonObject byGroup = groupRoot.getAsJsonObject();

                for (var groupEntry : byGroup.entrySet()) {
                    String groupName = groupEntry.getKey();
                    JsonObject groupObj = groupEntry.getValue().getAsJsonObject();

                    List<String> list = new ArrayList<>(groupObj.size());
                    for (var emojiEntry : groupObj.entrySet()) {
                        String emoji = emojiEntry.getKey();
                        list.add(emoji);

                        Meta meta = r.metaByEmoji.get(emoji);
                        if (meta != null) meta.group = groupName;
                    }
                    r.emojisByGroup.put(groupName, Collections.unmodifiableList(list));
                }

            } else {
                throw new IllegalStateException("data-by-group.json root must be JSON array or object");
            }

            INSTANCE = r;
            return r;
        }
    }

    public Map<String, List<String>> byGroup() { return emojisByGroup; }
    public Meta meta(String emoji) { return metaByEmoji.get(emoji); }

    public List<String> search(String q, int limit) {
        q = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || limit <= 0) return List.of();

        record Hit(String emoji, int score, String name) {}
        ArrayList<Hit> hits = new ArrayList<>();

        for (var entry : metaByEmoji.entrySet()) {
            String emoji = entry.getKey();
            Meta m = entry.getValue();

            String name = (m.name == null ? "" : m.name).toLowerCase(Locale.ROOT);
            String slug = (m.slug == null ? "" : m.slug).toLowerCase(Locale.ROOT);
            String group = (m.group == null ? "" : m.group).toLowerCase(Locale.ROOT);

            // scoring: startsWith > contains, name > slug > group
            int score = 0;
            if (name.startsWith(q)) score = 300;
            else if (name.contains(q)) score = 200;
            else if (slug.startsWith(q)) score = 180;
            else if (slug.contains(q)) score = 120;
            else if (group.contains(q)) score = 60;

            if (score > 0) hits.add(new Hit(emoji, score, name));
        }

        hits.sort((a, b) -> {
            int c = Integer.compare(b.score, a.score);
            if (c != 0) return c;
            c = a.name.compareTo(b.name);
            if (c != 0) return c;
            return a.emoji.compareTo(b.emoji);
        });

        int n = Math.min(limit, hits.size());
        ArrayList<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(hits.get(i).emoji);
        return out;
    }

    private static JsonElement readJson(String resourcePath) {
        try (InputStream in = EmojiRepo.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalStateException("Missing resource: " + resourcePath);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}