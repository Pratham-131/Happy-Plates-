package com.foodify.foodiesapi.service;

import com.foodify.foodiesapi.entity.FoodEntity;
import com.foodify.foodiesapi.io.FoodRequest;
import com.foodify.foodiesapi.io.FoodResponse;
import com.foodify.foodiesapi.repository.FoodRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service

public class FoodServiceImpl implements FoodService {

   @Autowired
    private  S3Client s3Client;
   @Autowired
    private FoodRepository foodRepository;
    @Value("${aws.s3.bucketname}")
    private String bucketName;

    @Value("${app.stub.storage:false}")
    private boolean stubStorage;

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8081}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file) {
        String filenameExtension=file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")+1);
        String key=UUID.randomUUID().toString()+"."+filenameExtension;

        if (stubStorage) {
            return "https://stub-bucket.local/"+key;
        }

        if ("local".equalsIgnoreCase(storageType)) {
            try {
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                java.io.File dest = new java.io.File(dir, key);
                file.transferTo(dest);
                return baseUrl + "/uploads/" + key;
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "file upload failed");
            }
        }

        try{

            PutObjectRequest putObjectRequest= PutObjectRequest.builder().bucket(bucketName)
                    .key(key)
                    .acl("public-read") //.acl(ObjectCannedACL.PUBLIC_READ)
                    .contentType(file.getContentType())
                    .build();


            PutObjectResponse response= s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            if(response.sdkHttpResponse().isSuccessful()){
                return "https://"+bucketName+".s3.amazonaws.com/"+key;
            }

            else{
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"file upload failed");

            }
        }

        catch(IOException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"an error occured while uploading the file");
        }
    }

    @Override
    @CacheEvict(value = "foods", allEntries = true)
    public FoodResponse addFood(FoodRequest request, MultipartFile file) {
        FoodEntity newFoodEntity = convertToEntity(request);
        String imageUrl = uploadFile(file);
        newFoodEntity.setImageUrl(imageUrl);
        newFoodEntity = foodRepository.save(newFoodEntity);
        System.out.println("successfully saved");
        return convertToResponse(newFoodEntity);
    }

    @Override
    @CacheEvict(value = {"foods", "food"}, allEntries = true)
    public FoodResponse updateFood(String id, FoodRequest request, MultipartFile file) {
        FoodEntity existingFood = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food not found for the id:" + id));

        existingFood.setName(request.getName());
        existingFood.setDescription(request.getDescription());
        existingFood.setCategory(request.getCategory());
        existingFood.setPrice(request.getPrice());
        existingFood.setRating(request.getRating());

        if (file != null && !file.isEmpty()) {
            String imageUrl = uploadFile(file);
            existingFood.setImageUrl(imageUrl);
        }

        existingFood = foodRepository.save(existingFood);
        return convertToResponse(existingFood);
    }

    @Override
    @Cacheable(value = "foods")
    public List<FoodResponse> readFoods() {
        List<FoodEntity> databaseEntries = foodRepository.findAll();
        return databaseEntries.stream().map(object -> convertToResponse(object)).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "food", key = "#id")
    public FoodResponse readFood(String id) {
        FoodEntity existingFood = foodRepository.findById(id).orElseThrow(() -> new RuntimeException("Food not found for the id:"+id));
        return convertToResponse(existingFood);
    }

    @Override
    public boolean deleteFile(String filename) {
        if (stubStorage) {
            return true;
        }

        if ("local".equalsIgnoreCase(storageType)) {
            java.io.File f = new java.io.File(uploadDir, filename);
            if (f.exists()) f.delete();
            return true;
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        return true;
    }

    @Override
    @CacheEvict(value = {"foods", "food"}, allEntries = true)
    public void deleteFood(String id) {
        FoodResponse response = readFood(id);
        String imageUrl = response.getImageUrl();
        String filename = imageUrl.substring(imageUrl.lastIndexOf("/")+1);
        boolean isFileDelete = deleteFile(filename);
        if (isFileDelete) {
            foodRepository.deleteById(response.getId());
        }
    }

    private FoodEntity convertToEntity(FoodRequest request) {
        return FoodEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .rating(request.getRating())
                .build();

    }

    private FoodResponse convertToResponse(FoodEntity entity) {
        return FoodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .price(entity.getPrice())
                .imageUrl(entity.getImageUrl())
                .rating(entity.getRating())
                .build();
    }
}