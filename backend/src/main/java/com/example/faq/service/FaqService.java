

// package com.example.faq.service;

// import com.example.faq.model.Faq;
// import com.example.faq.repository.FaqRepository;
// import org.springframework.stereotype.Service;

// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;

// import java.text.Normalizer;
// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;

// @Service
// public class FaqService {

//     private final FaqRepository faqRepository;
//     private final HttpClient httpClient = HttpClient.newHttpClient();

//     public FaqService(FaqRepository faqRepository) {
//         this.faqRepository = faqRepository;
//     }

//     private static final Set<String> STOPWORDS_EN = Set.of(
//             "what","is","a","an","the","of","in","on","to","for","and","between",
//             "how","do","does","did","can","should","will","my","your","its","it's",
//             "who","whom","where","when","which","with","by","about","that","this",
//             "these","those","as","are","was","were","be","been","being","at","from","why"
//     );

//     private static final Set<String> STOPWORDS_BN = Set.of(
//             "কি","কী","কেন","কখন","কোথায়","কোথায়","কিভাবে","কীভাবে",
//             "এবং","এই","ও","তার","আমি","আপনি","হয়ে","হয়","হবে","থেকে","তে","করে","ছে"
//     );

//     private static final int TOKEN_FETCH_LIMIT = 5;
//     private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");
//     private static final double MIN_ACCEPTED_SCORE = 0.0;

//     /**
//      * Main entry: automatically detects language and returns best DB answer + note.
//      */
//     public String getAnswer(String question) {
//         if (question == null || question.isBlank()) {
//             return "⚠️ Please enter a valid question.";
//         }

//         // 1️⃣ Auto-detect language
//         String lang = detectLanguage(question);
//         String rawQuery = question.trim();

//         // 2️⃣ Try exact-match shortcut
//         try {
//             Faq exact = faqRepository.findExact(rawQuery, lang);
//             if (exact != null) {
//                 return safeAnswer(exact) + "\n\nNote: " + fetchExtraInfo(rawQuery);
//             }
//         } catch (Exception e) {
//             System.err.println("Exact-match query failed: " + e.getMessage());
//         }

//         // 3️⃣ Tokenize and remove stopwords
//         List<String> allQueryTokens = extractWords(rawQuery).stream()
//                 .map(String::toLowerCase)
//                 .collect(Collectors.toList());

//         Set<String> stopwords = lang.equals("BN") ? STOPWORDS_BN : STOPWORDS_EN;

//         List<String> userTokens = allQueryTokens.stream()
//                 .filter(t -> !stopwords.contains(t))
//                 .collect(Collectors.toList());

//         if (userTokens.isEmpty()) userTokens = new ArrayList<>(allQueryTokens);
//         if (userTokens.isEmpty())
//             return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 4️⃣ Fetch candidates
//         LinkedHashMap<Long, Faq> candidates = new LinkedHashMap<>();
//         tryAddCandidates(rawQuery, lang, candidates);
//         int lim = Math.min(TOKEN_FETCH_LIMIT, userTokens.size());
//         for (int i = 0; i < lim; ++i) tryAddCandidates(userTokens.get(i), lang, candidates);

//         if (candidates.isEmpty())
//             return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 5️⃣ Check normalized exact among candidates
//         String normalizedQuery = normalizeForCompare(rawQuery);
//         for (Faq f : candidates.values()) {
//             if (normalizeForCompare(f.getQuestion()).equals(normalizedQuery)) {
//                 return safeAnswer(f) + "\n\nNote: " + fetchExtraInfo(rawQuery);
//             }
//         }

//         // 6️⃣ Score candidates by token overlap
//         Set<String> userTokenSet = new HashSet<>(userTokens);
//         double bestScore = -1.0;
//         Faq bestFaq = null;
//         int bestQuestionLength = Integer.MAX_VALUE;

//         for (Faq f : candidates.values()) {
//             String combined = (f.getQuestion() == null ? "" : f.getQuestion()) + " "
//                     + (f.getAnswer() == null ? "" : f.getAnswer());
//             Set<String> candTokens = extractWords(combined).stream()
//                     .map(String::toLowerCase)
//                     .collect(Collectors.toSet());
//             candTokens.removeAll(stopwords);

//             int matches = 0;
//             for (String t : userTokenSet) if (candTokens.contains(t)) matches++;

//             double score = userTokenSet.isEmpty() ? 0.0 : ((double) matches / userTokenSet.size());
//             int qlen = (f.getQuestion() == null) ? Integer.MAX_VALUE : f.getQuestion().length();

