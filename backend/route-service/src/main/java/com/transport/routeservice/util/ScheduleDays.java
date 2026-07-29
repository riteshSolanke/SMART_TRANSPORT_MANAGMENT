package com.transport.routeservice.util;

import com.transport.routeservice.enums.DayOfWeekEnum;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ScheduleDays {

    private ScheduleDays() {
    }

    public static String normalize(String rawDays) {
        return parse(rawDays).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    public static boolean runsOn(String rawDays, DayOfWeek dayOfWeek) {
        DayOfWeekEnum requestedDay =
                DayOfWeekEnum.valueOf(dayOfWeek.name().substring(0, 3));
        return parse(rawDays).contains(requestedDay);
    }

    public static boolean overlaps(String first, String second) {
        EnumSet<DayOfWeekEnum> sharedDays = parse(first);
        sharedDays.retainAll(parse(second));
        return !sharedDays.isEmpty();
    }

    private static EnumSet<DayOfWeekEnum> parse(String rawDays) {
        if (rawDays == null || rawDays.isBlank()) {
            throw new IllegalArgumentException("At least one operating day is required");
        }

        String normalized = rawDays.trim().toUpperCase(Locale.ROOT);
        if ("DAILY".equals(normalized)) {
            return EnumSet.allOf(DayOfWeekEnum.class);
        }

        EnumSet<DayOfWeekEnum> result = EnumSet.noneOf(DayOfWeekEnum.class);
        try {
            Arrays.stream(normalized.split("[,\\s]+"))
                    .filter(token -> !token.isBlank())
                    .map(DayOfWeekEnum::valueOf)
                    .forEach(result::add);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Days of week must use MON,TUE,WED,THU,FRI,SAT,SUN or DAILY");
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("At least one operating day is required");
        }
        return result;
    }
}
