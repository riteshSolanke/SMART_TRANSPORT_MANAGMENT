package com.transport.ticketservice.entity;


import com.transport.ticketservice.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column(nullable = false, unique = true, length = 20)
    private String pnrNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long routeId;

    @Column(nullable = false)
    private Long sourceStopId;

    @Column(nullable = false)
    private Long destinationStopId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fareAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(nullable = false)
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime travelDate;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.bookedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TicketStatus.BOOKED;
        }

    }

}
