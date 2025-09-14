package com.example.faq.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating; // e.g. stars 1–5
    private String comment;

    @ManyToOne
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    // Constructors
    public Review() {}

    public Review(int rating, String comment, Lawyer lawyer) {
        this.rating = rating;
        this.comment = comment;
        this.lawyer = lawyer;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public int getRating() { return rating; }

    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public Lawyer getLawyer() { return lawyer; }

    public void setLawyer(Lawyer lawyer) { this.lawyer = lawyer; }
}
