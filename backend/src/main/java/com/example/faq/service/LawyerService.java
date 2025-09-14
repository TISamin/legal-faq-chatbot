package com.example.faq.service;

import com.example.faq.model.Lawyer;
import com.example.faq.model.Review;
import com.example.faq.repository.LawyerRepository;
import com.example.faq.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LawyerService {

    private final LawyerRepository lawyerRepository;
    private final ReviewRepository reviewRepository;

    public LawyerService(LawyerRepository lawyerRepository, ReviewRepository reviewRepository) {
        this.lawyerRepository = lawyerRepository;
        this.reviewRepository = reviewRepository;
    }

    // Create or update lawyer
    public Lawyer saveLawyer(Lawyer lawyer) {
        return lawyerRepository.save(lawyer);
    }

    // Get all lawyers
    public List<Lawyer> getAllLawyers() {
        return lawyerRepository.findAll();
    }

    // Get single lawyer
    public Optional<Lawyer> getLawyerById(Long id) {
        return lawyerRepository.findById(id);
    }

    // Delete lawyer
    public void deleteLawyer(Long id) {
        lawyerRepository.deleteById(id);
    }

    // Sort lawyers by number of reviews (most clients handled)
    public List<Lawyer> sortByNumberOfReviews() {
        return lawyerRepository.findAll()
                .stream()
                .sorted((l1, l2) -> Integer.compare(
                        l2.getReviews().size(),
                        l1.getReviews().size()))
                .collect(Collectors.toList());
    }

    // Sort lawyers by average rating (highest first)
    public List<Lawyer> sortByAverageRating() {
        return lawyerRepository.findAll()
                .stream()
                .sorted((l1, l2) -> Double.compare(
                        calculateAverageRating(l2.getReviews()),
                        calculateAverageRating(l1.getReviews())))
                .collect(Collectors.toList());
    }

    // Helper method
    private double calculateAverageRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }
}
