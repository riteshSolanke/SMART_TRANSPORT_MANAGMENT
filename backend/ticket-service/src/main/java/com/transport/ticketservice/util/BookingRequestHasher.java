package com.transport.ticketservice.util;

import com.transport.ticketservice.dto.request.TicketRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class BookingRequestHasher {
    public String hash(Long ownerId, TicketRequestDto request) {
        String canonical = String.join("|",
                ownerId.toString(),
                request.getRouteId().toString(),
                request.getScheduleId().toString(),
                request.getSourceStopId().toString(),
                request.getDestinationStopId().toString(),
                request.getServiceDate().toString(),
                request.getPassengerCount().toString());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
