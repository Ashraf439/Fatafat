package com.ashraf.commerce.repository;

import com.ashraf.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    Optional<CustomerAddress> findByPublicId(UUID publicId);

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
    @Query("""
        select a from CustomerAddress a
        where a.publicId = :publicId and a.customer.id = :customerId and (a.archived is null or a.archived = false)
        """)
    Optional<CustomerAddress> findActiveByPublicIdAndCustomerId(@Param("publicId") UUID publicId,
                                                                @Param("customerId") Long customerId);
}
