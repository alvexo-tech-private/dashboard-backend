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

    /**
     * Workshops (not deleted) whose booking advance is switched on — the same switch the app uses to ask riders for
     * an advance (mechanic_settings.advance_enabled, "Booking Advance" in workshop Settings). It used to count
     * repairs_require_advance, a separate repairs-only flag, which showed 0 for workshops that do take an advance.
     */
    @Query(value = "SELECT COUNT(*) FROM mechanic_settings ms JOIN users u ON u.id = ms.mechanic_id "
            + "WHERE ms.advance_enabled = true AND u.deleted = false", nativeQuery = true)
    long countAdvancePaymentEnabledForActiveMechanics();

    Optional<MechanicConfigurationSettings> findByMechanicId(Long mechanicId);

    List<MechanicConfigurationSettings> findByMechanicIdIn(Collection<Long> mechanicIds);
}
