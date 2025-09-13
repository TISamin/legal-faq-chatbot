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

    // 1. Exact question match (ignores case and trims spaces)
    @Query(value = "SELECT * FROM faq " +
                   "WHERE language = :language " +
                   "AND LOWER(TRIM(question)) = LOWER(TRIM(:question)) " +
                   "LIMIT 1",
           nativeQuery = true)
    Faq findExact(@Param("question") String question,
                  @Param("language") String language);

    // 2. Keyword LIKE search for single-word queries
    @Query(value = "SELECT * FROM faq " +
                   "WHERE language = :language " +
                   "AND question LIKE %:keyword% " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Faq> findByKeyword(@Param("keyword") String keyword,
                            @Param("language") String language);

    // 3. Fulltext search (main fallback for multi-word queries)
    @Query(value = "SELECT * FROM faq " +
                   "WHERE language = :language " +
                   "AND MATCH(question, answer) AGAINST (:question IN NATURAL LANGUAGE MODE) " +
                   "LIMIT 5",
           nativeQuery = true)
    List<Faq> searchByKeyword(@Param("question") String question,
                              @Param("language") String language);
}
