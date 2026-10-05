package net.thesphynx.espritmarket.Partnership.Service;

import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService Unit Tests")
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Application application;

    @BeforeEach
    void setUp() {
        application = new Application();
        application.setId(1L);
        application.setStatus("PENDING");
        application.setMatchingScore(85.5);
    }

    @Test
    @DisplayName("Should create application successfully")
    void testCreate() {
        // Arrange
        when(applicationRepository.save(application)).thenReturn(application);

        // Act
        Application result = applicationService.create(application);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(85.5, result.getMatchingScore());
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    @DisplayName("Should get all applications")
    void testGetAll() {
        // Arrange
        List<Application> applications = Arrays.asList(application);
        when(applicationRepository.findAllWithDetails()).thenReturn(applications);

        // Act
        List<Application> result = applicationService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(applicationRepository, times(1)).findAllWithDetails();
    }

    @Test
    @DisplayName("Should get application by ID successfully")
    void testGetByIdSuccess() {
        // Arrange
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        // Act
        Application result = applicationService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
        verify(applicationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when application not found")
    void testGetByIdNotFound() {
        // Arrange
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            applicationService.getById(999L);
        });
        assertEquals("Application not found", exception.getMessage());
        verify(applicationRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should update application successfully")
    void testUpdate() {
        // Arrange
        Application updatedApplication = new Application();
        updatedApplication.setStatus("ACCEPTED");
        updatedApplication.setMatchingScore(90.0);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        // Act
        Application result = applicationService.update(1L, updatedApplication);

        // Assert
        assertNotNull(result);
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(90.0, result.getMatchingScore());
        verify(applicationRepository, times(1)).findById(1L);
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    @DisplayName("Should delete application successfully")
    void testDelete() {
        // Arrange
        doNothing().when(applicationRepository).deleteById(1L);

        // Act
        applicationService.delete(1L);

        // Assert
        verify(applicationRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should return empty list when no applications exist")
    void testGetAllEmpty() {
        // Arrange
        when(applicationRepository.findAllWithDetails()).thenReturn(Arrays.asList());

        // Act
        List<Application> result = applicationService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(applicationRepository, times(1)).findAllWithDetails();
    }

}
