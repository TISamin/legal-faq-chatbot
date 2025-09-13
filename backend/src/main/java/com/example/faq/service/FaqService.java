// package com.example.faq.service;

// import com.example.faq.model.Faq;
// import com.example.faq.repository.FaqRepository;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class FaqService {
//     private final FaqRepository faqRepository;

//     public FaqService(FaqRepository faqRepository) {
//         this.faqRepository = faqRepository;
//     }

//     public String getAnswer(String question, String languageOrLang) {
//         String lang = normalizeLang(languageOrLang);
//         List<Faq> faqs = faqRepository.findByQuestionContainingIgnoreCaseAndLanguage(question, lang);
//         if (!faqs.isEmpty()) {
//             return faqs.get(0).getAnswer();
//         }
//         return "Sorry, I don’t have an answer to that yet. Please try another question.";
//     }

//     private String normalizeLang(String language) {
//         if (language == null) return "EN";
//         String l = language.trim().toUpperCase();
//         if (l.startsWith("B")) return "BN";
//         return "EN";
//     }
// }
//************************************************************************************
// package com.example.faq.service;

// import com.example.faq.model.Faq;
// import com.example.faq.repository.FaqRepository;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class FaqService {
//     private final FaqRepository faqRepository;

//     public FaqService(FaqRepository faqRepository) {
//         this.faqRepository = faqRepository;
//     }

//     public String getAnswer(String question, String languageOrLang) {
//         String lang = normalizeLang(languageOrLang);
//         List<Faq> faqs = faqRepository.searchFaq(question, lang); // FULLTEXT search
//         if (!faqs.isEmpty()) {
//             return faqs.get(0).getAnswer(); // best match
//         }
//         return "Sorry, I don’t have an answer to that yet. Please try another question.";
//     }

//     private String normalizeLang(String language) {
//         if (language == null) return "EN";
//         String l = language.trim().toUpperCase();
//         if (l.startsWith("B")) return "BN";
//         return "EN";
//     }
// }
//**************************************************

// package com.example.faq.service;

// import com.example.faq.model.Faq;
// import com.example.faq.repository.FaqRepository;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.util.UriUtils;

// import java.nio.charset.StandardCharsets;
// import java.util.List;

// @Service
// public class FaqService {

//     private final FaqRepository faqRepository;
//     private final RestTemplate restTemplate;

//     public FaqService(FaqRepository faqRepository) {
//         this.faqRepository = faqRepository;
//         this.restTemplate = new RestTemplate();
//     }

//     public String getAnswer(String question, String language) {
//         List<Faq> faqs = faqRepository.searchByQuestion(question, language);

//         if (!faqs.isEmpty()) {
//             return faqs.get(0).getAnswer();
//         }

//         // If Bangla question, prompt to try English
//         if ("BN".equalsIgnoreCase(language)) {
//             return "এই প্রশ্নটি খুব নির্দিষ্ট বা জটিল। অনুগ্রহ করে ইংরেজিতে লিখে চেষ্টা করুন।";
//         }

//         // Wikipedia fallback for English
//         try {
//             String encodedQuestion = UriUtils.encode(question, StandardCharsets.UTF_8);
//             String wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedQuestion;

//             WikipediaResponse response = restTemplate.getForObject(wikiUrl, WikipediaResponse.class);
//             if (response != null && response.extract != null && !response.extract.isEmpty()) {
//                 return response.extract;
//             }
//         } catch (Exception e) {
//             // ignore errors, fallback to polite message
//         }

//         return "This question is very specific or complex. Please try rephrasing or searching elsewhere.";
//     }

//     // Inner class to map Wikipedia JSON
//     private static class WikipediaResponse {
//         public String extract;
//     }
// }
package com.example.faq.service;

