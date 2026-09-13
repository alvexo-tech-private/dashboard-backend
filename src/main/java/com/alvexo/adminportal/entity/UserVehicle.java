package com.alvexo.adminportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Maps to the existing `user_vehicles` table — a rider's ownership of a vehicle. */
@Entity
@Table(name = "user_vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "ownership_type", length = 50)
    private String ownershipType;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "registration_year")
    private Integer registrationYear;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
