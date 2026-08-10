package com.product_service.controller;


import com.product_service.dto.ApiResponse;
import com.product_service.dto.CategoryDto;
import com.product_service.dto.ProductDto;
import com.product_service.service.CategoryService;
import com.product_service.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private CategoryService categoryService;

    private ProductService productService;

    public ProductController(CategoryService categoryService,ProductService productService){
        this.categoryService=categoryService;
        this.productService=productService;
    }

    @GetMapping("/list/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories(){
        List<CategoryDto> categoriesDto=categoryService.findAll();
        ApiResponse<List<CategoryDto>> response=new ApiResponse<>();
        if(categoriesDto != null){
            response.setMessage("All categories data fetched");
            response.setStatus(200);
            response.setData(categoriesDto);
            return new ResponseEntity<>(response, HttpStatus.OK);

        }

        response.setMessage("No categories data found");
        response.setStatus(500);
        response.setData(null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }
    @GetMapping("/list/search")
    public ResponseEntity<ApiResponse<List<ProductDto>>> searchProducts(
            @RequestParam String keyword
    ){
        List<ProductDto>productDtos = productService.searchProducts(keyword);

        ApiResponse<List<ProductDto>> response=new ApiResponse<>();
        if(productDtos != null){
            response.setMessage("All data fetched");
            response.setStatus(200);
            response.setData(productDtos);
            return new ResponseEntity<>(response, HttpStatus.OK);

        }
        response.setMessage("No categories data found");
        response.setStatus(500);
        response.setData(null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
