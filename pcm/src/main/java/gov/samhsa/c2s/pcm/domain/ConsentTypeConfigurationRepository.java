package gov.samhsa.c2s.pcm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConsentTypeConfigurationRepository extends JpaRepository<ConsentTypeConfiguration, Long> {
    Optional<ConsentTypeConfiguration> findOneById(long id);
}
