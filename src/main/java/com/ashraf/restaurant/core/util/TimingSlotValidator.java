package com.ashraf.restaurant.core.util;

import com.ashraf.restaurant.core.dto.TimingSlotRequest;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TimingSlotValidator {

    private TimingSlotValidator() {}

    public static void validate(List<TimingSlotRequest> slots) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < slots.size(); i++) {
            TimingSlotRequest slot = slots.get(i);
            int rowNum = i + 1;
            if (slot.getDayOfWeek() == null) {
                errors.add("slot " + rowNum + ": dayOfWeek is required");
                continue;
            }
            if (slot.getOpenTime() == null || slot.getCloseTime() == null) {
                errors.add(slot.getDayOfWeek() + ": openTime and closeTime are required");
                continue;
            }
            if (!slot.getOpenTime().isBefore(slot.getCloseTime())) {
                errors.add(slot.getDayOfWeek() + ": openTime must be before closeTime");
            }
        }

        Map<DayOfWeek, List<TimingSlotRequest>> byDay = slots.stream()
                .filter(s -> s.getDayOfWeek() != null && s.getOpenTime() != null && s.getCloseTime() != null)
                .collect(Collectors.groupingBy(TimingSlotRequest::getDayOfWeek));

        for (Map.Entry<DayOfWeek, List<TimingSlotRequest>> entry : byDay.entrySet()) {
            List<TimingSlotRequest> daySlots = new ArrayList<>(entry.getValue());
            daySlots.sort(Comparator.comparing(TimingSlotRequest::getOpenTime));
            for (int i = 1; i < daySlots.size(); i++) {
                if (daySlots.get(i).getOpenTime().isBefore(daySlots.get(i - 1).getCloseTime())) {
                    errors.add(entry.getKey() + ": shifts overlap");
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Timing validation failed: " + String.join("; ", errors));
        }
    }
}