package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.ProductTypeRequest;
import com.shopee.product.dto.ProductTypeResponse;
import com.shopee.product.entity.AttributeSet;
import com.shopee.product.entity.CommonStatus;
import com.shopee.product.entity.ProductType;
import com.shopee.product.repository.AttributeSetRepository;
import com.shopee.product.repository.ProductRepository;
import com.shopee.product.repository.ProductTypeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Quan ly Product Type (SMARTPHONE, T_SHIRT...) - quyet dinh Attribute Set mac dinh khi tao san pham. */
@Service
public class ProductTypeService {

    private static final Sort SORT_ORDER_THEN_NAME = Sort.by("sortOrder", "name");

    private final ProductTypeRepository productTypeRepository;
    private final AttributeSetRepository attributeSetRepository;
    private final ProductRepository productRepository;

    public ProductTypeService(ProductTypeRepository productTypeRepository, AttributeSetRepository attributeSetRepository,
                               ProductRepository productRepository) {
        this.productTypeRepository = productTypeRepository;
        this.attributeSetRepository = attributeSetRepository;
        this.productRepository = productRepository;
    }

    public List<ProductTypeResponse> list() {
        List<ProductType> types = productTypeRepository.findAll(SORT_ORDER_THEN_NAME).stream()
                .filter(t -> t.getDeletedAt() == null)
                .toList();
        Map<UUID, String> setNames = attributeSetRepository.findAllById(
                types.stream().map(ProductType::getDefaultAttributeSetId).filter(java.util.Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(AttributeSet::getId, AttributeSet::getName));

        return types.stream()
                .map(t -> ProductTypeResponse.from(t, setNames.get(t.getDefaultAttributeSetId()),
                        productRepository.countByProductTypeIdAndDeletedAtIsNull(t.getId())))
                .toList();
    }

    public ProductTypeResponse get(UUID id) {
        ProductType type = findOrThrow(id);
        String setName = type.getDefaultAttributeSetId() == null ? null
                : attributeSetRepository.findById(type.getDefaultAttributeSetId()).map(AttributeSet::getName).orElse(null);
        return ProductTypeResponse.from(type, setName, productRepository.countByProductTypeIdAndDeletedAtIsNull(id));
    }

    public ProductTypeResponse create(ProductTypeRequest request) {
        if (productTypeRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã loại sản phẩm đã tồn tại");
        }
        AttributeSet defaultSet = resolveAttributeSet(request.defaultAttributeSetId());

        ProductType type = ProductType.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .defaultAttributeSetId(defaultSet != null ? defaultSet.getId() : null)
                .iconUrl(request.iconUrl())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();
        type = productTypeRepository.save(type);
        return ProductTypeResponse.from(type, defaultSet != null ? defaultSet.getName() : null, 0);
    }

    public ProductTypeResponse update(UUID id, ProductTypeRequest request) {
        ProductType type = findOrThrow(id);
        AttributeSet defaultSet = resolveAttributeSet(request.defaultAttributeSetId());

        type.setName(request.name());
        type.setDescription(request.description());
        type.setDefaultAttributeSetId(defaultSet != null ? defaultSet.getId() : null);
        type.setIconUrl(request.iconUrl());
        type.setSortOrder(request.sortOrder() == null ? type.getSortOrder() : request.sortOrder());
        type = productTypeRepository.save(type);
        return ProductTypeResponse.from(type, defaultSet != null ? defaultSet.getName() : null,
                productRepository.countByProductTypeIdAndDeletedAtIsNull(id));
    }

    public void activate(UUID id) {
        ProductType type = findOrThrow(id);
        type.setStatus(CommonStatus.ACTIVE);
        productTypeRepository.save(type);
    }

    public void deactivate(UUID id) {
        ProductType type = findOrThrow(id);
        type.setStatus(CommonStatus.INACTIVE);
        productTypeRepository.save(type);
    }

    public void delete(UUID id) {
        findOrThrow(id);
        if (productRepository.existsByProductTypeIdAndDeletedAtIsNull(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Loại sản phẩm đang được sản phẩm sử dụng, không thể xoá");
        }
        ProductType type = findOrThrow(id);
        type.setDeletedAt(Instant.now());
        productTypeRepository.save(type);
    }

    private AttributeSet resolveAttributeSet(UUID attributeSetId) {
        if (attributeSetId == null) {
            return null;
        }
        return attributeSetRepository.findByIdAndDeletedAtIsNull(attributeSetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bộ thuộc tính"));
    }

    private ProductType findOrThrow(UUID id) {
        return productTypeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại sản phẩm"));
    }
}
