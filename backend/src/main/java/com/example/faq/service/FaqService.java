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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FaqService {
    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public String getAnswer(String question, String languageOrLang) {
        String lang = normalizeLang(languageOrLang);

        // 1. Try exact DB match
        List<Faq> exactMatch = faqRepository.findByQuestionAndLanguage(question.trim(), lang);
        if (!exactMatch.isEmpty()) {
            return exactMatch.get(0).getAnswer();
        }

        // 2. Try FULLTEXT DB search
        List<Faq> faqs = faqRepository.searchByKeyword(question, lang);
        String dbAnswer = faqs.isEmpty() ? null : faqs.get(0).getAnswer();
        double dbScore = calculateDbScore(question, faqs);

        if (dbAnswer != null && dbScore >= 0.7) {
            return dbAnswer;
        }

        // 3. Try Wikipedia
        String wikiAnswer = searchWikipedia(question, lang);
        if (wikiAnswer != null && !wikiAnswer.isBlank()) {
            return wikiAnswer;
        }

        // 4. Nothing found
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

    private double calculateDbScore(String question, List<Faq> faqs) {
        if (faqs.isEmpty()) return 0.0;
        String q = question.toLowerCase();
        String text = faqs.get(0).getQuestion().toLowerCase();
        String[] words = q.split("\\s+");
        int matchCount = 0;
        for (String w : words) {
            if (text.contains(w)) matchCount++;
        }
        return (double) matchCount / words.length;
    }

    // --- Wikipedia Search ---
    private String searchWikipedia(String question, String language) {
        try {
            String langCode = language.equals("BN") ? "bn" : "en";
            String encoded = URLEncoder.encode(question, StandardCharsets.UTF_8);
            String urlStr = "https://" + langCode +
                    ".wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&titles="
                    + encoded + "&format=json&utf8=";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "LegalFAQBot/1.0 (https://example.com)");

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }

                String result = response.toString();
                int idx = result.indexOf("\"extract\":\"");
                if (idx != -1) {
                    int start = idx + 11;
                    int end = result.indexOf("\"", start);
                    if (end > start) {
                        String snippet = result.substring(start, end);
                        return snippet.replaceAll("\\\\n", " ").trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Wikipedia fetch failed: " + e.getMessage());
        }
        return null;
    }
}




