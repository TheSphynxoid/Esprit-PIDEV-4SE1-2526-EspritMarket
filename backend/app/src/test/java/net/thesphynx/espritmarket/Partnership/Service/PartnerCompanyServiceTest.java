package net.thesphynx.espritmarket.Partnership.Service;

import net.thesphynx.espritmarket.Partnership.Entity.PartnerCompany;
import net.thesphynx.espritmarket.Partnership.Repository.PartnerCompanyRepository;
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
@DisplayName("PartnerCompanyService Unit Tests")
class PartnerCompanyServiceTest {

    @Mock
    private PartnerCompanyRepository partnerCompanyRepository;

    @InjectMocks
    private PartnerCompanyService partnerCompanyService;

    private PartnerCompany partnerCompany;

    @BeforeEach
    void setUp() {
        partnerCompany = new PartnerCompany();
        partnerCompany.setId(1L);
        partnerCompany.setName("Esprit Tech Solutions");
        partnerCompany.setSector("IT");
        partnerCompany.setContactEmail("contact@esprittech.tn");
        partnerCompany.setPartnershipStatus("ACTIVE");
    }

    @Test
    @DisplayName("Should create partner company successfully")
    void testCreate() {
        // Arrange
        when(partnerCompanyRepository.save(partnerCompany)).thenReturn(partnerCompany);

        // Act
        PartnerCompany result = partnerCompanyService.create(partnerCompany);

        // Assert
        assertNotNull(result);
        assertEquals("Esprit Tech Solutions", result.getName());
        assertEquals("IT", result.getSector());
        assertEquals("ACTIVE", result.getPartnershipStatus());
        verify(partnerCompanyRepository, times(1)).save(partnerCompany);
    }

    @Test
    @DisplayName("Should get all partner companies")
    void testGetAll() {
        // Arrange
        List<PartnerCompany> companies = Arrays.asList(partnerCompany);
        when(partnerCompanyRepository.findAll()).thenReturn(companies);

        // Act
        List<PartnerCompany> result = partnerCompanyService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Esprit Tech Solutions", result.get(0).getName());
        verify(partnerCompanyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get partner company by ID successfully")
    void testGetByIdSuccess() {
        // Arrange
        when(partnerCompanyRepository.findById(1L)).thenReturn(Optional.of(partnerCompany));

        // Act
        PartnerCompany result = partnerCompanyService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Esprit Tech Solutions", result.getName());
        verify(partnerCompanyRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when partner company not found")
    void testGetByIdNotFound() {
        // Arrange
        when(partnerCompanyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            partnerCompanyService.getById(999L);
        });
        assertEquals("Company not found", exception.getMessage());
        verify(partnerCompanyRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should update partner company successfully")
    void testUpdate() {
        // Arrange
        PartnerCompany updatedCompany = new PartnerCompany();
        updatedCompany.setName("Esprit Digital");
        updatedCompany.setSector("Innovation");
        updatedCompany.setContactEmail("info@espritdigital.tn");
        updatedCompany.setPartnershipStatus("INACTIVE");

        when(partnerCompanyRepository.findById(1L)).thenReturn(Optional.of(partnerCompany));
        when(partnerCompanyRepository.save(partnerCompany)).thenReturn(partnerCompany);

        // Act
        PartnerCompany result = partnerCompanyService.update(1L, updatedCompany);

        // Assert
        assertNotNull(result);
        assertEquals("Esprit Digital", result.getName());
        assertEquals("Innovation", result.getSector());
        assertEquals("info@espritdigital.tn", result.getContactEmail());
        assertEquals("INACTIVE", result.getPartnershipStatus());
        verify(partnerCompanyRepository, times(1)).findById(1L);
        verify(partnerCompanyRepository, times(1)).save(partnerCompany);
    }

    @Test
    @DisplayName("Should delete partner company successfully")
    void testDelete() {
        // Arrange
        doNothing().when(partnerCompanyRepository).deleteById(1L);

        // Act
        partnerCompanyService.delete(1L);

        // Assert
        verify(partnerCompanyRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should return empty list when no partner companies exist")
    void testGetAllEmpty() {
        // Arrange
        when(partnerCompanyRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<PartnerCompany> result = partnerCompanyService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(partnerCompanyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should create multiple partner companies")
    void testCreateMultipleCompanies() {
        // Arrange
        PartnerCompany company1 = new PartnerCompany();
        company1.setId(1L);
        company1.setName("Company A");
        company1.setSector("IT");
        company1.setPartnershipStatus("ACTIVE");

        PartnerCompany company2 = new PartnerCompany();
        company2.setId(2L);
        company2.setName("Company B");
        company2.setSector("Finance");
        company2.setPartnershipStatus("ACTIVE");

        when(partnerCompanyRepository.save(company1)).thenReturn(company1);
        when(partnerCompanyRepository.save(company2)).thenReturn(company2);

        // Act
        PartnerCompany result1 = partnerCompanyService.create(company1);
        PartnerCompany result2 = partnerCompanyService.create(company2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("Company A", result1.getName());
        assertEquals("Company B", result2.getName());
        verify(partnerCompanyRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Should handle null contact email field")
    void testCreateWithNullContactEmail() {
        // Arrange
        partnerCompany.setContactEmail(null);
        when(partnerCompanyRepository.save(partnerCompany)).thenReturn(partnerCompany);

        // Act
        PartnerCompany result = partnerCompanyService.create(partnerCompany);

        // Assert
        assertNotNull(result);
        assertNull(result.getContactEmail());
        verify(partnerCompanyRepository, times(1)).save(partnerCompany);
    }

    @Test
    @DisplayName("Should update only specific company fields")
    void testUpdatePartialFields() {
        // Arrange
        PartnerCompany existingCompany = new PartnerCompany();
        existingCompany.setId(1L);
        existingCompany.setName("Original Name");
        existingCompany.setSector("Original Sector");
        existingCompany.setContactEmail("original@email.tn");
        existingCompany.setPartnershipStatus("ACTIVE");

        PartnerCompany updateData = new PartnerCompany();
        updateData.setName("New Name");
        updateData.setSector("New Sector");
        updateData.setContactEmail("new@email.tn");
        updateData.setPartnershipStatus("PENDING");

        when(partnerCompanyRepository.findById(1L)).thenReturn(Optional.of(existingCompany));
        when(partnerCompanyRepository.save(existingCompany)).thenReturn(existingCompany);

        // Act
        PartnerCompany result = partnerCompanyService.update(1L, updateData);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Sector", result.getSector());
        assertEquals("new@email.tn", result.getContactEmail());
        assertEquals("PENDING", result.getPartnershipStatus());
    }
}
