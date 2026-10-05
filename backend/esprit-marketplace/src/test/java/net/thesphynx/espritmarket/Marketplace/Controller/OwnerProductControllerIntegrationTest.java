package net.thesphynx.espritmarket.Marketplace.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.GlobalExceptionHandler;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductCreateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.OwnerProductUpdateRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductPromotionRequest;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductResponse;
import net.thesphynx.espritmarket.Marketplace.Service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OwnerProductControllerIntegrationTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        OwnerProductController controller = new OwnerProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void createMyStoreProductReturns201() throws Exception {
        OwnerProductCreateRequest request = new OwnerProductCreateRequest();
        request.setName("Phone");
        request.setDescription("Desc");
        request.setPrice(100.0);
        request.setStock(3);
        request.setCategoryId(11L);

        ProductResponse response = new ProductResponse();
        response.setId(100L);

        when(productService.createInMyStore(any(OwnerProductCreateRequest.class), eq("owner@test.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/products/my-store")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void ownerUpdateReturns200() throws Exception {
        OwnerProductUpdateRequest request = new OwnerProductUpdateRequest();
        request.setDescription("updated");
        request.setPrice(50.0);
        request.setStock(9);

        when(productService.ownerUpdate(eq(1L), any(OwnerProductUpdateRequest.class), eq("owner@test.com")))
                .thenReturn(new ProductResponse());

        mockMvc.perform(put("/api/products/1/owner-update")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void ownerUpdateForbiddenReturns403() throws Exception {
        OwnerProductUpdateRequest request = new OwnerProductUpdateRequest();
        request.setDescription("updated");
        request.setPrice(50.0);
        request.setStock(9);

        when(productService.ownerUpdate(eq(1L), any(OwnerProductUpdateRequest.class), eq("owner@test.com")))
                .thenThrow(new ForbiddenException("You are not allowed to modify this product"));

        mockMvc.perform(put("/api/products/1/owner-update")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this product"));
    }

    @Test
    void ownerUpdateNotFoundReturns404() throws Exception {
        OwnerProductUpdateRequest request = new OwnerProductUpdateRequest();
        request.setDescription("updated");
        request.setPrice(50.0);
        request.setStock(9);

        when(productService.ownerUpdate(eq(404L), any(OwnerProductUpdateRequest.class), eq("owner@test.com")))
                .thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(put("/api/products/404/owner-update")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPromotionPayloadReturns400() throws Exception {
        ProductPromotionRequest request = new ProductPromotionRequest();
        request.setDiscountPercent(150.0);
        request.setPromoEndAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(patch("/api/products/1/promotion")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerDeleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/1/owner-delete")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A")))
                .andExpect(status().isNoContent());
    }

    @Test
    void removePromotionNotFoundReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("not found"))
                .when(productService)
                .removePromotion(404L, "owner@test.com");

        mockMvc.perform(delete("/api/products/404/promotion")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A")))
                .andExpect(status().isNotFound());
    }
}
