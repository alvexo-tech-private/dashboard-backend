package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.dto.DashboardSummaryDto;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public MyApiResponse<DashboardSummaryDto> getSummary() {
        return MyApiResponse.success(dashboardService.getSummary());
    }
}
