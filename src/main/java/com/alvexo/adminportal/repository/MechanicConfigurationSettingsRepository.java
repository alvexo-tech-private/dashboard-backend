package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.MechanicConfigurationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MechanicConfigurationSettingsRepository extends JpaRepository<MechanicConfigurationSettings, Long> {

    @Query("SELECT COUNT(s) FROM MechanicConfigurationSettings s " +
            "WHERE s.pickupDropFacilityEnabled = true AND s.mechanic.deleted = false")
    long countPickupDropEnabledForActiveMechanics();

    @Query("SELECT COUNT(s) FROM MechanicConfigurationSettings s " +
            "WHERE s.repairsRequireAdvance = true AND s.mechanic.deleted = false")
    long countAdvancePaymentEnabledForActiveMechanics();

    Optional<MechanicConfigurationSettings> findByMechanicId(Long mechanicId);

    List<MechanicConfigurationSettings> findByMechanicIdIn(Collection<Long> mechanicIds);
}