//             if (score > bestScore + 1e-9 ||
//                     (Math.abs(score - bestScore) < 1e-9 && qlen < bestQuestionLength)) {
//                 bestScore = score;
//                 bestFaq = f;
//                 bestQuestionLength = qlen;
//             }
//         }

//         if (bestFaq != null && bestScore > MIN_ACCEPTED_SCORE)
//             return safeAnswer(bestFaq) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 7️⃣ Fallback to shortest candidate
//         Faq fallback = candidates.values().stream()
//                 .min(Comparator.comparingInt(a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().length()))
//                 .orElse(null);

//         return (fallback != null ? safeAnswer(fallback) : fallbackMessage(lang))
//                 + "\n\nNote: " + fetchExtraInfo(rawQuery);
//     }

//     // ---------- helpers ----------

//     private void tryAddCandidates(String keyword, String lang, Map<Long, Faq> out) {
//         if (keyword == null || keyword.isBlank()) return;
//         try {
//             List<Faq> found = faqRepository.findCandidates(keyword, lang);
//             if (found != null) {
//                 for (Faq f : found) if (f != null && f.getId() != null) out.putIfAbsent(f.getId(), f);
//             }
//         } catch (Exception e) {
//             System.err.println("Candidate fetch failed for \"" + safeForLog(keyword) + "\": " + e.getMessage());
//         }
//     }

//     private String safeAnswer(Faq f) {
//         if (f == null) return fallbackMessage("EN");
//         String a = f.getAnswer();
//         return (a == null || a.isBlank()) ? fallbackMessage("EN") : a.trim();
//     }

//     private String fallbackMessage(String lang) {
//         return "BN".equalsIgnoreCase(lang)
//                 ? "দুঃখিত, আমি এটির নির্দিষ্ট উত্তর খুঁজে পাইনি। অনুগ্রহ করে প্রশ্নটি অন্যভাবে লিখে দেখুন।"
//                 : "Sorry, I couldn't find an answer. Please try rephrasing your question.";
//     }

//     private String normalizeForCompare(String text) {
//         if (text == null) return "";
//         return String.join(" ", extractWords(text)).toLowerCase();
//     }

//     private List<String> extractWords(String text) {
//         if (text == null || text.isBlank()) return Collections.emptyList();
//         String norm = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
//         Matcher m = WORD_PATTERN.matcher(norm);
//         List<String> tokens = new ArrayList<>();
//         while (m.find()) tokens.add(m.group());
//         return tokens;
//     }

//     private String safeForLog(String s) {
//         if (s == null) return "";
//         return s.replaceAll("[\\r\\n]+", " ").trim();
//     }

//     // --------- NEW: language detection ----------
//     private String detectLanguage(String text) {
//         if (text == null || text.isBlank()) return "EN";
//         int bnCount = 0, enCount = 0;
//         for (char c : text.toCharArray()) {
//             if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BENGALI) bnCount++;
//             else if (Character.isLetter(c) && c < 128) enCount++;
//         }
//         return bnCount > enCount ? "BN" : "EN";
//     }

//     // --------- NEW: Fetch Wikipedia or HuggingFace ---------
//     private String fetchExtraInfo(String query) {
//         try {
//             // Wikipedia REST API
//             String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
//                     query.trim().replace(" ", "_");
//             HttpRequest req = HttpRequest.newBuilder()
//                     .uri(URI.create(url))
//                     .header("User-Agent", "LegalFAQBot/1.0")
//                     .GET()
//                     .build();

//             HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
//             if (resp.statusCode() == 200 && resp.body().contains("\"extract\"")) {
//                 String body = resp.body();
//                 int idx = body.indexOf("\"extract\":\"");
//                 if (idx != -1) {
//                     String extract = body.substring(idx + 11);
//                     extract = extract.split("\",\"")[0];
//                     return extract.replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"");
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Wiki fetch failed: " + e.getMessage());
//         }

//         // HuggingFace fallback
//         try {
//             String apiKey = System.getenv("HUGGINGFACE_API_KEY");
//             if (apiKey == null || apiKey.isBlank()) return "(no extra info available)";

//             String payload = "{ \"inputs\": \"" + query.replace("\"", "'") + "\", " +
//                     "\"parameters\": { \"max_new_tokens\": 120, \"temperature\": 0.85 } }";

//             HttpRequest req = HttpRequest.newBuilder()
//                     .uri(URI.create("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2"))
//                     .header("Authorization", "Bearer " + apiKey)
//                     .header("Content-Type", "application/json")
//                     .POST(HttpRequest.BodyPublishers.ofString(payload))
//                     .build();