import com.example.faq.model.Faq;
import com.example.faq.repository.FaqRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    // ---------------- CONFIG ----------------
    // Edit these stopword sets if you want to change ignored words.
    private static final Set<String> STOPWORDS_EN = Set.of(
            "what","is","a","an","the","of","in","on","to","for","and","between",
            "how","do","does","did","can","should","will","my","your","its","it's",
            "who","whom","where","when","which","with","by","about","that","this",
            "these","those","as","are","was","were","be","been","being","at","from","why"
    );

    private static final Set<String> STOPWORDS_BN = Set.of(
            // add/remove Bengali stopwords as needed
            "কি","কী","কেন","কখন","কোথায়","কোথায়","কিভাবে","কীভাবে",
            "এবং","এই","ও","তার","আমি","আপনি","হয়ে","হয়","হবে","থেকে","তে","করে","ছে"
    );

    // How many tokens to use for candidate keyword fetch (first N tokens)
    private static final int TOKEN_FETCH_LIMIT = 5;

    // Regex to extract letter sequences (works for Latin and Indic scripts)
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");

    // Minimum accepted overlap (you can increase if you want stricter matches)
    private static final double MIN_ACCEPTED_SCORE = 0.0; // 0.0 means accept best even if single token matches

    // ----------------------------------------

    /**
     * Main entry: returns the best DB answer using Option B (LIKE + percentage overlap).
     */
    public String getAnswer(String question, String languageOrLang) {
        if (question == null || question.isBlank()) {
            return "⚠️ Please enter a valid question.";
        }

        String lang = normalizeLang(languageOrLang);
        String rawQuery = question.trim();

        // 1) Try a strict exact-match short-circuit (fast)
        try {
            Faq exact = faqRepository.findExact(rawQuery, lang);
            if (exact != null) {
                return safeAnswer(exact);
            }
        } catch (Exception e) {
            // ignore and continue to candidate fetch — do not fail the request
            System.err.println("Exact-match query failed: " + e.getMessage());
        }

        // 2) Tokenize and remove stopwords
        List<String> allQueryTokens = extractWords(rawQuery).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Set<String> stopwords = lang.equals("BN") ? STOPWORDS_BN : STOPWORDS_EN;

        List<String> userTokens = allQueryTokens.stream()
                .filter(t -> !stopwords.contains(t))
                .collect(Collectors.toList());

        // If removing stopwords leaves nothing, use the raw tokens
        if (userTokens.isEmpty()) {
            userTokens = new ArrayList<>(allQueryTokens);
        }

        if (userTokens.isEmpty()) { // still empty -> fallback
            return fallbackMessage(lang);
        }

        // 3) Fetch candidates (LIKE). We'll call findCandidates with the full query and then with first tokens
        LinkedHashMap<Long, Faq> candidates = new LinkedHashMap<>(); // dedupe preserving order

        // Try full phrase first (helps when DB question contains whole phrase)
        tryAddCandidates(rawQuery, lang, candidates);

        // Then try by tokens: up to TOKEN_FETCH_LIMIT tokens (to widen pool but limit DB calls)
        int lim = Math.min(TOKEN_FETCH_LIMIT, userTokens.size());
        for (int i = 0; i < lim; ++i) {
            tryAddCandidates(userTokens.get(i), lang, candidates);
        }

        if (candidates.isEmpty()) {
            return fallbackMessage(lang);
        }

        // 4) Check normalized exact among candidates (handles punctuation differences)
        String normalizedQuery = normalizeForCompare(rawQuery);
        for (Faq f : candidates.values()) {
            if (normalizeForCompare(f.getQuestion()).equals(normalizedQuery)) {
                return safeAnswer(f);
            }
        }

        // 5) Score candidates by percentage-overlap
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

            // remove stopwords from candidate token set too
            candTokens.removeAll(stopwords);

            int matches = 0;
            for (String t : userTokenSet) {
                if (candTokens.contains(t)) matches++;
            }

            double score = userTokenSet.isEmpty() ? 0.0 : ((double) matches / userTokenSet.size());

            int qlen = (f.getQuestion() == null) ? Integer.MAX_VALUE : f.getQuestion().length();

            // Tie-breaker: higher score wins; if equal prefer shorter question (definition)
            if (score > bestScore + 1e-9 ||
                    (Math.abs(score - bestScore) < 1e-9 && qlen < bestQuestionLength)) {
                bestScore = score;
                bestFaq = f;
                bestQuestionLength = qlen;
            }
        }

        // 6) Return best candidate if above threshold, else fallback to shortest candidate
        if (bestFaq != null && bestScore > MIN_ACCEPTED_SCORE) {
            return safeAnswer(bestFaq);
        }

        // If no meaningful overlap, return the shortest candidate as a practical fallback
        Faq fallback = candidates.values().stream()
                .min(Comparator.comparingInt(a -> a.getQuestion() == null ? Integer.MAX_VALUE : a.getQuestion().length()))
                .orElse(null);

        if (fallback != null) return safeAnswer(fallback);

        return fallbackMessage(lang);
    }

    // ---------- helpers ----------

    private void tryAddCandidates(String keyword, String lang, Map<Long, Faq> out) {
        if (keyword == null || keyword.isBlank()) return;
        try {
            List<Faq> found = faqRepository.findCandidates(keyword, lang);
            if (found != null) {
                for (Faq f : found) {
                    if (f != null && f.getId() != null) out.putIfAbsent(f.getId(), f);
                }
            }
        } catch (Exception e) {
            System.err.println("Candidate fetch failed for \"" + safeForLog(keyword) + "\": " + e.getMessage());
        }
    }

    private String safeAnswer(Faq f) {
        if (f == null) return fallbackMessage("EN");
        String a = f.getAnswer();
        if (a == null || a.isBlank()) return fallbackMessage("EN");
        return a.trim();
    }

    private String fallbackMessage(String lang) {
        if ("BN".equalsIgnoreCase(lang)) {
            return "দুঃখিত, আমি এটির নির্দিষ্ট উত্তর খুঁজে পাইনি। অনুগ্রহ করে প্রশ্নটি অন্যভাবে লিখে দেখুন।";
        }
        return "Sorry, I couldn't find an answer. Please try rephrasing your question.";
    }

    private String normalizeLang(String language) {
        if (language == null) return "EN";
        String l = language.trim().toUpperCase();
        return l.startsWith("B") ? "BN" : "EN";
    }

    private String normalizeForCompare(String text) {
        if (text == null) return "";
        List<String> words = extractWords(text);
        return String.join(" ", words).toLowerCase();
    }

    private List<String> extractWords(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String norm = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        Matcher m = WORD_PATTERN.matcher(norm);
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    private String safeForLog(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]+", " ").trim();
    }
}
