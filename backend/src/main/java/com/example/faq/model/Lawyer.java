// package com.example.faq.model;

// import jakarta.persistence.*;
// import java.util.List;

// @Entity
// @Table(name = "lawyers")
// public class Lawyer {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     private String name;
//     private String specialization;
//     private String email;
//     private String phone;

//     @OneToMany(mappedBy = "lawyer", cascade = CascadeType.ALL, orphanRemoval = true)
//     private List<Review> reviews;

//     // Constructors
//     public Lawyer() {}

//     public Lawyer(String name, String specialization, String email, String phone) {
//         this.name = name;
//         this.specialization = specialization;
//         this.email = email;
//         this.phone = phone;
//     }

//     // Getters and Setters
//     public Long getId() { return id; }

//     public void setId(Long id) { this.id = id; }

//     public String getName() { return name; }

//     public void setName(String name) { this.name = name; }

//     public String getSpecialization() { return specialization; }

//     public void setSpecialization(String specialization) { this.specialization = specialization; }

//     public String getEmail() { return email; }

//     public void setEmail(String email) { this.email = email; }

//     public String getPhone() { return phone; }

//     public void setPhone(String phone) { this.phone = phone; }

//     public List<Review> getReviews() { return reviews; }

//     public void setReviews(List<Review> reviews) { this.reviews = reviews; }
// }
package com.example.faq.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "lawyers") // explicitly map to the existing lawyers table
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String specialization;
    private String email;
    private String phone;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "cases_handled")
    private Integer casesHandled;

    @OneToMany(mappedBy = "lawyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;

    // Constructors
    public Lawyer() {}

    public Lawyer(String name, String specialization, String email, String phone, String photoUrl, Integer casesHandled) {
        this.name = name;
        this.specialization = specialization;
        this.email = email;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.casesHandled = casesHandled;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Integer getCasesHandled() { return casesHandled; }
    public void setCasesHandled(Integer casesHandled) { this.casesHandled = casesHandled; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}

