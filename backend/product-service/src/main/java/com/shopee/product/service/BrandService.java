package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.BrandRequest;
import com.shopee.product.dto.BrandResponse;
import com.shopee.product.entity.Brand;
import com.shopee.product.repository.BrandRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BrandService {

    private static final Sort SORT_ORDER_THEN_NAME = Sort.by("sortOrder", "name");

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public List<BrandResponse> list() {
        return brandRepository.findAll(SORT_ORDER_THEN_NAME).stream().map(BrandResponse::from).toList();
    }

    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã thương hiệu đã tồn tại");
        }
        if (brandRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Slug thương hiệu đã tồn tại");
        }
        Brand brand = Brand.builder()
                .code(request.code())
                .name(request.name())
                .slug(request.slug())
                .logoUrl(request.logoUrl())
                .bannerUrl(request.bannerUrl())
                .description(request.description())
                .websiteUrl(request.websiteUrl())
                .countryCode(request.countryCode())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .build();
        return BrandResponse.from(brandRepository.save(brand));
    }

    public BrandResponse update(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
        brand.setName(request.name());
        brand.setSlug(request.slug());
        brand.setLogoUrl(request.logoUrl());
        brand.setBannerUrl(request.bannerUrl());
        brand.setDescription(request.description());
        brand.setWebsiteUrl(request.websiteUrl());
        brand.setCountryCode(request.countryCode());
        brand.setSortOrder(request.sortOrder() == null ? brand.getSortOrder() : request.sortOrder());
        brand.setSeoTitle(request.seoTitle());
        brand.setSeoDescription(request.seoDescription());
        return BrandResponse.from(brandRepository.save(brand));
    }
}
