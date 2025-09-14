package com.example.faq.controller;

import com.example.faq.model.Lawyer;
import com.example.faq.service.LawyerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lawyers")
@CrossOrigin(origins = "*")
public class LawyerController {

    private final LawyerService lawyerService;

    public LawyerController(LawyerService lawyerService) {
        this.lawyerService = lawyerService;
    }

    @PostMapping
    public Lawyer addLawyer(@RequestBody Lawyer lawyer) {
        return lawyerService.saveLawyer(lawyer);
    }

    @GetMapping
    public List<Lawyer> getAllLawyers() {
        return lawyerService.getAllLawyers();
    }

    @GetMapping("/{id}")
    public Optional<Lawyer> getLawyer(@PathVariable Long id) {
        return lawyerService.getLawyerById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteLawyer(@PathVariable Long id) {
        lawyerService.deleteLawyer(id);
    }

    @GetMapping("/sort/reviews")
    public List<Lawyer> getLawyersSortedByReviews() {
        return lawyerService.sortByNumberOfReviews();
    }

    @GetMapping("/sort/rating")
    public List<Lawyer> getLawyersSortedByRating() {
        return lawyerService.sortByAverageRating();
    }
}
