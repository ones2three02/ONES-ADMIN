package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.system.dto.RoleSaveRequest;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRolePermissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
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
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private SystemPermissionMapper permissionMapper;

    @Autowired
    private SystemRolePermissionMapper rolePermissionMapper;

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
                .andExpect(jsonPath("$.data.storageType").value("LOCAL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

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
        assertThat(metadata.getStatus()).isEqualTo("ACTIVE");
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
                .andExpect(jsonPath("$.data.storageType").value("LOCAL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void listFileMetadataByStatusAndBusinessType() throws Exception {
        SystemFileEntity activeFile = newFile("active-contract.pdf", "pdf", "ACTIVE");
        activeFile.setBusinessType("HR_EMPLOYEE_CONTRACT");
        activeFile.setBusinessId("1001");
        fileMapper.insert(activeFile);
        SystemFileEntity deletedFile = newFile("deleted-report.txt", "txt", "DELETED");
        fileMapper.insert(deletedFile);

        mockMvc.perform(get("/api/system/files")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "ACTIVE")
                        .param("businessType", "HR_EMPLOYEE_CONTRACT")
                        .param("businessId", "1001")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(activeFile.getId()))
                .andExpect(jsonPath("$.data.list[0].originalName").value("active-contract.pdf"))
                .andExpect(jsonPath("$.data.list[0].businessType").value("HR_EMPLOYEE_CONTRACT"))
                .andExpect(jsonPath("$.data.list[0].status").value("ACTIVE"));
    }

    @Test
    void rejectListWhenUserHasNoFileReadPermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "file_reader_" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(new UserCreateRequest(
                username,
                "文件列表权限测试用户",
                "operator123",
                1L,
                "用于验证文件列表权限",
                true,
                List.of("OPERATOR")
        ));
        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/system/files")
                        .header("Authorization", "Bearer " + login(username, "operator123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));
    }

    @Test
    void softDeleteUnboundFileAndHideMetadata() throws Exception {
        given(fileStorageService.store(any(MultipartFile.class), any()))
                .willAnswer(invocation -> {
                    String storedName = invocation.getArgument(1);
                    return new FileStorageService.StoredFile(storedName, "/api/system/files/" + storedName);
                });
        String token = login();
        String uploadResponse = mockMvc.perform(multipart("/api/system/files/upload")
                        .file(new MockMultipartFile(
                                "file",
                                "temporary.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "temporary".getBytes()
                        ))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();

        mockMvc.perform(delete("/api/system/files/" + fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.status").value("DELETED"))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty());

        mockMvc.perform(get("/api/system/files/" + fileId + "/metadata")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.FILE_NOT_FOUND.code()));
        SystemFileEntity metadata = fileMapper.selectById(fileId);
        assertThat(metadata.getStatus()).isEqualTo("DELETED");
        assertThat(metadata.getDeletedAt()).isNotNull();
    }

    @Test
    void rejectDownloadWhenFileMetadataDeleted() throws Exception {
        SystemFileEntity file = new SystemFileEntity();
        file.setOriginalName("temporary.txt");
        file.setStoredName("deleted-temporary.txt");
        file.setUrl("/api/system/files/deleted-temporary.txt");
        file.setContentType(MediaType.TEXT_PLAIN_VALUE);
        file.setExtension("txt");
        file.setSizeBytes(9L);
        file.setStorageType("LOCAL");
        file.setStatus("DELETED");
        fileMapper.insert(file);

        mockMvc.perform(get("/api/system/files/deleted-temporary.txt")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isNotFound());

        verify(fileStorageService, never()).load("deleted-temporary.txt");
    }

    @Test
    void rejectDownloadUnboundFileWhenUserHasNoFileReadPermission() throws Exception {
        String storedName = "read-protected-" + System.nanoTime() + ".txt";
        SystemFileEntity file = newFile(storedName, "txt", "ACTIVE");
        fileMapper.insert(file);

        String adminToken = login("admin", "admin123");
        String username = "file_download_" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(new UserCreateRequest(
                username,
                "文件下载权限测试用户",
                "operator123",
                1L,
                "用于验证未绑定文件下载权限",
                true,
                List.of("OPERATOR")
        ));
        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/system/files/" + storedName)
                        .header("Authorization", "Bearer " + login(username, "operator123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        verify(fileStorageService, never()).load(storedName);
    }

    @Test
    void uploaderCanPreviewOwnUnboundFileWithoutGlobalFileReadPermission() throws Exception {
        given(fileStorageService.store(any(MultipartFile.class), any()))
                .willAnswer(invocation -> {
                    String storedName = invocation.getArgument(1);
                    return new FileStorageService.StoredFile(storedName, "/api/system/files/" + storedName);
                });
        String adminToken = login("admin", "admin123");
        String suffix = String.valueOf(System.nanoTime());
        String uploaderToken = createFileUploadUser(adminToken, "file_uploader_" + suffix);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "own-contract.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "preview".getBytes()
        );
        String uploadResponse = mockMvc.perform(multipart("/api/system/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + uploaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();
        String storedName = objectMapper.readTree(uploadResponse).at("/data/url").asText()
                .substring("/api/system/files/".length());

        mockMvc.perform(get("/api/system/files/" + fileId + "/metadata")
                        .header("Authorization", "Bearer " + uploaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.originalName").value("own-contract.pdf"));

        given(fileStorageService.load(storedName))
                .willReturn(java.util.Optional.of(new FileStorageService.StoredResource(
                        new ByteArrayResource("preview".getBytes()),
                        MediaType.APPLICATION_PDF,
                        storedName
                )));
        mockMvc.perform(get("/api/system/files/" + storedName)
                        .header("Authorization", "Bearer " + uploaderToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("preview"));
        verify(fileStorageService).load(storedName);
    }

    @Test
    void rejectMetadataAndDownloadForOthersUnboundFileWithoutGlobalFileReadPermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = createFileUploadUser(adminToken, "file_owner_" + suffix);
        String otherToken = createFileUploadUser(adminToken, "file_other_" + suffix);
        given(fileStorageService.store(any(MultipartFile.class), any()))
                .willAnswer(invocation -> {
                    String storedName = invocation.getArgument(1);
                    return new FileStorageService.StoredFile(storedName, "/api/system/files/" + storedName);
                });
        String uploadResponse = mockMvc.perform(multipart("/api/system/files/upload")
                        .file(new MockMultipartFile(
                                "file",
                                "private-draft.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "private".getBytes()
                        ))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();
        String storedName = objectMapper.readTree(uploadResponse).at("/data/url").asText()
                .substring("/api/system/files/".length());

        mockMvc.perform(get("/api/system/files/" + fileId + "/metadata")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/files/" + storedName)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));
        verify(fileStorageService, never()).load(storedName);
    }

    @Test
    void rejectDeleteOwnUnboundFileWhenUploaderHasNoDeletePermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String uploaderToken = createFileUploadUser(adminToken, "file_delete_" + System.nanoTime());
        SystemFileEntity file = newFile("delete-own-" + System.nanoTime() + ".pdf", "pdf", "ACTIVE");
        file.setUploadedBy(currentUserId(uploaderToken));
        fileMapper.insert(file);

        mockMvc.perform(delete("/api/system/files/" + file.getId())
                        .header("Authorization", "Bearer " + uploaderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));
    }

    @Test
    void downloadUnboundFileWhenUserHasFileReadPermission() throws Exception {
        String storedName = "read-allowed-" + System.nanoTime() + ".txt";
        SystemFileEntity file = newFile(storedName, "txt", "ACTIVE");
        file.setContentType(MediaType.TEXT_PLAIN_VALUE);
        fileMapper.insert(file);
        given(fileStorageService.load(storedName))
                .willReturn(java.util.Optional.of(new FileStorageService.StoredResource(
                        new ByteArrayResource("hello".getBytes()),
                        MediaType.TEXT_PLAIN,
                        storedName
                )));

        mockMvc.perform(get("/api/system/files/" + storedName)
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("hello"));

        verify(fileStorageService).load(storedName);
    }

    @Test
    void rejectDeleteWhenFileIsBoundToBusiness() throws Exception {
        SystemFileEntity file = new SystemFileEntity();
        file.setOriginalName("contract.pdf");
        file.setStoredName("bound-contract.pdf");
        file.setUrl("/api/system/files/bound-contract.pdf");
        file.setContentType(MediaType.APPLICATION_PDF_VALUE);
        file.setExtension("pdf");
        file.setSizeBytes(128L);
        file.setStorageType("LOCAL");
        file.setStatus("ACTIVE");
        file.setBusinessType("HR_EMPLOYEE_CONTRACT");
        file.setBusinessId("1");
        fileMapper.insert(file);

        mockMvc.perform(delete("/api/system/files/" + file.getId())
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.FILE_IN_USE.code()));
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

    private String createFileUploadUser(String adminToken, String username) throws Exception {
        String roleCode = "FILE_UPLOAD_" + System.nanoTime();
        String roleResponse = mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleSaveRequest(
                                roleCode,
                                "文件上传测试角色",
                                "SELF",
                                "仅用于验证未绑定文件上传人访问",
                                1,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long roleId = objectMapper.readTree(roleResponse).at("/data/id").asLong();
        grantPermission(roleId, "system:file:upload");
        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateRequest(
                                username,
                                "文件上传测试用户",
                                "operator123",
                                1L,
                                "仅用于验证文件上传人访问",
                                true,
                                List.of(roleCode)
                        ))))
                .andExpect(status().isOk());
        return login(username, "operator123");
    }

    private long currentUserId(String token) throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void grantPermission(long roleId, String permissionCode) {
        SystemPermissionEntity permission = permissionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemPermissionEntity>()
                        .eq(SystemPermissionEntity::getCode, permissionCode)
                        .last("limit 1")
        );
        if (permission == null) {
            throw new IllegalStateException("权限不存在：" + permissionCode);
        }
        SystemRolePermissionEntity relation = new SystemRolePermissionEntity();
        relation.setRoleId(roleId);
        relation.setPermissionId(permission.getId());
        rolePermissionMapper.insert(relation);
    }

    private SystemFileEntity newFile(String originalName, String extension, String status) {
        SystemFileEntity file = new SystemFileEntity();
        file.setOriginalName(originalName);
        file.setStoredName(originalName);
        file.setUrl("/api/system/files/" + originalName);
        file.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        file.setExtension(extension);
        file.setSizeBytes(128L);
        file.setStorageType("LOCAL");
        file.setStatus(status);
        file.setUploadedBy(1L);
        return file;
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
