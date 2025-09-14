package com.example.faq.controller;

import com.example.faq.model.Review;
import com.example.faq.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{lawyerId}")
    public Review addReview(@PathVariable Long lawyerId, @RequestBody Review review) {
        return reviewService.addReview(lawyerId, review);
    }

    @GetMapping("/lawyer/{lawyerId}")
    public List<Review> getReviewsForLawyer(@PathVariable Long lawyerId) {
        return reviewService.getReviewsByLawyer(lawyerId);
    }
}
