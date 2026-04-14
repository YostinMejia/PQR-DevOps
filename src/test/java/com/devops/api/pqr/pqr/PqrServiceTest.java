package com.devops.api.pqr.pqr;

import com.devops.api.pqr.book.BookOrderNotificationRepository;
import com.devops.api.pqr.book.BookOrderPort;
import com.devops.api.pqr.book.entity.BookOrderNotification;
import com.devops.api.pqr.document.DocumentRepository;
import com.devops.api.pqr.document.entity.Document;
import com.devops.api.pqr.pqr.dto.BookDto;
import com.devops.api.pqr.pqr.dto.CreatePqrDto;
import com.devops.api.pqr.pqr.entity.Pqr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pqr Service Tests")
class PqrServiceTest {

    @Mock
    private PqrRepository pqrRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private BookOrderPort bookOrderPort;

    @Mock
    private BookOrderNotificationRepository notificationRepository;

    private PqrService pqrService;

    private CreatePqrDto validQuejaDto;
    private CreatePqrDto validBookPeticionDto;
    private List<MultipartFile> mockFiles;

    @BeforeEach
    void setUp() {
        pqrService = new PqrService(
                new ObjectMapper(),
                pqrRepository,
                documentRepository,
                bookOrderPort,
                notificationRepository
        );
        ReflectionTestUtils.setField(pqrService, "bookOrderThreshold", 5);

        validQuejaDto = new CreatePqrDto(
                "queja", "customer@test.com", "Service description", "soporte", null
        );

        validBookPeticionDto = new CreatePqrDto(
                "peticion", "customer@test.com", "Quiero comprar este libro",
                "comprar libro", new BookDto("Clean Code", "Robert Martin")
        );

        mockFiles = List.of(
                new MockMultipartFile("files", "doc1.pdf", "application/pdf", "data".getBytes())
        );
    }

    @Test
    @DisplayName("Should save PQR and associated documents")
    void shouldCreatePqrWithDocumentsSuccessfully() {
        // GIVEN
        String pqrId = UUID.randomUUID().toString();
        Pqr savedPqr = buildSavedPqr(pqrId, validQuejaDto, null);

        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);
        given(documentRepository.save(any(Document.class))).willReturn(new Document());

        // WHEN
        Pqr result = pqrService.createPqr(validQuejaDto, mockFiles);

        // THEN
        assertNotNull(result);
        assertEquals(pqrId, result.getId());
        then(documentRepository).should(times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("Should save PQR even if file list is null")
    void shouldCreatePqrWhenFilesAreNull() {
        // GIVEN
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validQuejaDto, null);
        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);

        // WHEN
        Pqr result = pqrService.createPqr(validQuejaDto, null);

