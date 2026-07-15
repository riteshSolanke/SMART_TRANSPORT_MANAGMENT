package com.transport.authservice.repository;


import com.transport.authservice.entity.OtpVerification;
import com.transport.authservice.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification>  findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String identifier, OtpPurpose purpose);

    void deleteByIdentifierAndPurpose(String identifier, OtpPurpose purpose);

}
