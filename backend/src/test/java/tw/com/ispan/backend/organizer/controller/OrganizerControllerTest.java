package tw.com.ispan.backend.organizer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import tw.com.ispan.backend.login.dto.LoginRequest;
import tw.com.ispan.backend.login.dto.RegisterRequest;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.service.RedisService;
import tw.com.ispan.backend.organizer.dto.CreateOrganizerRequest;
import tw.com.ispan.backend.organizer.dto.SubmitKycRequest;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrganizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    private String ownerEmail = "org.owner@ispan.com";
    private String otherEmail = "org.other@ispan.com";
    private String testPassword = "securePassword123";

    private String ownerToken;
    private String otherToken;

    private void mockOtpVerified(String email) {
        redisService.setWithTtl("otp:verified:" + email, "true", 600);
    }

    private String registerAndLogin(String email, String name) throws Exception {
        mockOtpVerified(email);
        RegisterRequest regReq = RegisterRequest.builder()
                .email(email)
                .password(testPassword)
                .name(name)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password(testPassword)
                .build();

        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseBody).get("accessToken").asText();
    }

    @BeforeEach
    public void setup() throws Exception {
        // 清除先前可能的殘留測試資料
        organizerRepository.findAll().forEach(org -> {
            if (org.getOwner().getEmail().equals(ownerEmail) || org.getOwner().getEmail().equals(otherEmail)) {
                organizerRepository.delete(org);
            }
        });
        userRepository.findByEmail(ownerEmail).ifPresent(u -> userRepository.delete(u));
        userRepository.findByEmail(otherEmail).ifPresent(u -> userRepository.delete(u));
        redisService.delete("otp:verified:" + ownerEmail);
        redisService.delete("otp:verified:" + otherEmail);

        // 註冊與登入 Owner 及 Other 使用者
        ownerToken = registerAndLogin(ownerEmail, "組織負責人");
        otherToken = registerAndLogin(otherEmail, "一般旁觀者");
    }

    @Test
    public void testKycUploadAndSubmitFlowSuccess() throws Exception {
        // 1. Owner 建立新組織 (起步為 DRAFT)
        CreateOrganizerRequest createReq = new CreateOrganizerRequest();
        createReq.setName("傑森票務娛樂公司");
        createReq.setTaxId("24482900");

        String orgResponse = mockMvc.perform(post("/api/organizer")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.kycStatus").value(KycStatus.DRAFT.ordinal()))
                .andReturn().getResponse().getContentAsString();

        String organizerId = objectMapper.readTree(orgResponse).get("id").asText();

        // 2. Owner 上傳公司設立登記表 (PDF)
        MockMultipartFile docFile = new MockMultipartFile(
                "file", 
                "company_register.pdf", 
                "application/pdf", 
                "Dummy PDF content".getBytes()
        );

        String uploadDocResponse = mockMvc.perform(multipart("/api/organizer/" + organizerId + "/kyc/upload")
                .file(docFile)
                .param("fileType", "DOC")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filePath").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String docPath = objectMapper.readTree(uploadDocResponse).get("filePath").asText();

        // 3. Owner 上傳負責人身份證影本 (PNG)
        MockMultipartFile idFile = new MockMultipartFile(
                "file", 
                "owner_idcard.png", 
                "image/png", 
                "Dummy PNG content".getBytes()
        );

        String uploadIdResponse = mockMvc.perform(multipart("/api/organizer/" + organizerId + "/kyc/upload")
                .file(idFile)
                .param("fileType", "ID")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filePath").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String idPath = objectMapper.readTree(uploadIdResponse).get("filePath").asText();

        // 4. Owner 上傳組織大頭貼 (JPG)
        MockMultipartFile logoFile = new MockMultipartFile(
                "file", 
                "org_logo.jpg", 
                "image/jpeg", 
                "Dummy JPG logo content".getBytes()
        );

        String uploadLogoResponse = mockMvc.perform(multipart("/api/organizer/" + organizerId + "/kyc/upload")
                .file(logoFile)
                .param("fileType", "LOGO")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filePath").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String logoPath = objectMapper.readTree(uploadLogoResponse).get("filePath").asText();

        // 5. 非組織成員嘗試上傳 (OtherToken) -> 應該被 403 Forbidden
        mockMvc.perform(multipart("/api/organizer/" + organizerId + "/kyc/upload")
                .file(idFile)
                .param("fileType", "ID")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // 6. Owner 提交 KYC 審核
        SubmitKycRequest submitReq = new SubmitKycRequest();
        submitReq.setTaxId("24482900");
        submitReq.setBankCode("007");
        submitReq.setBankName("第一商業銀行");
        submitReq.setAccountNo("123456789012");
        submitReq.setAccountName("傑森票務娛樂公司");
        submitReq.setPhone("02-27208889");
        submitReq.setFax("02-27208880");
        submitReq.setAddress("台北市信義區松高路11號");
        submitReq.setOwnerName("陳文傑");
        submitReq.setOwnerIdNumber("A123456789");
        submitReq.setLogoUrl(logoPath);
        submitReq.setRegistrationDocName("company_register.pdf");
        submitReq.setRegistrationDocUrl(docPath);
        submitReq.setIdentityCardName("owner_idcard.png");
        submitReq.setIdentityCardUrl(idPath);

        mockMvc.perform(post("/api/organizer/" + organizerId + "/kyc/submit")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 7. 驗證資料庫狀態已轉為 PENDING
        Organizer updatedOrg = organizerRepository.findById(organizerId).orElseThrow();
        assertEquals(KycStatus.PENDING, updatedOrg.getKycStatus());
        assertEquals("24482900", updatedOrg.getTaxId());

        // 8. 非組織成員嘗試再次提交 KYC 審核 -> 403 Forbidden
        mockMvc.perform(post("/api/organizer/" + organizerId + "/kyc/submit")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isForbidden());
    }
}
