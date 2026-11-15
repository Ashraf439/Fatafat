package com.ashraf.rider.service;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class RiderLocationService {
    private static final String GEO_KEY = "riders:online";
    private final StringRedisTemplate redisTemplate;

    public RiderLocationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateLocation(Long riderId, double latitude, double longitude) {
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(longitude, latitude), riderId.toString());
    }

    public void markUnavailable(Long riderId) {
        redisTemplate.opsForGeo().remove(GEO_KEY, riderId.toString());
    }

    public List<Long> findNearbyRiderIds(BigDecimal lat, BigDecimal lng, double radiusKm, int limit) {
        GeoReference<String> reference = GeoReference.fromCoordinate(lng.doubleValue(), lat.doubleValue());
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .sortAscending()
                .limit(limit);

        GeoResults<GeoLocation<String>> results =
                redisTemplate.opsForGeo().search(GEO_KEY, reference, radius, args);

        if (results == null) {
            return Collections.emptyList();
        }

        return results.getContent().stream()
                .map(r -> Long.valueOf(r.getContent().getName()))
                .toList();
    }
}
