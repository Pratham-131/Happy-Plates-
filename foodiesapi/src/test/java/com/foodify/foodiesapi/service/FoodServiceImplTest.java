package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.FoodEntity;
import com.foodify.foodiesapi.io.FoodRequest;
import com.foodify.foodiesapi.io.FoodResponse;
import com.foodify.foodiesapi.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceImplTest {

    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private FoodServiceImpl foodService;

    private FoodRequest request;
    private FoodEntity entity;

    @BeforeEach
    void setUp() {
        // force stub storage mode so no real S3/local file IO happens
        ReflectionTestUtils.setField(foodService, "stubStorage", true);
        ReflectionTestUtils.setField(foodService, "storageType", "local");
        ReflectionTestUtils.setField(foodService, "baseUrl", "http://localhost:8081");

        request = new FoodRequest("Chicken Biryani", "Spicy basmati rice", 250.0, "Biryani", 4.5);

        entity = FoodEntity.builder()
                .id("food123")
                .name("Chicken Biryani")
                .description("Spicy basmati rice")
                .category("Biryani")
                .price(250.0)
                .rating(4.5)
                .imageUrl("https://stub-bucket.local/abc.jpg")
                .build();
    }

    private MultipartFile mockFile() {
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
    }

    @Test
    void uploadFile_stubMode_returnsStubUrl() {
        String url = foodService.uploadFile(mockFile());
        assertTrue(url.startsWith("https://stub-bucket.local/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void addFood_success() {
        when(foodRepository.save(any(FoodEntity.class))).thenReturn(entity);

        FoodResponse response = foodService.addFood(request, mockFile());

        assertNotNull(response);
        assertEquals("food123", response.getId());
        assertEquals("Chicken Biryani", response.getName());
        assertEquals(250.0, response.getPrice());
        verify(foodRepository).save(any(FoodEntity.class));
    }

    @Test
    void updateFood_success() {
        when(foodRepository.findById("food123")).thenReturn(Optional.of(entity));
        when(foodRepository.save(any(FoodEntity.class))).thenReturn(entity);

        FoodRequest updateReq = new FoodRequest("Mutton Biryani", "Slow-cooked mutton", 300.0, "Biryani", 4.7);
        FoodResponse response = foodService.updateFood("food123", updateReq, null);

        assertNotNull(response);
        verify(foodRepository).findById("food123");
        verify(foodRepository).save(any(FoodEntity.class));
    }

    @Test
    void updateFood_notFound_throwsException() {
        when(foodRepository.findById("badId")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> foodService.updateFood("badId", request, null));

        assertTrue(ex.getMessage().contains("Food not found"));
        verify(foodRepository, never()).save(any());
    }

    @Test
    void updateFood_withNewFile_updatesImageUrl() {
        when(foodRepository.findById("food123")).thenReturn(Optional.of(entity));
        when(foodRepository.save(any(FoodEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FoodResponse response = foodService.updateFood("food123", request, mockFile());

        assertNotNull(response.getImageUrl());
        assertTrue(response.getImageUrl().startsWith("https://stub-bucket.local/"));
    }

    @Test
    void readFoods_returnsList() {
        FoodEntity entity2 = FoodEntity.builder()
                .id("food124")
                .name("Veg Burger")
                .category("Burger")
                .price(150.0)
                .rating(4.3)
                .imageUrl("https://stub-bucket.local/def.jpg")
                .build();

        when(foodRepository.findAll()).thenReturn(Arrays.asList(entity, entity2));

        List<FoodResponse> results = foodService.readFoods();

        assertEquals(2, results.size());
        assertEquals("Chicken Biryani", results.get(0).getName());
        assertEquals("Veg Burger", results.get(1).getName());
    }

    @Test
    void readFood_success() {
        when(foodRepository.findById("food123")).thenReturn(Optional.of(entity));

        FoodResponse response = foodService.readFood("food123");

        assertEquals("food123", response.getId());
        assertEquals("Chicken Biryani", response.getName());
    }

    @Test
    void readFood_notFound_throwsException() {
        when(foodRepository.findById("badId")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> foodService.readFood("badId"));

        assertTrue(ex.getMessage().contains("Food not found"));
    }

    @Test
    void deleteFile_stubMode_returnsTrue() {
        assertTrue(foodService.deleteFile("anyfile.jpg"));
    }

    @Test
    void deleteFood_success_deletesById() {
        when(foodRepository.findById("food123")).thenReturn(Optional.of(entity));
        doNothing().when(foodRepository).deleteById("food123");

        foodService.deleteFood("food123");

        verify(foodRepository).deleteById("food123");
    }
}