//             HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
//             if (resp.statusCode() == 200) {
//                 String body = resp.body();
//                 int start = body.indexOf("generated_text");
//                 if (start != -1) {
//                     String cut = body.substring(start + 17);
//                     String out = cut.split("\"")[0];
//                     return out.trim();
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("HF fetch failed: " + e.getMessage());
//         }

//         return "(no extra info available)";
//     }
// }

//***************************************************************************************************

// package com.example.faq.service;

// import com.example.faq.model.Faq;
// import com.example.faq.repository.FaqRepository;
// import org.springframework.stereotype.Service;

// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;

// import java.text.Normalizer;
// import java.util.*;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;

// @Service
// public class FaqService {

//     private final FaqRepository faqRepository;
//     private final HttpClient httpClient = HttpClient.newHttpClient();

//     public FaqService(FaqRepository faqRepository) {
//         this.faqRepository = faqRepository;
//     }

//     private static final Set<String> STOPWORDS_EN = Set.of(
//             "what","is","a","an","the","of","in","on","to","for","and","between",
//             "how","do","does","did","can","should","will","my","your","its","it's",
//             "who","whom","where","when","which","with","by","about","that","this",
//             "these","those","as","are","was","were","be","been","being","at","from","why"
//     );

//     private static final Set<String> STOPWORDS_BN = Set.of(
//             "কি","কী","কেন","কখন","কোথায়","কোথায়","কিভাবে","কীভাবে",
//             "এবং","এই","ও","তার","আমি","আপনি","হয়ে","হয়","হবে","থেকে","তে","করে","ছে"
//     );

//     private static final int TOKEN_FETCH_LIMIT = 5;
//     private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");
//     private static final double MIN_ACCEPTED_SCORE = 0.0;

//     /**
//      * Main entry: automatically detects language and returns best DB answer + note.
//      */
//     public String getAnswer(String question) {
//         if (question == null || question.isBlank()) {
//             return "⚠️ Please enter a valid question.";
//         }

//         // 1️⃣ Auto-detect language
//         String lang = detectLanguage(question);
//         String rawQuery = question.trim();

//         // 2️⃣ Try exact-match shortcut
//         try {
//             Faq exact = faqRepository.findExact(rawQuery, lang);
//             if (exact != null) {
//                 return safeAnswer(exact) + "\n\nNote: " + fetchExtraInfo(rawQuery);
//             }
//         } catch (Exception e) {
//             System.err.println("Exact-match query failed: " + e.getMessage());
//         }

//         // 3️⃣ Tokenize and remove stopwords
//         List<String> allQueryTokens = extractWords(rawQuery).stream()
//                 .map(String::toLowerCase)
//                 .collect(Collectors.toList());

//         Set<String> stopwords = lang.equals("BN") ? STOPWORDS_BN : STOPWORDS_EN;

//         List<String> userTokens = allQueryTokens.stream()
//                 .filter(t -> !stopwords.contains(t))
//                 .collect(Collectors.toList());

//         if (userTokens.isEmpty()) userTokens = new ArrayList<>(allQueryTokens);
//         if (userTokens.isEmpty())
//             return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 4️⃣ Fetch candidates
//         LinkedHashMap<Long, Faq> candidates = new LinkedHashMap<>();
//         tryAddCandidates(rawQuery, lang, candidates);
//         int lim = Math.min(TOKEN_FETCH_LIMIT, userTokens.size());
//         for (int i = 0; i < lim; ++i) tryAddCandidates(userTokens.get(i), lang, candidates);

//         if (candidates.isEmpty())
//             return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 5️⃣ Check normalized exact among candidates
//         String normalizedQuery = normalizeForCompare(rawQuery);
//         for (Faq f : candidates.values()) {
//             if (normalizeForCompare(f.getQuestion()).equals(normalizedQuery)) {
//                 return safeAnswer(f) + "\n\nNote: " + fetchExtraInfo(rawQuery);
//             }
//         }

//         // 6️⃣ Score candidates by token overlap
//         Set<String> userTokenSet = new HashSet<>(userTokens);
//         double bestScore = -1.0;
//         Faq bestFaq = null;
//         int bestQuestionLength = Integer.MAX_VALUE;

//         for (Faq f : candidates.values()) {
//             String combined = (f.getQuestion() == null ? "" : f.getQuestion()) + " "
//                     + (f.getAnswer() == null ? "" : f.getAnswer());
//             Set<String> candTokens = extractWords(combined).stream()
//                     .map(String::toLowerCase)
//                     .collect(Collectors.toSet());
//             candTokens.removeAll(stopwords);

//             int matches = 0;
//             for (String t : userTokenSet) if (candTokens.contains(t)) matches++;

