package net.thesphynx.espritmarket.Marketplace.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Common.Exception.ForbiddenException;
import net.thesphynx.espritmarket.Common.Exception.GlobalExceptionHandler;
import net.thesphynx.espritmarket.Common.Exception.ResourceNotFoundException;
import net.thesphynx.espritmarket.Marketplace.Dto.ProductUpdateRequest;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductUpdateEndpointIntegrationTest {

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
    void updateProductReturns200() throws Exception {
        ProductResponse response = new ProductResponse();
        response.setId(10L);
        response.setName("crk");
        response.setDescription("sac for");
        response.setPrice(550.0);
        response.setStock(5);
        response.setImageUrl("https://cdn.example.com/p.png");
        response.setStoreId(1L);
        response.setCategoryId(2L);

        when(productService.update(eq(10L), any(ProductUpdateRequest.class), eq("owner@test.com"))).thenReturn(response);

        Map<String, Object> payload = Map.of(
                "name", "crk",
                "description", "sac for",
                "price", 550,
                "stock", 5,
                "imageUrl", "https://cdn.example.com/p.png",
                "store", Map.of("id", 1),
                "category", Map.of("id", 2)
        );

        mockMvc.perform(put("/api/products/10")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("crk"));
    }

    @Test
    void updateProductReturns404WhenNotFound() throws Exception {
        when(productService.update(eq(10L), any(ProductUpdateRequest.class), eq("owner@test.com")))
                .thenThrow(new ResourceNotFoundException("Product not found with id=10"));

        Map<String, Object> payload = Map.of(
                "name", "crk",
                "price", 550,
                "stock", 5,
                "store", Map.of("id", 1),
                "category", Map.of("id", 2)
        );

        mockMvc.perform(put("/api/products/10")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProductReturns403WhenNotOwner() throws Exception {
        when(productService.update(eq(10L), any(ProductUpdateRequest.class), eq("other@test.com")))
                .thenThrow(new ForbiddenException("You are not allowed to modify this product"));

        Map<String, Object> payload = Map.of(
                "name", "crk",
                "price", 550,
                "stock", 5,
                "store", Map.of("id", 1),
                "category", Map.of("id", 2)
        );

        mockMvc.perform(put("/api/products/10")
                        .principal(new UsernamePasswordAuthenticationToken("other@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this product"));
    }

    @Test
    void updateProductReturns400WhenPayloadInvalid() throws Exception {
        Map<String, Object> payload = Map.of(
                "description", "sac for",
                "price", 550,
                "stock", 5,
                "store", Map.of("id", 1),
                "category", Map.of("id", 2)
        );

        mockMvc.perform(put("/api/products/10")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation error"));
    }

    @Test
    void updateProductReturns400WhenRelationIdsInvalid() throws Exception {
        when(productService.update(eq(10L), any(ProductUpdateRequest.class), eq("owner@test.com")))
                .thenThrow(new BadRequestException("Invalid category id=999"));

        Map<String, Object> payload = Map.of(
                "name", "crk",
                "price", 550,
                "stock", 5,
                "store", Map.of("id", 1),
                "category", Map.of("id", 999)
        );

        mockMvc.perform(put("/api/products/10")
                        .principal(new UsernamePasswordAuthenticationToken("owner@test.com", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid category id=999"));
    }
}
