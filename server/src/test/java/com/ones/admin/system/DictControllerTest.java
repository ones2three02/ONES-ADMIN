package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.system.dto.DictItemSaveRequest;
import com.ones.admin.system.dto.DictTypeSaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void manageDictionaryTypesAndItems() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/system/dicts/hr_employment_status/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("ACTIVE")))
                .andExpect(jsonPath("$.data[*].label", hasItem("在职(正式)")));

        mockMvc.perform(get("/api/system/dicts/hr_contract_type/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("FIXED_TERM")))
                .andExpect(jsonPath("$.data[*].value", hasItem("OPEN_ENDED")))
                .andExpect(jsonPath("$.data[*].value", hasItem("INTERNSHIP")))
                .andExpect(jsonPath("$.data[*].value", hasItem("SERVICE")))
                .andExpect(jsonPath("$.data[*].value").value(org.hamcrest.Matchers.not(hasItem("FIXED"))))
                .andExpect(jsonPath("$.data[*].value").value(org.hamcrest.Matchers.not(hasItem("UNFIXED"))));

        mockMvc.perform(get("/api/system/dicts/hr_gender/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("MALE")))
                .andExpect(jsonPath("$.data[*].value", hasItem("FEMALE")));

        mockMvc.perform(get("/api/system/dicts/hr_contract_status/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("DRAFT")))
                .andExpect(jsonPath("$.data[*].value", hasItem("ACTIVE")))
                .andExpect(jsonPath("$.data[*].value", hasItem("EXPIRING")))
                .andExpect(jsonPath("$.data[*].value", hasItem("TERMINATED")))
                .andExpect(jsonPath("$.data[*].value").value(org.hamcrest.Matchers.not(hasItem("EXPIRED"))));

        mockMvc.perform(get("/api/system/dicts/hr_roster_import_status/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("PARSING")))
                .andExpect(jsonPath("$.data[*].value", hasItem("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.data[*].value", hasItem("PARTIAL_SUCCESS")))
                .andExpect(jsonPath("$.data[*].value", hasItem("SUCCESS")))
                .andExpect(jsonPath("$.data[*].value", hasItem("FAILED")))
                .andExpect(jsonPath("$.data[*].value").value(org.hamcrest.Matchers.not(hasItem("PENDING"))));

        mockMvc.perform(get("/api/system/dicts/hr_employee_document_type/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].value", hasItem("IDENTITY")))
                .andExpect(jsonPath("$.data[*].value", hasItem("EDUCATION")))
                .andExpect(jsonPath("$.data[*].value", hasItem("CERTIFICATE")))
                .andExpect(jsonPath("$.data[*].value", hasItem("MEDICAL")))
                .andExpect(jsonPath("$.data[*].value", hasItem("OTHER")));

        String createTypeBody = objectMapper.writeValueAsString(new DictTypeSaveRequest(
                "qa_status",
                "质检状态",
                "测试用字典",
                true,
                90
        ));
        String typeResponse = mockMvc.perform(post("/api/system/dicts/types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTypeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.dictCode").value("qa_status"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long typeId = objectMapper.readTree(typeResponse).at("/data/id").asLong();

        String createItemBody = objectMapper.writeValueAsString(new DictItemSaveRequest(
                typeId,
                null,
                "待质检",
                "PENDING",
                "warning",
                "等待质检处理",
                true,
                10
        ));
        String itemResponse = mockMvc.perform(post("/api/system/dicts/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createItemBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.dictCode").value("qa_status"))
                .andExpect(jsonPath("$.data.itemValue").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long itemId = objectMapper.readTree(itemResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/system/dicts/items")
                        .param("dictCode", "qa_status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].itemLabel").value("待质检"));

        mockMvc.perform(delete("/api/system/dicts/types/" + typeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.DICT_TYPE_HAS_ITEMS.code()));

        String updateItemBody = objectMapper.writeValueAsString(new DictItemSaveRequest(
                typeId,
                null,
                "质检通过",
                "PASSED",
                "success",
                "已通过质检",
                true,
                20
        ));
        mockMvc.perform(put("/api/system/dicts/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateItemBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemValue").value("PASSED"))
                .andExpect(jsonPath("$.data.color").value("success"));

        mockMvc.perform(delete("/api/system/dicts/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Thread.sleep(2100);

        mockMvc.perform(delete("/api/system/dicts/types/" + typeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login() throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"));
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
