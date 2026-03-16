package com.mysociety.backend.repository;

import com.mysociety.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for {@link Profile} entities.
 *
 * <p>
 * Provides CRUD operations against the {@code profiles} table, which stores
 * resident information such as flat number and role. This table is linked to
 * Supabase {@code auth.users} via the shared {@code id} (UUID) primary key.
 * </p>
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
