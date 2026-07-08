package tw.com.ispan.backend.system.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.MediaFile;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    List<MediaFile> findByRelatedTableAndRelatedId(String relatedTable, String relatedId);
}
