package com.ashraf.commerce.repository;

import com.ashraf.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    // "archived" is NULL on rows created before the column existed, so treat NULL as active.
    @Query("""
            select a from CustomerAddress a
            where a.customer.id = :customerId and (a.archived is null or a.archived = false)
            order by a.isDefault desc, a.id desc
            """)
    List<CustomerAddress> findActiveByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select a from CustomerAddress a
            where a.id = :id and a.customer.id = :customerId and (a.archived is null or a.archived = false)
            """)
    Optional<CustomerAddress> findActiveByIdAndCustomerId(@Param("id") Long id,
                                                          @Param("customerId") Long customerId);
}
