package com.ashraf.rider.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RoutingService {

    private final RestClient restClient;

    public RoutingService(@Value("${osrm.base-url:https://router.project-osrm.org}") String osrmBaseUrl) {
        this.restClient = RestClient.create(osrmBaseUrl);
    }

    public record RouteResult(double distanceMeters, double durationSeconds, String polyline) {}
    private record OsrmResponse(List<OsrmRoute> routes) {}
    private record OsrmRoute(double distance, double duration, String geometry) {}

    public RouteResult route(BigDecimal fromLat, BigDecimal fromLng, BigDecimal toLat, BigDecimal toLng) {
        String path = String.format("/route/v1/driving/%s,%s;%s,%s?overview=full&geometries=polyline",
                fromLng, fromLat, toLng, toLat);
        OsrmResponse response = restClient.get().uri(path).retrieve().body(OsrmResponse.class);
        if (response == null || response.routes().isEmpty()) {
            throw new IllegalStateException("No route found");
        }
        OsrmRoute route = response.routes().get(0);
        return new RouteResult(route.distance(), route.duration(), route.geometry());
    }
}