// package com.example.faq.repository;

// import com.example.faq.model.Faq;
// import org.springframework.data.jpa.repository.JpaRepository;

// import java.util.List;

// public interface FaqRepository extends JpaRepository<Faq, Long> {
//     List<Faq> findByQuestionContainingIgnoreCaseAndLanguage(String question, String language);
// }
// ************************************************************
// package com.example.faq.repository;

// import com.example.faq.model.Faq;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

// import java.util.List;

// public interface FaqRepository extends JpaRepository<Faq, Long> {

    
//     @Query(value = "SELECT * FROM faq WHERE language = :language AND MATCH(question, answer) AGAINST (:question IN NATURAL LANGUAGE MODE) ORDER BY MATCH(question, answer) AGAINST (:question IN NATURAL LANGUAGE MODE) DESC LIMIT 3", nativeQuery = true)
//     List<Faq> searchFaq(@Param("question") String question, @Param("language") String language);
// }
//****************************
// package com.example.faq.repository;

// import com.example.faq.model.Faq;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

// import java.util.List;

// public interface FaqRepository extends JpaRepository<Faq, Long> {

//     @Query(value = "SELECT * FROM faq " +
//                    "WHERE language = :language " +
//                    "AND MATCH(question, answer) AGAINST (:question IN NATURAL LANGUAGE MODE)",
//            nativeQuery = true)
//     List<Faq> searchByQuestion(@Param("question") String question,
//                                @Param("language") String language);
// }


package com.example.faq.repository;

import com.example.faq.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    @Query(value = "SELECT * FROM faq " +
                   "WHERE language = :language " +
                   "AND LOWER(TRIM(question)) = LOWER(TRIM(:question)) " +
                   "LIMIT 1",
           nativeQuery = true)
    Faq findExact(@Param("question") String question,
                  @Param("language") String language);

    @Query(value = "SELECT * FROM faq " +
                   "WHERE language = :language " +
                   "AND LOWER(question) LIKE CONCAT('%', LOWER(:keyword), '%') " +
                   "ORDER BY CHAR_LENGTH(question) ASC " +
                   "LIMIT 50",
           nativeQuery = true)
    List<Faq> findCandidates(@Param("keyword") String keyword,
                             @Param("language") String language);
}
