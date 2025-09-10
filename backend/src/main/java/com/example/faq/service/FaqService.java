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
package com.example.faq.service;

import com.example.faq.model.Faq;
import com.example.faq.repository.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FaqService {

    private final FaqRepository faqRepository;
    private final RestTemplate restTemplate;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
        this.restTemplate = new RestTemplate();
    }

    public String getAnswer(String question, String language) {
        List<Faq> faqs = faqRepository.searchByQuestion(question, language);

        if (!faqs.isEmpty()) {
            return faqs.get(0).getAnswer();
        }

        // If Bangla question, prompt to try English
        if ("BN".equalsIgnoreCase(language)) {
            return "এই প্রশ্নটি খুব নির্দিষ্ট বা জটিল। অনুগ্রহ করে ইংরেজিতে লিখে চেষ্টা করুন।";
        }

        // Wikipedia fallback for English
        try {
            String encodedQuestion = UriUtils.encode(question, StandardCharsets.UTF_8);
            String wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedQuestion;

            WikipediaResponse response = restTemplate.getForObject(wikiUrl, WikipediaResponse.class);
            if (response != null && response.extract != null && !response.extract.isEmpty()) {
                return response.extract;
            }
        } catch (Exception e) {
            // ignore errors, fallback to polite message
        }

        return "This question is very specific or complex. Please try rephrasing or searching elsewhere.";
    }

    // Inner class to map Wikipedia JSON
    private static class WikipediaResponse {
        public String extract;
    }
}
