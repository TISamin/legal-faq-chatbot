package com.example.faq.service;

import com.example.faq.model.Lawyer;
import com.example.faq.model.Review;
import com.example.faq.repository.LawyerRepository;
import com.example.faq.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LawyerRepository lawyerRepository;

    public ReviewService(ReviewRepository reviewRepository, LawyerRepository lawyerRepository) {
        this.reviewRepository = reviewRepository;
        this.lawyerRepository = lawyerRepository;
    }

    // Add review to lawyer
    public Review addReview(Long lawyerId, Review review) {
        Optional<Lawyer> lawyer = lawyerRepository.findById(lawyerId);
        if (lawyer.isPresent()) {
            review.setLawyer(lawyer.get());
            return reviewRepository.save(review);
        }
        throw new RuntimeException("Lawyer not found with ID: " + lawyerId);
    }

    // Get all reviews of a lawyer
    public List<Review> getReviewsByLawyer(Long lawyerId) {
        return reviewRepository.findByLawyerId(lawyerId);
    }
}
