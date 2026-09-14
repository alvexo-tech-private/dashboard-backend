package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to vehicle-service-booking-api's existing `users` table — this table
 * holds riders (VEHICLE_USER), mechanics/workshops (MECHANIC), sales
 * representatives (SALES_REPRESENTATIVE) and that service's own admins
 * (ADMINISTRATOR), distinguished by `role`. Not owned by this service: no
 * Liquibase changeset here creates or alters it (see dashboard/CLAUDE.md).
 * The password/refresh-token fields from the source entity are intentionally
 * omitted — this service has no business reading rider/mechanic credentials.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "mobile_verified")
    private Boolean mobileVerified;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String city;
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "workshop_name")
    private String workshopName;

    private String area;

    // Mechanic-specific fields
    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "total_bookings_completed")
    private Integer totalBookingsCompleted;

    @Column(name = "certification_details", columnDefinition = "TEXT")
    private String certificationDetails;

    // Sales-representative fields
    @Column(name = "referral_code", unique = true, length = 50)
    private String referralCode;

    @Column(name = "total_referrals")
    private Integer totalReferrals;

    @Column(name = "total_bonus_earned", precision = 10, scale = 2)
    private BigDecimal totalBonusEarned;

    @Column(nullable = false)
    private Boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