//             double score = userTokenSet.isEmpty() ? 0.0 : ((double) matches / userTokenSet.size());
//             int qlen = (f.getQuestion() == null) ? Integer.MAX_VALUE : f.getQuestion().length();

//             if (score > bestScore + 1e-9 ||
//                     (Math.abs(score - bestScore) < 1e-9 && qlen < bestQuestionLength)) {
//                 bestScore = score;
//                 bestFaq = f;
//                 bestQuestionLength = qlen;
//             }
//         }

//         if (bestFaq != null && bestScore > MIN_ACCEPTED_SCORE)
//             return safeAnswer(bestFaq) + "\n\nNote: " + fetchExtraInfo(rawQuery);

//         // 7️⃣ Fallback to shortest candidate
//         Faq fallback = candidates.values().stream()
//                 .min(Comparator.comparingInt(a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().length()))
//                 .orElse(null);

//         return (fallback != null ? safeAnswer(fallback) : fallbackMessage(lang))
//                 + "\n\nNote: " + fetchExtraInfo(rawQuery);
//     }

//     // ---------- helpers ----------

//     private void tryAddCandidates(String keyword, String lang, Map<Long, Faq> out) {
//         if (keyword == null || keyword.isBlank()) return;
//         try {
//             List<Faq> found = faqRepository.findCandidates(keyword, lang);
//             if (found != null) {
//                 for (Faq f : found) if (f != null && f.getId() != null) out.putIfAbsent(f.getId(), f);
//             }
//         } catch (Exception e) {
//             System.err.println("Candidate fetch failed for \"" + safeForLog(keyword) + "\": " + e.getMessage());
//         }
//     }

//     private String safeAnswer(Faq f) {
//         if (f == null) return fallbackMessage("EN");
//         String a = f.getAnswer();
//         return (a == null || a.isBlank()) ? fallbackMessage("EN") : a.trim();
//     }

//     private String fallbackMessage(String lang) {
//         return "BN".equalsIgnoreCase(lang)
//                 ? "দুঃখিত, আমি এটির নির্দিষ্ট উত্তর খুঁজে পাইনি। অনুগ্রহ করে প্রশ্নটি অন্যভাবে লিখে দেখুন।"
//                 : "Sorry, I couldn't find an answer. Please try rephrasing your question.";
//     }

//     private String normalizeForCompare(String text) {
//         if (text == null) return "";
//         return String.join(" ", extractWords(text)).toLowerCase();
//     }

//     private List<String> extractWords(String text) {
//         if (text == null || text.isBlank()) return Collections.emptyList();
//         String norm = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
//         Matcher m = WORD_PATTERN.matcher(norm);
//         List<String> tokens = new ArrayList<>();
//         while (m.find()) tokens.add(m.group());
//         return tokens;
//     }

//     private String safeForLog(String s) {
//         if (s == null) return "";
//         return s.replaceAll("[\\r\\n]+", " ").trim();
//     }

//     // --------- NEW: language detection ----------
//     private String detectLanguage(String text) {
//         if (text == null || text.isBlank()) return "EN";
//         int bnCount = 0, enCount = 0;
//         for (char c : text.toCharArray()) {
//             if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BENGALI) bnCount++;
//             else if (Character.isLetter(c) && c < 128) enCount++;
//         }
//         return bnCount > enCount ? "BN" : "EN";
//     }

//     // --------- NEW: Fetch Wikipedia or HuggingFace ---------

//     private String fetchExtraInfo(String query) {
//     // 1. Wikipedia check (same as before)
//     try {
//         String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
//                 query.trim().replace(" ", "_");
//         HttpRequest req = HttpRequest.newBuilder()
//                 .uri(URI.create(url))
//                 .header("User-Agent", "LegalFAQBot/1.0")
//                 .GET()
//                 .build();

//         HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
//         if (resp.statusCode() == 200 && resp.body().contains("\"extract\"")) {
//             String body = resp.body();
//             int idx = body.indexOf("\"extract\":\"");
//             if (idx != -1) {
//                 String extract = body.substring(idx + 11);
//                 extract = extract.split("\",\"")[0];
//                 return extract.replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"");
//             }
//         }
//     } catch (Exception e) {
//         System.err.println("Wiki fetch failed: " + e.getMessage());
//     }

//     // 2. Gemini LLM attempt
//     try {
//         String apiKey = System.getenv("GEMINI_API_KEY");
//         if (apiKey != null && !apiKey.isBlank()) {
//             String payload = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" +
//                     query.replace("\"", "'") + "\" }] }], " +
//                     "\"generationConfig\": { \"temperature\": 0.85, \"maxOutputTokens\": 120 } }";

