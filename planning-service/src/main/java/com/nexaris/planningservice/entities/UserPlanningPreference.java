package com.nexaris.planningservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "user_planning_preferences")
public class UserPlanningPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "work_day_start", nullable = false)
    private LocalTime workDayStart;

    @Column(name = "work_day_end", nullable = false)
    private LocalTime workDayEnd;

    @Column(name = "preferred_meeting_block_minutes", nullable = false)
    private Integer preferredMeetingBlockMinutes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalTime getWorkDayStart() {
        return workDayStart;
    }

    public void setWorkDayStart(LocalTime workDayStart) {
        this.workDayStart = workDayStart;
    }

    public LocalTime getWorkDayEnd() {
        return workDayEnd;
    }

    public void setWorkDayEnd(LocalTime workDayEnd) {
        this.workDayEnd = workDayEnd;
    }

    public Integer getPreferredMeetingBlockMinutes() {
        return preferredMeetingBlockMinutes;
    }

    public void setPreferredMeetingBlockMinutes(Integer preferredMeetingBlockMinutes) {
        this.preferredMeetingBlockMinutes = preferredMeetingBlockMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
