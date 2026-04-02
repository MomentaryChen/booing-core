package com.bookingcore.modules.platform;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformPageRepository extends JpaRepository<PlatformPage, Long> {

  List<PlatformPage> findByActiveTrueOrderBySortOrderAsc();
}
