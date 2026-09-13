package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/mechanics")
    public MyApiResponse<List<MechanicSummaryDto>> getMechanics() {
        return MyApiResponse.success(userService.getMechanics());
    }

    @GetMapping("/vehicle-users")
    public MyApiResponse<List<VehicleUserSummaryDto>> getVehicleUsers() {
        return MyApiResponse.success(userService.getVehicleUsers());
    }
}
