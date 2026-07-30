package com.ashraf.commerce.repository;

import com.ashraf.commerce.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
}
