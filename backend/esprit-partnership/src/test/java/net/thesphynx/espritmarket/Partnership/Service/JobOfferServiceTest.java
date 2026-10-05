package net.thesphynx.espritmarket.Partnership.Service;

import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Repository.JobOfferRepository;
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
@DisplayName("JobOfferService Unit Tests")
class JobOfferServiceTest {

    @Mock
    private JobOfferRepository jobOfferRepository;

    @InjectMocks
    private JobOfferService jobOfferService;

    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        jobOffer = new JobOffer();
        jobOffer.setId(1L);
        jobOffer.setTitle("Senior Angular Developer");
        jobOffer.setDescription("We are looking for an experienced Angular developer");
        jobOffer.setType("INTERNSHIP");
        jobOffer.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should create job offer successfully")
    void testCreate() {
        // Arrange
        when(jobOfferRepository.save(jobOffer)).thenReturn(jobOffer);

        // Act
        JobOffer result = jobOfferService.create(jobOffer);

        // Assert
        assertNotNull(result);
        assertEquals("Senior Angular Developer", result.getTitle());
        assertEquals("INTERNSHIP", result.getType());
        verify(jobOfferRepository, times(1)).save(jobOffer);
    }

    @Test
    @DisplayName("Should get all job offers")
    void testGetAll() {
        // Arrange
        List<JobOffer> jobOffers = Arrays.asList(jobOffer);
        when(jobOfferRepository.findAll()).thenReturn(jobOffers);

        // Act
        List<JobOffer> result = jobOfferService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Senior Angular Developer", result.get(0).getTitle());
        verify(jobOfferRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get job offer by ID successfully")
    void testGetByIdSuccess() {
        // Arrange
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));

        // Act
        JobOffer result = jobOfferService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Senior Angular Developer", result.getTitle());
        verify(jobOfferRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when job offer not found")
    void testGetByIdNotFound() {
        // Arrange
        when(jobOfferRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jobOfferService.getById(999L);
        });
        assertEquals("JobOffer not found", exception.getMessage());
        verify(jobOfferRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should update job offer successfully")
    void testUpdate() {
        // Arrange
        JobOffer updatedJobOffer = new JobOffer();
        updatedJobOffer.setTitle("Lead Developer");
        updatedJobOffer.setDescription("Updated description");
        updatedJobOffer.setType("CDI");
        updatedJobOffer.setStatus("INACTIVE");

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(jobOffer));
        when(jobOfferRepository.save(jobOffer)).thenReturn(jobOffer);

        // Act
        JobOffer result = jobOfferService.update(1L, updatedJobOffer);

        // Assert
        assertNotNull(result);
        assertEquals("Lead Developer", result.getTitle());
        assertEquals("CDI", result.getType());
        assertEquals("INACTIVE", result.getStatus());
        verify(jobOfferRepository, times(1)).findById(1L);
        verify(jobOfferRepository, times(1)).save(jobOffer);
    }

    @Test
    @DisplayName("Should delete job offer successfully")
    void testDelete() {
        // Arrange
        doNothing().when(jobOfferRepository).deleteById(1L);

        // Act
        jobOfferService.delete(1L);

        // Assert
        verify(jobOfferRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should return empty list when no job offers exist")
    void testGetAllEmpty() {
        // Arrange
        when(jobOfferRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<JobOffer> result = jobOfferService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(jobOfferRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle null description field")
    void testCreateWithNullDescription() {
        // Arrange
        jobOffer.setDescription(null);
        when(jobOfferRepository.save(jobOffer)).thenReturn(jobOffer);

        // Act
        JobOffer result = jobOfferService.create(jobOffer);

        // Assert
        assertNotNull(result);
        assertNull(result.getDescription());
        verify(jobOfferRepository, times(1)).save(jobOffer);
    }

    @Test
    @DisplayName("Should update only specific fields without affecting others")
    void testUpdatePartialFields() {
        // Arrange
        JobOffer existingOffer = new JobOffer();
        existingOffer.setId(1L);
        existingOffer.setTitle("Original Title");
        existingOffer.setDescription("Original Description");
        existingOffer.setType("INTERNSHIP");
        existingOffer.setStatus("ACTIVE");

        JobOffer updateData = new JobOffer();
        updateData.setTitle("New Title");
        updateData.setDescription("New Description");
        updateData.setType("CDI");
        updateData.setStatus("INACTIVE");

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(existingOffer));
        when(jobOfferRepository.save(existingOffer)).thenReturn(existingOffer);

        // Act
        JobOffer result = jobOfferService.update(1L, updateData);

        // Assert
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals("CDI", result.getType());
        assertEquals("INACTIVE", result.getStatus());
    }
}
