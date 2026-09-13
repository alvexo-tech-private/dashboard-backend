package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<MechanicSummaryDto> getMechanics() {
        return userRepository.findByRole(UserRole.MECHANIC).stream()
                .map(this::toMechanicSummaryDto)
                .toList();
    }

    public List<VehicleUserSummaryDto> getVehicleUsers() {
        return userRepository.findByRole(UserRole.VEHICLE_USER).stream()
                .map(this::toVehicleUserSummaryDto)
                .toList();
    }

    private MechanicSummaryDto toMechanicSummaryDto(User user) {
        return MechanicSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .workshopName(user.getWorkshopName())
                .city(user.getCity())
                .state(user.getState())
                .specialization(user.getSpecialization())
                .experienceYears(user.getExperienceYears())
                .hourlyRate(user.getHourlyRate())
                .rating(user.getRating())
                .active(user.getActive())
                .build();
    }

    private VehicleUserSummaryDto toVehicleUserSummaryDto(User user) {
        return VehicleUserSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .city(user.getCity())
                .state(user.getState())
                .emailVerified(user.getEmailVerified())
                .mobileVerified(user.getMobileVerified())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
