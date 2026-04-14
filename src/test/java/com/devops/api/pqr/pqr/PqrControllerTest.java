package com.devops.api.pqr.pqr;

import com.devops.api.pqr.document.mapper.DocumentMapper;
import com.devops.api.pqr.pqr.dto.BookDto;
import com.devops.api.pqr.pqr.dto.CreatePqrDto;
import com.devops.api.pqr.pqr.entity.Pqr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PqrController.class)
@DisplayName("Pqr Controller Tests")
public class PqrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PqrService pqrService;

    @MockitoBean
    private DocumentMapper documentMapper;

    private static final String BASE_URL = "/api/v2/pqr";

    @Test
    @DisplayName("POST /pqr - Should create PQR with files successfully")
    void testCreatePqrShouldReturnCreated() throws Exception {
        // GIVEN
        CreatePqrDto dto = buildValidDto();

        MockMultipartFile pqrPart = buildPqrPart(dto);
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "evidence.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "dummy content".getBytes()
        );

        Pqr savedPqr = Pqr.builder()
                .id(UUID.randomUUID().toString())
                .type(dto.getType())
                .customerEmail(dto.getCustomerEmail())
                .description(dto.getDescription())
                .subject(dto.getSubject())
                .build();

        given(pqrService.createPqr(any(CreatePqrDto.class), anyList()))
                .willReturn(savedPqr);

        // WHEN / THEN
        mockMvc.perform(multipart(BASE_URL)
                        .file(pqrPart)
                        .file(filePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("queja"))
                .andExpect(jsonPath("$.subject").value("comprar_libro"));
    }

    @Test
    @DisplayName("POST /pqr - Should create PQR without files successfully")
    void testCreatePqrWithoutFilesShouldReturnCreated() throws Exception {
        // GIVEN
        CreatePqrDto dto = buildValidDto();

        MockMultipartFile pqrPart = buildPqrPart(dto);

        Pqr savedPqr = Pqr.builder()
                .id(UUID.randomUUID().toString())
                .type(dto.getType())
                .customerEmail(dto.getCustomerEmail())
                .description(dto.getDescription())
                .subject(dto.getSubject())
                .build();

        given(pqrService.createPqr(any(CreatePqrDto.class), any()))
                .willReturn(savedPqr);

        // WHEN / THEN
        mockMvc.perform(multipart(BASE_URL)
                        .file(pqrPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /pqr - Should return 400 when required fields are blank")
    void testCreatePqrShouldReturnBadRequestWhenInvalidDto() throws Exception {
        // GIVEN
        CreatePqrDto invalidDto = new CreatePqrDto("", "not-an-email", "", "", null);

        MockMultipartFile pqrPart = buildPqrPart(invalidDto);

        // WHEN / THEN
        mockMvc.perform(multipart(BASE_URL)
                        .file(pqrPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /pqr - Should return 400 when type does not match allowed values")
    void testCreatePqrShouldReturnBadRequestWhenTypeIsInvalid() throws Exception {
        // GIVEN
        CreatePqrDto invalidDto = new CreatePqrDto(
                "tipo_invalido", "test@gmail.com", "Description", "comprar_libro",
                new BookDto("Clean Code", "Robert Martin")
        );

        MockMultipartFile pqrPart = buildPqrPart(invalidDto);

        // WHEN / THEN
        mockMvc.perform(multipart(BASE_URL)
                        .file(pqrPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    private CreatePqrDto buildValidDto() {
        return new CreatePqrDto(
                "queja",
                "test@gmail.com",
                "Description test",
                "comprar_libro",
                new BookDto("Clean Code", "Robert Martin")
        );
    }

    private MockMultipartFile buildPqrPart(CreatePqrDto dto) throws Exception {
        return new MockMultipartFile(
                "pqr",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto)
        );
    }
}