        // THEN
        assertNotNull(result);
        then(documentRepository).should(never()).save(any(Document.class));
    }

    @Test
    @DisplayName("Should save PQR even if file list is empty")
    void shouldCreatePqrWhenFilesAreEmpty() {
        // GIVEN
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validQuejaDto, null);
        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);

        // WHEN
        Pqr result = pqrService.createPqr(validQuejaDto, List.of());

        // THEN
        assertNotNull(result);
        then(documentRepository).should(never()).save(any(Document.class));
    }

    @Test
    @DisplayName("Should delete PQR when id exists")
    void shouldDeletePqrWhenIdExists() {
        // GIVEN
        String pqrId = UUID.randomUUID().toString();
        Pqr pqr = Pqr.builder().id(pqrId).build();
        given(pqrRepository.findById(pqrId)).willReturn(Optional.of(pqr));

        // WHEN
        pqrService.delete(pqrId);

        // THEN
        then(pqrRepository).should(times(1)).delete(pqr);
    }

    @Test
    @DisplayName("Should throw 400 when deleting a non-existent id")
    void shouldThrowWhenDeletingNonExistentId() {
        // GIVEN
        String unknownId = UUID.randomUUID().toString();
        given(pqrRepository.findById(unknownId)).willReturn(Optional.empty());

        // WHEN / THEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> pqrService.delete(unknownId)
        );
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Should NOT notify when type is not 'peticion'")
    void shouldNotNotifyWhenTypeIsNotPeticion() {
        // GIVEN
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validQuejaDto, null);
        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);

        // WHEN
        pqrService.createPqr(validQuejaDto, null);

        // THEN
        then(notificationRepository).should(never()).existsByBookTitleAndBookAuthor(any(), any());
        then(bookOrderPort).should(never()).notifyBookOrder(any());
    }

    @Test
    @DisplayName("Should NOT notify when book was already notified")
    void shouldNotNotifyWhenBookAlreadyNotified() {
        // GIVEN
        Map<String, Object> bookMap = Map.of("bookTitle", "Clean Code", "bookAuthor", "Robert Martin");
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validBookPeticionDto, bookMap);

        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);
        given(notificationRepository.existsByBookTitleAndBookAuthor("Clean Code", "Robert Martin"))
                .willReturn(true);

        // WHEN
        pqrService.createPqr(validBookPeticionDto, null);

        // THEN
        then(pqrRepository).should(never()).countByTypeSubjectAndBook(any(), any(), any(), any());
        then(bookOrderPort).should(never()).notifyBookOrder(any());
    }

    @Test
    @DisplayName("Should NOT notify when count is below threshold")
    void shouldNotNotifyWhenCountIsBelowThreshold() {
        // GIVEN
        Map<String, Object> bookMap = Map.of("bookTitle", "Clean Code", "bookAuthor", "Robert Martin");
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validBookPeticionDto, bookMap);

        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);


        // WHEN
        pqrService.createPqr(validBookPeticionDto, null);

        // THEN
        then(bookOrderPort).should(never()).notifyBookOrder(any());
        then(notificationRepository).should(never()).save(any(BookOrderNotification.class));
    }

    @Test
    @DisplayName("Should notify and save notification when threshold is reached")
    void shouldNotifyAndSaveNotificationWhenThresholdReached() {
        // GIVEN
        Map<String, Object> bookMap = Map.of("bookTitle", "Clean Code", "bookAuthor", "Robert Martin");
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validBookPeticionDto, bookMap);

        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);
        given(notificationRepository.existsByBookTitleAndBookAuthor("Clean Code", "Robert Martin"))
                .willReturn(false);
        given(pqrRepository.countByTypeSubjectAndBook("peticion", "comprar libro", "Clean Code", "Robert Martin"))
                .willReturn(5L);
        given(bookOrderPort.notifyBookOrder(savedPqr)).willReturn(true);

        // WHEN
        pqrService.createPqr(validBookPeticionDto, null);

        // THEN
        then(bookOrderPort).should(times(1)).notifyBookOrder(savedPqr);
        then(notificationRepository).should(times(1)).save(any(BookOrderNotification.class));
    }

    @Test
    @DisplayName("Should NOT save notification when notification call fails")
    void shouldNotSaveNotificationWhenNotifyFails() {
        // GIVEN
        Map<String, Object> bookMap = Map.of("bookTitle", "Clean Code", "bookAuthor", "Robert Martin");
        Pqr savedPqr = buildSavedPqr(UUID.randomUUID().toString(), validBookPeticionDto, bookMap);

        given(pqrRepository.save(any(Pqr.class))).willReturn(savedPqr);
        given(notificationRepository.existsByBookTitleAndBookAuthor("Clean Code", "Robert Martin"))
                .willReturn(false);
        given(pqrRepository.countByTypeSubjectAndBook("peticion", "comprar libro", "Clean Code", "Robert Martin"))
                .willReturn(5L);
        given(bookOrderPort.notifyBookOrder(savedPqr)).willReturn(false);

        // WHEN
        pqrService.createPqr(validBookPeticionDto, null);

        // THEN
        then(bookOrderPort).should(times(1)).notifyBookOrder(savedPqr);
        then(notificationRepository).should(never()).save(any(BookOrderNotification.class));
    }

    private Pqr buildSavedPqr(String id, CreatePqrDto dto, Map<String, Object> bookMap) {
        return Pqr.builder()
                .id(id)
                .type(dto.getType())
                .customerEmail(dto.getCustomerEmail())
                .description(dto.getDescription())
                .subject(dto.getSubject())
                .book(bookMap)
                .build();
    }
}