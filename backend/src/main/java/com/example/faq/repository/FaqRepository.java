package com.example.faq.repository;

import com.example.faq.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByQuestionContainingIgnoreCaseAndLanguage(String question, String language);
}
