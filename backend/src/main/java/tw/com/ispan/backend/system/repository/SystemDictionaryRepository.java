package tw.com.ispan.backend.system.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.SystemDictionary;

public interface SystemDictionaryRepository extends JpaRepository<SystemDictionary, Long> {
    List<SystemDictionary> findByDictTypeOrderBySortOrderAsc(String dictType);
    Optional<SystemDictionary> findByDictTypeAndDictCode(String dictType, String dictCode);
}
