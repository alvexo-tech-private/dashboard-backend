package com.alvexo.adminportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "Today" for dashboard counts is the business day in India, where the
 * platform operates — the same zone the booking API's scheduled jobs use —
 * not whatever zone the server happens to run in. A bean so tests can fix the date.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
