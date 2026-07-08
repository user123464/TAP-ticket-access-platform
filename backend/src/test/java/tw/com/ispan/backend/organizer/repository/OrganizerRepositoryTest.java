package tw.com.ispan.backend.organizer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;

@SpringBootTest
public class OrganizerRepositoryTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private OrganizerRepository organizerRepository;

        @Autowired
        private OrganizerMemberRepository organizerMemberRepository;

        @Autowired
        private EntityManager entityManager; // 引入 EntityManager 來控制快取同步

        @Test
        @Transactional // 測試結束後會自動 rollback 資料庫
        public void testOrganizerAndMemberCRUD() {
                // ── 1. 建立擁有者帳號與被邀請成員帳號 ──
                User owner = User.builder()
                                .userId("USR8888801")
                                .email("owner@test.com")
                                .name("負責人王小明")
                                .authProvider(AuthProvider.LOCAL)
                                .passwordHash("hashed_pwd")
                                .isActive(true)
                                .isDeleted(false)
                                .isTwoFactorEnabled(false)
                                .build();
                userRepository.save(owner);

                User memberUser = User.builder()
                                .userId("USR8888802")
                                .email("member@test.com")
                                .name("受邀成員李小美")
                                .authProvider(AuthProvider.LOCAL)
                                .passwordHash("hashed_pwd")
                                .isActive(true)
                                .isDeleted(false)
                                .isTwoFactorEnabled(false)
                                .build();
                userRepository.save(memberUser);

                // ── 2. 建立廠商組織 (C) ──
                Organizer organizer = Organizer.builder()
                                .organizerId("ORG8888801")
                                .owner(owner) // 關係注入
                                .name("極光創意有限公司")
                                .taxId("24681357")
                                .status(OrganizerStatus.ACTIVE)
                                .kycStatus(KycStatus.DRAFT)
                                .build();
                organizerRepository.save(organizer);

                // ── 3. 建立邀請成員關係 (C) ──
                OrganizerMember memberRelation = OrganizerMember.builder()
                                .memberId("MBR8888801")
                                .organizer(organizer)
                                .user(memberUser)
                                .invitedBy(owner)
                                .status(OrganizerMemberStatus.PENDING)
                                .build();
                organizerMemberRepository.save(memberRelation);

                // 【關鍵步驟】強制將上面的 Insert 寫入資料庫，並清空 L1 Cache (EntityManager 快取)
                // 這樣後續的查詢才會真正從資料庫取得帶有資料庫自動生成的 row_version 的新鮮實體！
                entityManager.flush();
                entityManager.clear();

                // ── 4. 驗證魔術查詢方法 (R) ──
                // (A) 查詢負責人的所有組織
                // 註：因為 clear() 了快取，物件變成了游離態 (Detached)，我們從資料庫取得受管的 (Managed) owner
                User managedOwner = userRepository.findById("USR8888801").get();
                List<Organizer> ownerOrgs = organizerRepository.findByOwner(managedOwner);
                assertFalse(ownerOrgs.isEmpty());
                assertEquals("極光創意有限公司", ownerOrgs.get(0).getName());

                Organizer orgInDb = ownerOrgs.get(0);
                assertNotNull(orgInDb.getCreatedAt()); // 驗證自動審計時間

                // (B) 查詢組織的成員
                List<OrganizerMember> orgMembers = organizerMemberRepository.findByOrganizer(orgInDb);
                assertEquals(1, orgMembers.size());
                assertEquals("受邀成員李小美", orgMembers.get(0).getUser().getName());

                // (C) 驗證兩人關係
                User managedMember = userRepository.findById("USR8888802").get();
                Optional<OrganizerMember> relationOpt = organizerMemberRepository.findByOrganizerAndUser(orgInDb,
                                managedMember);
                assertTrue(relationOpt.isPresent());
                assertEquals(OrganizerMemberStatus.PENDING, relationOpt.get().getStatus());

                // ── 5. 模擬業務邏輯更新 (U) ──
                // (A) 負責人審核通過此廠商 (此時 orgInDb 來自上面的查詢，已含有資料庫產生的 row_version)
                orgInDb.setKycStatus(KycStatus.APPROVED);
                orgInDb.setKycReviewedBy(managedOwner);
                orgInDb.setKycReviewedAt(LocalDateTime.now());
                Organizer updatedOrganizer = organizerRepository.saveAndFlush(orgInDb);
                assertEquals(KycStatus.APPROVED, updatedOrganizer.getKycStatus());

                // (B) 成員同意加入
                OrganizerMember relationToUpdate = relationOpt.get();
                relationToUpdate.setStatus(OrganizerMemberStatus.ACCEPTED);
                relationToUpdate.setJoinedAt(LocalDateTime.now());
                OrganizerMember updatedRelation = organizerMemberRepository.saveAndFlush(relationToUpdate);
                assertEquals(OrganizerMemberStatus.ACCEPTED, updatedRelation.getStatus());
                assertNotNull(updatedRelation.getJoinedAt());

                // ── 6. 刪除資料測試 (D) ──
                // (A) 刪除成員關係
                organizerMemberRepository.delete(updatedRelation);
                organizerMemberRepository.flush(); // 強制同步刪除
                assertFalse(organizerMemberRepository.findById("MBR8888801").isPresent());

                // (B) 刪除組織
                // 注意：因為上一步更新了組織狀態，資料庫的 row_version 已更新。
                // 我們必須清除快取，重新查詢最新的實體來進行刪除，否則會觸發樂觀鎖版本不對的異常。
                entityManager.clear();
                Organizer orgToDelete = organizerRepository.findById(updatedOrganizer.getOrganizerId()).get();
                organizerRepository.delete(orgToDelete);
                organizerRepository.flush();
                assertFalse(organizerRepository.findById("ORG8888801").isPresent()); // 驗證組織是否被成功刪除
        }
}
