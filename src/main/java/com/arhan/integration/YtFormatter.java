package com.arhan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class YtFormatter {
    private static final ObjectMapper OM = new ObjectMapper();


    public static String fromMetadata(String metaJson, String ytWebBase) throws Exception {
        return fromMetadata(metaJson, ytWebBase, null);
    }

    public static String fromMetadata(String metaJson, String ytWebBase, String recipientName) throws Exception {
        JsonNode m = OM.readTree(metaJson);

        // Creates strings for each component we want to output in our discord webhook notification
        String id      = m.path("issue").path("id").asText("");
        String summary = m.path("issue").path("summary").asText("");
        String header  = m.path("header").asText("");

        String issueUrl = m.path("issue").path("url").asText(null);
        if (issueUrl == null || issueUrl.isBlank()) {
            String base = ytWebBase.endsWith("/") ? ytWebBase : ytWebBase + "/";
            issueUrl = base + "issue/" + id;
        }

        // Note that the author feature is not working at the moment. Time, however, is just fine
        String author = firstNonBlank(
                m.path("change").path("author").path("fullName").asText(""),
                m.path("change").path("author").path("login").asText(""),
                m.path("author").path("fullName").asText(""),
                m.path("author").path("login").asText("")
        );
        String time  = m.path("change").path("humanReadableTimeStamp").asText("");

        // Identifies reason as change on certain events
        // Below functions are boilerplate of retreiving relevant information from
        // the API endpoint metadata to display to user on Discord
        List<String> lines = new ArrayList<>();
        ArrayNode events = (ArrayNode) m.path("change").path("events");
        if (events != null) {
            for (JsonNode e : events) {
                String cat = e.path("category").asText("");
                String name = e.path("name").asText("");
                String removed = firstElement(e.path("removedValues"));
                String added   = firstElement(e.path("addedValues"));

                if ("CUSTOM_FIELD".equals(cat) && "Priority".equalsIgnoreCase(name)) {
                    lines.add("**Priority:** " + removed + " → " + added);
                } else if ("CUSTOM_FIELD".equals(cat) && "State".equalsIgnoreCase(name)) {
                    lines.add("**Status:** " + removed + " → " + added);
                } else if ("COMMENT".equals(cat)) {
                    String comment = (added == null || added.isBlank()) ? name : added;
                    if (comment != null && !comment.isBlank()) {
                        lines.add("**Comment:** " + comment);
                    }
                }
            }
        }

        // Description (text from issue)
        String desc = m.path("issue").path("description").asText("");
        if (desc != null && !desc.isBlank()) {
            String shortDesc = shorten(oneLine(desc), 160);
            lines.add("**Description:** " + shortDesc);
        }


        String reasonSentence = buildReasonSentence(m, recipientName);

        // Builds the header of info that states post time and author (but not working atm)
        String headerLine = italic(joinNonBlank(header,
                (author.isBlank() ? null : "by " + author),
                (time.isBlank() ? null : "at " + time)));

        String changes = lines.isEmpty() ? "" : String.join("\n", lines);
        // regex formatting the output
        return """
**%s** — %s
%s

%s

[Open in YouTrack](%s)
%s
""".formatted(id, summary, headerLine, changes, issueUrl,
                reasonSentence.isBlank() ? "" : "\n" + reasonSentence).trim();
    }

    private static String buildReasonSentence(JsonNode m, String recipientName) {
        String subject = (recipientName == null || recipientName.isBlank())
                ? "You"
                : "You (" + recipientName + ")";

        List<String> clauses = new ArrayList<>();

        // Mentions (from YouTrack)
        JsonNode mentions = m.path("reason").path("mentionReasons");
        if (mentions.isArray() && !mentions.isEmpty()) {
            clauses.add("you subscribe to **@mentions** in issue descriptions and comments");
        }

        // Saved searches (to output)
        JsonNode searches = m.path("reason").path("savedSearchReasons");
        if (searches.isArray() && !searches.isEmpty()) {
            List<String> ss = new ArrayList<>();
            for (JsonNode n : searches) {
                String name = n.path("name").asText("");
                if (!name.isBlank()) ss.add("**" + name + "**");
            }
            if (!ss.isEmpty()) {
                clauses.add("notification events for the " + listTogether(ss) + " saved search"
                        + (ss.size() > 1 ? "es" : ""));
            } else {
                clauses.add("a **saved search** you’re subscribed to");
            }
        }

        // Identifies true reasons you receive msg from metadata (eg issue subscription)
        JsonNode tags = m.path("reason").path("tagReasons");
        if (tags.isArray() && tags.size() > 0) {
            List<String> tg = new ArrayList<>();
            for (JsonNode n : tags) {
                String name = n.path("name").asText("");
                if (!name.isBlank()) tg.add(name);
            }
            if (!tg.isEmpty()) {
                List<String> pretty = tg.stream()
                        .map(x -> "Star".equalsIgnoreCase(x) ? "**Star** tag" : "**" + x + "** tag")
                        .collect(Collectors.toList());
                clauses.add("notification events for the " + listTogether(pretty));
            } else {
                clauses.add("a **tag subscription**");
            }
        }

        if (clauses.isEmpty()) return "";

        String because = listTogether(clauses);
        return subject + " received this message because " + because + ". "
                + "To unsubscribe, you can mute notifications for this issue or edit your notification preferences.";
    }

   // Necessary functions to format text appropriately (continuitation of boiler plate enhancements)
    private static String firstElement(JsonNode arr) {
        if (arr != null && arr.isArray() && !arr.isEmpty()) {
            return arr.get(0).path("name").asText("");
        }
        return "";
    }

    private static String oneLine(String s) {
        return s.replace("\r", " ").replace("\n", " ").replace("\t", " ").replaceAll(" +", " ").trim();
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
    
    private static String listTogether(List<String> parts) {
        if (parts == null || parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);
        String allButLast = String.join(", ", parts.subList(0, parts.size()-1));
        return allButLast + ", and " + parts.get(parts.size()-1);
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(p);
        }
        return sb.toString();
    }

    private static String italic(String s) {
        return (s == null || s.isBlank()) ? "" : "_" + s + "_";
    }
}