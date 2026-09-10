package com.book_store.user_service.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Column(name = "id", nullable = false, unique = true)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String first_name;

    @Column(name = "last_name")
    private String last_name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "mobile_number", nullable = false)
    private String mobile_number;

    @Column(name = "secondary_mobile_number")
    private String secondaryMobileNumber;

    @Column(name = "gender")
    private String gender;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

}
