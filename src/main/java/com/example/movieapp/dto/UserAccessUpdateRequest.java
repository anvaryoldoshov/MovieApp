package com.example.movieapp.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UserAccessUpdateRequest {
    private Map<Long, Integer> seriesAccessMap;

    private Boolean subscription;
    private Integer subscriptionDays;
    /** Admin "to'lov qildi" deb belgilagan seriallar: seriesId -> summa (so'mda); summa bo'sh bo'lsa serial narxi olinadi. */
    private Map<Long, Long> manualPayments;
}