//             HttpRequest req = HttpRequest.newBuilder()
//                     .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey))
//                     .header("Content-Type", "application/json")
//                     .POST(HttpRequest.BodyPublishers.ofString(payload))
//                     .build();

//             HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
//             if (resp.statusCode() == 200 && resp.body().contains("\"text\"")) {
//                 String body = resp.body();
//                 int idx = body.indexOf("\"text\":");
//                 if (idx != -1) {
//                     String cut = body.substring(idx + 8);
//                     String out = cut.split("\"")[1];
//                     return out.trim();
//                 }
//             }
//         }
//     } catch (Exception e) {
//         System.err.println("Gemini fetch failed: " + e.getMessage());
//     }

//     // 3. HuggingFace fallback (Mistral)
//     try {
//         String apiKey = System.getenv("HUGGINGFACE_API_KEY");
//         if (apiKey == null || apiKey.isBlank()) return "(no extra info available)";

//         String payload = "{ \"inputs\": \"" + query.replace("\"", "'") + "\", " +
//                 "\"parameters\": { \"max_new_tokens\": 120, \"temperature\": 0.85 } }";

//         HttpRequest req = HttpRequest.newBuilder()
//                 .uri(URI.create("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2"))
//                 .header("Authorization", "Bearer " + apiKey)
//                 .header("Content-Type", "application/json")
//                 .POST(HttpRequest.BodyPublishers.ofString(payload))
//                 .build();

//         HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
//         if (resp.statusCode() == 200) {
//             String body = resp.body();
//             int start = body.indexOf("generated_text");
//             if (start != -1) {
//                 String cut = body.substring(start + 17);
//                 String out = cut.split("\"")[0];
//                 return out.trim();
//             }
//         }
//     } catch (Exception e) {
//         System.err.println("HF fetch failed: " + e.getMessage());
//     }

//     // 4. Fallback if all fail
//     return "(no extra info available)";
//     }

// }
//***************************************************************
package com.example.faq.service;

