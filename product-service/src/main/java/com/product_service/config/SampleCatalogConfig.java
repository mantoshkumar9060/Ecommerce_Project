package com.product_service.config;

import com.product_service.entity.Brand;
import com.product_service.entity.Category;
import com.product_service.entity.Product;
import com.product_service.entity.Size;
import com.product_service.entity.SubCategory;
import com.product_service.repository.CategoryRepository;
import com.product_service.repository.ProductRepository;
import com.product_service.repository.SubCategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleCatalogConfig {

    @Bean
    CommandLineRunner sampleCatalog(CategoryRepository categories, SubCategoryRepository subCategories,
                                    ProductRepository products) {
        return args -> {
            SubCategory men = subCategory("Men", "Menswear", categories, subCategories);
            SubCategory women = subCategory("Women", "Womenswear", categories, subCategories);
            SubCategory shoes = subCategory("Footwear", "Sneakers", categories, subCategories);

            seed(products, men, List.of(
                    item("Classic Cotton T-Shirt", "Roadster", "799"), item("Relaxed Fit Oxford Shirt", "HIGHLANDER", "1199"),
                    item("Slim Fit Denim Jeans", "Levi's", "2199"), item("Everyday Denim Jacket", "WROGN", "2499"),
                    item("Linen Blend Resort Shirt", "Mast & Harbour", "1399"), item("Pique Polo T-Shirt", "Tommy Hilfiger", "1899"),
                    item("Tapered Chino Trousers", "U.S. Polo Assn.", "1699"), item("Checked Casual Shirt", "Louis Philippe", "1499"),
                    item("Crew Neck Sweatshirt", "Puma", "1799"), item("Lightweight Bomber Jacket", "Jack & Jones", "2999"),
                    item("Regular Fit Cargo Pants", "H&M", "1599"), item("Striped Cotton Shirt", "Blackberrys", "1899"),
                    item("Graphic Print Hoodie", "HRX", "1499"), item("Textured Knit Pullover", "Van Heusen", "2099"),
                    item("Classic Fit Blazer", "Allen Solly", "4499"), item("Washed Denim Overshirt", "GAP", "2299"),
                    item("Performance Training Tee", "Adidas", "1299"), item("Cotton Henley T-Shirt", "Flying Machine", "999"),
                    item("Smart Casual Waistcoat", "Raymond", "2699"), item("Lounge Jogger Pants", "Marks & Spencer", "1399")));
            seed(products, women, List.of(
                    item("Floral Summer Dress", "DressBerry", "1899"), item("Satin Slip Midi Dress", "MANGO", "2999"),
                    item("Oversized Graphic Tee", "Tokyo Talkies", "899"), item("High Rise Wide Leg Jeans", "VERO MODA", "2399"),
                    item("Printed A-Line Kurta", "Biba", "1599"), item("Pleated Midi Skirt", "ONLY", "1899"),
                    item("Ribbed Knit Top", "Forever New", "1299"), item("Tailored Linen Blazer", "Zara", "3899"),
                    item("Classic Straight Jeans", "Levi's", "2499"), item("Embroidered Cotton Kurta", "W", "1799"),
                    item("Satin Party Shirt", "Mast & Harbour", "1499"), item("Cropped Denim Jacket", "Vero Moda", "2299"),
                    item("Relaxed Fit Co-ord Set", "SASSAFRAS", "2199"), item("Floral Wrap Dress", "AND", "2699"),
                    item("Cotton Poplin Shirt", "H&M", "1199"), item("Wide Leg Trousers", "MANGO", "2399"),
                    item("Ruffle Detail Top", "DressBerry", "1099"), item("Printed Palazzo Pants", "Biba", "1399"),
                    item("Soft Touch Cardigan", "Marks & Spencer", "2499"), item("Everyday Tank Top", "ONLY", "699")));
            seed(products, shoes, List.of(
                    item("Minimal Running Shoes", "Puma", "3199"), item("Court Vision Sneakers", "Nike", "4299"),
                    item("Leather Everyday Loafers", "Allen Solly", "2799"), item("Canvas Weekend Sneakers", "HRX", "1599"),
                    item("Retro Suede Trainers", "Adidas", "3999"), item("Cushioned Walking Shoes", "Skechers", "4599"),
                    item("Platform Casual Sneakers", "Converse", "3299"), item("Leather Chelsea Boots", "Red Tape", "3499"),
                    item("Mesh Training Shoes", "ASICS", "4199"), item("Classic White Sneakers", "Puma", "2799"),
                    item("Comfort Slide Sandals", "Crocs", "2299"), item("Formal Derby Shoes", "Louis Philippe", "3199"),
                    item("Trail Running Shoes", "New Balance", "5299"), item("Slip On Canvas Shoes", "Vans", "2899"),
                    item("Minimal Block Heels", "Mochi", "1999"), item("Everyday Ballet Flats", "Metro", "1599"),
                    item("Sporty Sandals", "Woodland", "2499"), item("Chunky Street Sneakers", "Nike", "4999"),
                    item("Leather Office Loafers", "Clarks", "4299"), item("Lightweight Slip Ons", "Skechers", "3699")));
        };
    }

    private SubCategory subCategory(String categoryName, String subCategoryName, CategoryRepository categories,
                                    SubCategoryRepository subCategories) {
        return subCategories.findByNameIgnoreCase(subCategoryName).orElseGet(() -> {
            Category category = categories.findByNameIgnoreCase(categoryName).orElseGet(() -> {
                Category created = new Category();
                created.setName(categoryName);
                return categories.save(created);
            });
            SubCategory created = new SubCategory();
            created.setName(subCategoryName);
            created.setCategory(category);
            return subCategories.save(created);
        });
    }

    private void addIfMissing(ProductRepository products, String name, SubCategory subCategory, String brandName,
                              String price) {
        if (!products.existsByNameIgnoreCase(name)) {
            products.save(product(name, subCategory, brandName, price));
        }
    }

    private void seed(ProductRepository products, SubCategory subCategory, List<CatalogItem> items) {
        items.forEach(item -> addIfMissing(products, item.name(), subCategory, item.brandName(), item.price()));
    }

    private CatalogItem item(String name, String brandName, String price) {
        return new CatalogItem(name, brandName, price);
    }

    private record CatalogItem(String name, String brandName, String price) {
    }

    private Product product(String name, SubCategory subCategory, String brandName, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSubCategory(subCategory);
        Brand brand = new Brand();
        brand.setName(brandName);
        brand.setPrice(new BigDecimal(price));
        brand.setProduct(product);
        Size size = new Size();
        size.setSize("M");
        size.setQuantity("25");
        size.setBrand(brand);
        brand.getSizes().add(size);
        product.getBrands().add(brand);
        return product;
    }
}
