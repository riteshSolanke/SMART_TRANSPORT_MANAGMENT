package com.transport.routeservice.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "stops", uniqueConstraints = @UniqueConstraint(
        name = "uk_route_sequence", columnNames = {"route_id", "sequence_order" }
))
@Getter
@Setter
@NoArgsConstructor
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonBackReference
    private Route route;

    @Column(name ="stop_name", nullable = false, length = 100)
    private String stopName;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "distance_from_start", precision = 8, scale = 2, nullable = false)
    private BigDecimal distanceFromStart;


}