import com.example.faq.model.Faq;
import com.example.faq.repository.FaqRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FaqService {

    private final FaqRepository faqRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    private static final Set<String> STOPWORDS_EN = Set.of(
            "what","is","a","an","the","of","in","on","to","for","and","between",
            "how","do","does","did","can","should","will","my","your","its","it's",
            "who","whom","where","when","which","with","by","about","that","this",
            "these","those","as","are","was","were","be","been","being","at","from","why"
    );

    private static final Set<String> STOPWORDS_BN = Set.of(
            "কি","কী","কেন","কখন","কোথায়","কোথায়","কিভাবে","কীভাবে",
            "এবং","এই","ও","তার","আমি","আপনি","হয়ে","হয়","হবে","থেকে","তে","করে","ছে"
    );

    private static final int TOKEN_FETCH_LIMIT = 5;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");
    private static final double MIN_ACCEPTED_SCORE = 0.0;

    /**
     * Main entry: automatically detects language and returns best DB answer,
     * plus Wikipedia/LLM note.
     */
    public String getAnswer(String question) {
        if (question == null || question.isBlank()) {
            return "⚠️ Please enter a valid question.";
        }

        // 1️⃣ Auto-detect language
        String lang = detectLanguage(question);
        String rawQuery = question.trim();

        // 2️⃣ Try exact-match shortcut
        try {
            Faq exact = faqRepository.findExact(rawQuery, lang);
            if (exact != null) return safeAnswer(exact) + "\n\nNote: " + fetchExtraInfo(rawQuery);
        } catch (Exception e) {
            System.err.println("Exact-match query failed: " + e.getMessage());
        }

        // 3️⃣ Tokenize and remove stopwords
        List<String> allQueryTokens = extractWords(rawQuery).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Set<String> stopwords = lang.equals("BN") ? STOPWORDS_BN : STOPWORDS_EN;

        List<String> userTokens = allQueryTokens.stream()
                .filter(t -> !stopwords.contains(t))
                .collect(Collectors.toList());

        if (userTokens.isEmpty()) userTokens = new ArrayList<>(allQueryTokens);
        if (userTokens.isEmpty()) return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

        // 4️⃣ Fetch candidates
        LinkedHashMap<Long, Faq> candidates = new LinkedHashMap<>();
        tryAddCandidates(rawQuery, lang, candidates);
        int lim = Math.min(TOKEN_FETCH_LIMIT, userTokens.size());
        for (int i = 0; i < lim; ++i) tryAddCandidates(userTokens.get(i), lang, candidates);

        if (candidates.isEmpty()) return fallbackMessage(lang) + "\n\nNote: " + fetchExtraInfo(rawQuery);

        // 5️⃣ Check normalized exact among candidates
        String normalizedQuery = normalizeForCompare(rawQuery);
        for (Faq f : candidates.values()) {
            if (normalizeForCompare(f.getQuestion()).equals(normalizedQuery))
                return safeAnswer(f) + "\n\nNote: " + fetchExtraInfo(rawQuery);
        }

        // 6️⃣ Score candidates by token overlap
        Set<String> userTokenSet = new HashSet<>(userTokens);
        double bestScore = -1.0;
        Faq bestFaq = null;
        int bestQuestionLength = Integer.MAX_VALUE;

        for (Faq f : candidates.values()) {
            String combined = (f.getQuestion() == null ? "" : f.getQuestion()) + " "
                    + (f.getAnswer() == null ? "" : f.getAnswer());
            Set<String> candTokens = extractWords(combined).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            candTokens.removeAll(stopwords);

            int matches = 0;
            for (String t : userTokenSet) if (candTokens.contains(t)) matches++;

            double score = userTokenSet.isEmpty() ? 0.0 : ((double) matches / userTokenSet.size());
            int qlen = (f.getQuestion() == null) ? Integer.MAX_VALUE : f.getQuestion().length();

            if (score > bestScore + 1e-9 ||
                    (Math.abs(score - bestScore) < 1e-9 && qlen < bestQuestionLength)) {
                bestScore = score;
                bestFaq = f;
                bestQuestionLength = qlen;
            }
        }

        if (bestFaq != null && bestScore > MIN_ACCEPTED_SCORE)
            return safeAnswer(bestFaq) + "\n\nNote: " + fetchExtraInfo(rawQuery);

        // 7️⃣ Fallback to shortest candidate
        Faq fallback = candidates.values().stream()
                .min(Comparator.comparingInt(a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().length()))
                .orElse(null);

        return (fallback != null ? safeAnswer(fallback) : fallbackMessage(lang)) + "\n\nNote: " + fetchExtraInfo(rawQuery);
    }

    // ---------- helpers ----------

    private void tryAddCandidates(String keyword, String lang, Map<Long, Faq> out) {
        if (keyword == null || keyword.isBlank()) return;
        try {
            List<Faq> found = faqRepository.findCandidates(keyword, lang);
            if (found != null) {
                for (Faq f : found) if (f != null && f.getId() != null) out.putIfAbsent(f.getId(), f);
            }
        } catch (Exception e) {
            System.err.println("Candidate fetch failed for \"" + safeForLog(keyword) + "\": " + e.getMessage());
        }
    }

    private String safeAnswer(Faq f) {
        if (f == null) return fallbackMessage("EN");
        String a = f.getAnswer();
        return (a == null || a.isBlank()) ? fallbackMessage("EN") : a.trim();
    }

    private String fallbackMessage(String lang) {
        return "BN".equalsIgnoreCase(lang)
                ? "দুঃখিত, আমি এটির নির্দিষ্ট উত্তর খুঁজে পাইনি। অনুগ্রহ করে প্রশ্নটি অন্যভাবে লিখে দেখুন।"
                : "Sorry, I couldn't find an answer. Please try rephrasing your question.";
    }

    private String normalizeForCompare(String text) {
        if (text == null) return "";
        return String.join(" ", extractWords(text)).toLowerCase();
    }

    private List<String> extractWords(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String norm = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        Matcher m = WORD_PATTERN.matcher(norm);
        List<String> tokens = new ArrayList<>();
        while (m.find()) tokens.add(m.group());
        return tokens;
    }

    private String safeForLog(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]+", " ").trim();
    }

    private String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "EN";
        int bnCount = 0, enCount = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BENGALI) bnCount++;
            else if (Character.isLetter(c) && c < 128) enCount++;
        }
        return bnCount > enCount ? "BN" : "EN";
    }

    // ---------- NEW: Wikipedia + Gemini + HuggingFace ----------
    // private String fetchExtraInfo(String query) {
    //     // Wikipedia first
    //     try {
    //         String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
    //                 query.trim().replace(" ", "_");
    //         HttpRequest req = HttpRequest.newBuilder()
    //                 .uri(URI.create(url))
    //                 .header("User-Agent", "LegalFAQBot/1.0")
    //                 .GET()
    //                 .build();

    //         HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    //         if (resp.statusCode() == 200 && resp.body().contains("\"extract\"")) {
    //             String body = resp.body();
    //             int idx = body.indexOf("\"extract\":\"");
    //             if (idx != -1) {
    //                 String extract = body.substring(idx + 11);
    //                 extract = extract.split("\",\"")[0];
    //                 return extract.replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"");
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println("Wiki fetch failed: " + e.getMessage());
    //     }

    //     // Gemini fallback
    //     try {
    //         String apiKey = System.getenv("GEMINI_API_KEY");
    //         if (apiKey != null && !apiKey.isBlank()) {
    //             String payload = "{ \"contents\": [{ \"parts\":[{\"text\":\"" + query.replace("\"","'") + "\"}]}]}";

    //             HttpRequest req = HttpRequest.newBuilder()
    //                     .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
    //                     .header("Content-Type", "application/json")
    //                     .POST(HttpRequest.BodyPublishers.ofString(payload))
    //                     .build();

    //             HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    //             if (resp.statusCode() == 200 && resp.body().contains("text")) {
    //                 String body = resp.body();
    //                 int idx = body.indexOf("\"text\":");
    //                 if (idx != -1) {
    //                     String cut = body.substring(idx + 8);
    //                     String out = cut.split("\"")[1];
    //                     return out.trim();
    //                 }
    //             } else {
    //                 System.err.println("Gemini returned status " + resp.statusCode() + " body: " + resp.body());
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println("Gemini fetch failed: " + e.getMessage());
    //     }

    //     // HuggingFace fallback (free Falcon-7B-Instruct)
    //     try {
    //         String apiKey = System.getenv("HUGGINGFACE_API_KEY");
    //         if (apiKey != null && !apiKey.isBlank()) {
    //             String payload = "{ \"inputs\": \"" + query.replace("\"", "'") + "\", " +
    //                     "\"parameters\": { \"max_new_tokens\": 120, \"temperature\": 0.85 } }";

    //             HttpRequest req = HttpRequest.newBuilder()
    //                     .uri(URI.create("https://api-inference.huggingface.co/models/tiiuae/falcon-7b-instruct"))
    //                     .header("Authorization", "Bearer " + apiKey)
    //                     .header("Content-Type", "application/json")
    //                     .POST(HttpRequest.BodyPublishers.ofString(payload))
    //                     .build();

    //             HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    //             if (resp.statusCode() == 200) {
    //                 String body = resp.body();
    //                 int start = body.indexOf("generated_text");
    //                 if (start != -1) {
    //                     String cut = body.substring(start + 17);
    //                     String out = cut.split("\"")[0];
    //                     return out.trim();
    //                 }
    //             } else {
    //                 System.err.println("HuggingFace returned status " + resp.statusCode() + " body: " + resp.body());
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println("HF fetch failed: " + e.getMessage());
    //     }

    //     return "(no extra info available)";
    // }
    private String fetchExtraInfo(String query) {
    if (query == null || query.isBlank()) return "(no extra info available)";
    String trimmed = query.trim();

    // 1) Wikipedia summary
    try {
        String wikiEncoded = java.net.URLEncoder.encode(trimmed.replace(" ", "_"), java.nio.charset.StandardCharsets.UTF_8);
        String wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + wikiEncoded;

        HttpRequest wikiReq = HttpRequest.newBuilder()
                .uri(URI.create(wikiUrl))
                .header("User-Agent", "LegalFAQBot/1.0")
                .GET()
                .build();

        HttpResponse<String> wikiResp = httpClient.send(wikiReq, HttpResponse.BodyHandlers.ofString());
        System.err.println("Wiki returned status " + wikiResp.statusCode());
        if (wikiResp.statusCode() == 200 && wikiResp.body() != null && wikiResp.body().contains("\"extract\"")) {
            String body = wikiResp.body();
            int idx = body.indexOf("\"extract\":\"");
            if (idx != -1) {
                String extract = body.substring(idx + 11);
                // cut at the next '","' or closing quote
                int cut = extract.indexOf("\",\"");
                if (cut == -1) cut = extract.indexOf("\"}");
                if (cut == -1) {
                    int q = extract.indexOf("\"");
                    if (q != -1) cut = q;
                }
                if (cut == -1) cut = extract.length();
                String text = extract.substring(0, cut)
                        .replaceAll("\\\\n", " ")
                        .replaceAll("\\\\\"", "\"")
                        .trim();
                if (!text.isEmpty()) return text;
            }
        }
    } catch (Exception e) {
        System.err.println("Wiki fetch failed: " + e.getMessage());
    }

    // 2) Gemini (try gemini-1.5-flash) - use API key if present
    try {
        String gemKey = System.getenv("GEMINI_API_KEY");
        if (gemKey != null && !gemKey.isBlank()) {
            String safeQ = trimmed.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
            String gemBody = "{"
                    + "\"contents\":[{\"parts\":[{\"text\":\"" + safeQ + "\"}]}],"
                    + "\"generationConfig\":{"
                    + "\"temperature\":0.85,"
                    + "\"maxOutputTokens\":120"
                    + "}"
                    + "}";

            String gemUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + gemKey;
            HttpRequest gemReq = HttpRequest.newBuilder()
                    .uri(URI.create(gemUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gemBody))
                    .build();

            HttpResponse<String> gemResp = httpClient.send(gemReq, HttpResponse.BodyHandlers.ofString());
            System.err.println("Gemini returned status " + gemResp.statusCode() + " body preview: " + nullablePreview(gemResp.body()));
            if (gemResp.statusCode() == 200 && gemResp.body() != null) {
                String body = gemResp.body();

                // try a few heuristic extraction patterns (covers typical Gemini responses)
                String candidate = extractFirstMatch(body, new String[] {
                        "\"text\":\"",                // simple "text"
                        "\"outputText\":\"",          // variants
                        "\"generated_text\":\"",      // some providers
                        "\"content\":{\"parts\":[{\"text\":\"", // nested content.parts[0].text
                });
                if (candidate != null && !candidate.isBlank()) {
                    return candidate.replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"").trim();
                }

                // try regex to find parts text under candidates/content/parts
                Pattern p = Pattern.compile("\"candidates\"\\s*:\\s*\\[.*?\"text\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
                Matcher m = p.matcher(body);
                if (m.find()) {
                    return m.group(1).replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"").trim();
                }
            } else {
                // if not 200, proceed to HF fallback (but log)
                System.err.println("Gemini call non-200: " + gemResp.statusCode());
            }
        } else {
            System.err.println("GEMINI_API_KEY not set — skipping Gemini.");
        }
    } catch (Exception e) {
        System.err.println("Gemini fetch failed: " + e.getMessage());
    }

    // 3) Hugging Face fallback (free-friendly model: tiiuae/falcon-7b-instruct)
    try {
        String hfKey = System.getenv("HUGGINGFACE_API_KEY");
        if (hfKey != null && !hfKey.isBlank()) {
            String safeQ = trimmed.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
            String hfPayload = "{ \"inputs\": \"" + safeQ + "\", \"parameters\": { \"max_new_tokens\": 120, \"temperature\": 0.85 } }";

            HttpRequest hfReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-inference.huggingface.co/models/tiiuae/falcon-7b-instruct"))
                    .header("Authorization", "Bearer " + hfKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(hfPayload))
                    .build();

            HttpResponse<String> hfResp = httpClient.send(hfReq, HttpResponse.BodyHandlers.ofString());
            System.err.println("HuggingFace returned status " + hfResp.statusCode() + " body preview: " + nullablePreview(hfResp.body()));
            if (hfResp.statusCode() == 200 && hfResp.body() != null) {
                String body = hfResp.body();

                // common HF responses contain "generated_text"
                String gen = extractFirstMatch(body, new String[] { "\"generated_text\":\"", "\"generated_text\": \"", "\"text\":\"", "\"answer\":\"" });
                if (gen != null && !gen.isBlank()) {
                    return gen.replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"").trim();
                }

                // regex fallback: {"generated_text":"..."} inside array/object
                Pattern p = Pattern.compile("\"generated_text\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = p.matcher(body);
                if (m.find()) return m.group(1).replaceAll("\\\\n", " ").replaceAll("\\\\\"", "\"").trim();

                // if body is plain text (rare), return it
                String trimmedBody = body.trim();
                if (!trimmedBody.isEmpty() && !trimmedBody.toLowerCase().contains("error")) {
                    return trimmedBody;
                }
            } else {
                System.err.println("HuggingFace non-200: " + hfResp.statusCode() + " preview: " + nullablePreview(hfResp.body()));
            }
        } else {
            System.err.println("HUGGINGFACE_API_KEY not set — skipping HuggingFace.");
        }
    } catch (Exception e) {
        System.err.println("HF fetch failed: " + e.getMessage());
    }

    // 4) all failed
    return "(no extra info available)";
}

    // add inside FaqService class (bottom is fine)

private String nullablePreview(String input) {
    if (input == null) return "(null)";
    int len = Math.min(120, input.length());
    return input.substring(0, len).replaceAll("\\s+", " ") + (input.length() > 120 ? "..." : "");
}

private String extractFirstMatch(String input, String[] patterns) {
    if (input == null) return null;
    for (String p : patterns) {
        int idx = input.indexOf(p);
        if (idx != -1) {
            String cut = input.substring(idx + p.length());
            int end = cut.indexOf("\"");
            if (end != -1) return cut.substring(0, end);
            return cut; // fallback if no quote
        }
    }
    return null;
}


}
