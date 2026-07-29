package com.transport.routeservice.repository;

import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Stop;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RouteRepositoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RouteRepository routeRepository;

    @Test
    void queryFindsIntermediateStopsOnlyInTravelDirection() {
        Route route = new Route();
        route.setRouteName("Integration Line");
        route.setStartPoint("Alpha");
        route.setEndPoint("Delta");
        route.setActive(true);
        entityManager.persist(route);

        persistStop(route, "Alpha", 1, "0");
        persistStop(route, "Beta", 2, "5");
        persistStop(route, "Gamma", 3, "12");
        persistStop(route, "Delta", 4, "20");
        entityManager.flush();
        entityManager.clear();

        List<Route> forward =
                routeRepository.findRoutesServingStopsInOrder("Beta", "Gamma");
        List<Route> reverse =
                routeRepository.findRoutesServingStopsInOrder("Gamma", "Beta");

        assertThat(forward)
                .extracting(Route::getRouteName)
                .containsExactly("Integration Line");
        assertThat(reverse).isEmpty();
    }

    private void persistStop(
            Route route, String name, int sequence, String distance) {
        Stop stop = new Stop();
        stop.setRoute(route);
        stop.setStopName(name);
        stop.setSequenceOrder(sequence);
        stop.setDistanceFromStart(new BigDecimal(distance));
        entityManager.persist(stop);
    }
}
