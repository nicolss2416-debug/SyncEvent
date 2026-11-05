package com.syncevent.attendance.api;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.syncevent.attendance.application.AttendanceLiveStatsService;

import reactor.core.publisher.Flux;

@Controller
public class AttendanceLiveGraphQlController {

    private final AttendanceLiveStatsService liveStatsService;

    public AttendanceLiveGraphQlController(AttendanceLiveStatsService liveStatsService) {
        this.liveStatsService = liveStatsService;
    }

    @QueryMapping("attendanceLiveStats")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'MONITOR')")
    public AttendanceLiveStatsResponse attendanceLiveStats(@Argument UUID eventId) {
        return AttendanceLiveStatsResponse.from(liveStatsService.getCurrentStats(eventId));
    }

    @SubscriptionMapping("attendanceLive")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'MONITOR')")
    public Flux<AttendanceLiveStatsResponse> attendanceLive(@Argument UUID eventId) {
        return liveStatsService.subscribe(eventId).map(AttendanceLiveStatsResponse::from);
    }
}
