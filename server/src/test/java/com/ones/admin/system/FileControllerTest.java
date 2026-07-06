package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Autowired
    private SystemFileMapper fileMapper;

    @Test
    void uploadAllowedFile() throws Exception {
        given(fileStorageService.store(any(MultipartFile.class), any()))
                .willAnswer(invocation -> {
                    String storedName = invocation.getArgument(1);
                    return new FileStorageService.StoredFile(storedName, "/api/system/files/" + storedName);
                });
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );

        mockMvc.perform(multipart("/api/system/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.url", containsString("/api/system/files/")))
                .andExpect(jsonPath("$.data.originalName").value("report.txt"))
                .andExpect(jsonPath("$.data.sizeBytes").value(5))
                .andExpect(jsonPath("$.data.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(jsonPath("$.data.extension").value("txt"))
                .andExpect(jsonPath("$.data.storageType").value("LOCAL"));

        verify(fileStorageService).store(
                any(MultipartFile.class),
                argThat((String storedName) -> storedName.endsWith(".txt"))
        );
        SystemFileEntity metadata = fileMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getOriginalName, "report.txt")
                .last("limit 1"));
        assertThat(metadata).isNotNull();
        assertThat(metadata.getStoredName()).endsWith(".txt");
        assertThat(metadata.getSizeBytes()).isEqualTo(5L);
        assertThat(metadata.getStorageType()).isEqualTo("LOCAL");
        assertThat(metadata.getUploadedBy()).isNotNull();
    }

    @Test
    void getUploadedFileMetadata() throws Exception {
        given(fileStorageService.store(any(MultipartFile.class), any()))
                .willAnswer(invocation -> {
                    String storedName = invocation.getArgument(1);
                    return new FileStorageService.StoredFile(storedName, "/api/system/files/" + storedName);
                });
        String token = login();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "contract".getBytes()
        );
        String uploadResponse = mockMvc.perform(multipart("/api/system/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/system/files/" + fileId + "/metadata")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.originalName").value("contract.pdf"))
                .andExpect(jsonPath("$.data.extension").value("pdf"))
                .andExpect(jsonPath("$.data.storageType").value("LOCAL"));
    }

    @Test
    void rejectUnsupportedFileExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "binary".getBytes()
        );

        mockMvc.perform(multipart("/api/system/files/upload")
                .file(file)
                .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.FILE_EXTENSION_NOT_ALLOWED.code()));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void rejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/system/files/upload")
                .file(file)
                .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.FILE_EMPTY.code()));
    }

    @Test
    void rejectUploadWhenUserHasNoFilePermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "file_operator_" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(new UserCreateRequest(
                username,
                "文件权限测试用户",
                "operator123",
                1L,
                "用于验证文件上传权限",
                true,
                List.of("OPERATOR")
        ));
        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );
        mockMvc.perform(multipart("/api/system/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + login(username, "operator123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));
    }

    private String login() throws Exception {
        return login("admin", "admin123");
    }

    private String login(String username, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest(username, password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/token/tokenValue").asText();
    }
}
