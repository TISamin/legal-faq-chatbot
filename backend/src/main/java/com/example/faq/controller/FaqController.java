package com.example.faq.controller;

import com.example.faq.service.FaqService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faq")
@CrossOrigin(origins = "*")
public class FaqController {
    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    // Primary endpoint: /api/faq?question=...&language=EN
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String getAnswer(@RequestParam String question,
                            @RequestParam(name = "language", defaultValue = "EN") String language,
                            @RequestParam(name = "lang", required = false) String lang) {
        String effectiveLang = (lang != null && !lang.isBlank()) ? lang : language;
        return faqService.getAnswer(question, effectiveLang);
    }

    // Secondary alias endpoint: /api/faq/ask?question=...&lang=EN
    @GetMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public String ask(@RequestParam String question,
                      @RequestParam(name = "lang", defaultValue = "EN") String lang) {
        return faqService.getAnswer(question, lang);
    }
}
