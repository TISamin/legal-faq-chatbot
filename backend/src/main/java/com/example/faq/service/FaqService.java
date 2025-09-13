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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Service
public class FaqService {
    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public String getAnswer(String question, String languageOrLang) {
        String lang = normalizeLang(languageOrLang);
        String q = question.trim();

        // 1. Try exact match
        Faq exact = faqRepository.findExact(q, lang);
        if (exact != null) {
            return exact.getAnswer();
        }

        // 2. If single-word query → use keyword LIKE search and prefer shortest question
        if (!q.contains(" ")) {
            List<Faq> candidates = faqRepository.findByKeyword(q, lang);
            if (!candidates.isEmpty()) {
                Faq best = candidates.stream()
                        .min(Comparator.comparingInt(f -> f.getQuestion().length()))
                        .orElse(candidates.get(0));
                return best.getAnswer();
            }
        }

        // 3. Otherwise → use fulltext search
        List<Faq> faqs = faqRepository.searchByKeyword(q, lang);
        if (!faqs.isEmpty()) {
            return faqs.get(0).getAnswer();
        }

        // 4. Try Wikipedia if DB fails
        String wikiAnswer = searchWikipedia(q, lang);
        if (wikiAnswer != null && containsKeywords(q, wikiAnswer)) {
            return wikiAnswer;
        }

        // 5. Fallback message
        if (lang.equals("BN")) {
            return "দুঃখিত, আমি এটির নির্দিষ্ট উত্তর খুঁজে পাইনি। অনুগ্রহ করে প্রশ্নটি ইংরেজিতে করে দেখুন।";
        } else {
            return "Sorry, I couldn’t find an exact answer. Please try rephrasing your question in English.";
        }
    }

    // --- Helpers ---

    private String normalizeLang(String language) {
        if (language == null) return "EN";
        String l = language.trim().toUpperCase();
        if (l.startsWith("B")) return "BN";
        return "EN";
    }

    private boolean containsKeywords(String question, String wikiAnswer) {
        if (wikiAnswer == null || wikiAnswer.isBlank()) return false;

        String qLower = question.toLowerCase();
        String answerLower = wikiAnswer.toLowerCase();

        // Full phrase check first
        if (answerLower.contains(qLower)) return true;

        // Word match threshold
        String[] keywords = qLower.split("\\s+");
        int matches = 0;
        for (String k : keywords) {
            if (answerLower.contains(k)) matches++;
        }
        return matches >= 2;
    }

    private String searchWikipedia(String question, String language) {
        try {
            String langCode = language.equals("BN") ? "bn" : "en";
            String encoded = URLEncoder.encode(question, StandardCharsets.UTF_8);
            String urlStr = "https://" + langCode + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + encoded + "&format=json&utf8=&srlimit=1";

            HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "LegalFAQBot/1.0 (https://example.com)");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                String result = response.toString();
                int snippetIndex = result.indexOf("\"snippet\":\"");
                if (snippetIndex != -1) {
                    int start = snippetIndex + 11;
                    int end = result.indexOf("\"", start);
                    if (end > start) {
                        String snippet = result.substring(start, end);
                        return snippet.replaceAll("<[^>]+>", ""); // remove HTML tags
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Wikipedia fetch failed: " + e.getMessage());
        }
        return null;
    }
}

