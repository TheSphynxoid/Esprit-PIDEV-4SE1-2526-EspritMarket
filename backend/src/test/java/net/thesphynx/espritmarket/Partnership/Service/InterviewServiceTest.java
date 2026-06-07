package net.thesphynx.espritmarket.Partnership.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Partnership.Dto.InterviewUpdateRequest;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.Interview;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewResult;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus;
import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Repository.ApplicationRepository;
import net.thesphynx.espritmarket.Partnership.Repository.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService Unit Tests")
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private InterviewService interviewService;

    private Application application;
    private Interview interview;

    @BeforeEach
    void setUp() {
        User applicant = new User();
        applicant.setId(10L);
        applicant.setName("Jane Doe");

        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(20L);
        jobOffer.setTitle("Backend Engineer");

        application = new Application();
        application.setId(1L);
        application.setApplicant(applicant);
        application.setJobOffer(jobOffer);

        interview = new Interview();
        interview.setId(1L);
        interview.setInterviewDate(LocalDateTime.now().plusDays(7));
        interview.setType("VIDEO");
        interview.setApplication(application);
    }

    @Test
    @DisplayName("Should create interview successfully")
    void testCreate() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interview result = interviewService.create(interview);

        assertNotNull(result);
        assertEquals(LocalDateTime.now().plusDays(7).toLocalDate(), result.getInterviewDate().toLocalDate());
        assertEquals("VIDEO", result.getType());
        assertEquals(application, result.getApplication());
        verify(applicationRepository, times(1)).findById(1L);
        verify(interviewRepository, times(1)).save(any(Interview.class));
    }

    @Test
    @DisplayName("Should get all interviews")
    void testGetAll() {
        when(interviewRepository.findAllWithApplicationDetails()).thenReturn(List.of(interview));

        List<Interview> result = interviewService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VIDEO", result.get(0).getType());
        verify(interviewRepository, times(1)).findAllWithApplicationDetails();
    }

    @Test
    @DisplayName("Should get interview by ID successfully")
    void testGetByIdSuccess() {
        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(interview));

        Interview result = interviewService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("VIDEO", result.getType());
        verify(interviewRepository, times(1)).findByIdWithDetails(1L);
    }

    @Test
    @DisplayName("Should throw exception when interview not found")
    void testGetByIdNotFound() {
        when(interviewRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> interviewService.getById(999L));
        assertEquals("Interview not found", exception.getMessage());
        verify(interviewRepository, times(1)).findByIdWithDetails(999L);
    }

    @Test
    @DisplayName("Should update interview successfully")
    void testUpdate() {
        InterviewUpdateRequest request = new InterviewUpdateRequest();
        request.setInterviewDate(LocalDateTime.now().plusDays(10));
        request.setType("IN_PERSON");
        request.setStatus("COMPLETED");
        request.setResult("ACCEPTED");

        when(interviewRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interview result = interviewService.update(1L, request);

        assertNotNull(result);
        assertEquals(LocalDateTime.now().plusDays(10).toLocalDate(), result.getInterviewDate().toLocalDate());
        assertEquals("IN_PERSON", result.getType());
        assertEquals(InterviewResult.ACCEPTED, result.getResult());
        assertEquals(InterviewStatus.COMPLETED, result.getStatus());
        verify(interviewRepository, times(1)).save(any(Interview.class));
    }

    @Test
    @DisplayName("Should delete interview successfully")
    void testDelete() {
        doNothing().when(interviewRepository).deleteById(1L);

        interviewService.delete(1L);

        verify(interviewRepository, times(1)).deleteById(1L);
    }
}
