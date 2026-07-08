package tw.com.ispan.backend.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.ticket.entity.TicketType;

public interface TicketTypeRepository extends JpaRepository<TicketType, Integer> {

    // // 【前台購票用】撈出某個活動主題下，所有「未刪除」且「開放購買 (isEnabled=true)」的營運票種
    // // 提供給前端 bookticket.vue 右側的即時餘票監控面板使用
    // List<TicketType> findByTheme_ThemeIdAndIsDeletedFalseAndIsEnabledTrue(Integer
    // themeId);
}
