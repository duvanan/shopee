package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.AttributeValueRequest;
import com.shopee.product.dto.AttributeValueResponse;
import com.shopee.product.dto.ProductAttributeRequest;
import com.shopee.product.dto.ProductAttributeResponse;
import com.shopee.product.entity.AttributeDataType;
import com.shopee.product.entity.AttributeInputType;
import com.shopee.product.entity.AttributeValue;
import com.shopee.product.entity.ProductAttribute;
import com.shopee.product.repository.AttributeValueRepository;
import com.shopee.product.repository.ProductAttributeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductAttributeService {

    /** findAll() khong dam bao thu tu on dinh giua cac lan goi (Postgres co the tra khac thu tu sau UPDATE) - luon sort de UI khong bi xao tron dong. */
    private static final Sort SORT_ORDER_THEN_NAME = Sort.by("sortOrder", "name");
    /** AttributeValue khong co field "name" - chi co "value" (xem entity AttributeValue), phai dung sort rieng. */
    private static final Sort SORT_ORDER_THEN_VALUE = Sort.by("sortOrder", "value");

    private final ProductAttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    public ProductAttributeService(ProductAttributeRepository attributeRepository,
                                    AttributeValueRepository attributeValueRepository) {
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
    }

    public List<ProductAttributeResponse> list() {
        return attributeRepository.findAll(SORT_ORDER_THEN_NAME).stream().map(ProductAttributeResponse::from).toList();
    }

    public ProductAttributeResponse create(ProductAttributeRequest request) {
        if (attributeRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã thuộc tính đã tồn tại");
        }
        ProductAttribute attribute = ProductAttribute.builder()
                .code(request.code())
                .name(request.name())
                .dataType(parseDataType(request.dataType()))
                .inputType(parseInputType(request.inputType()))
                .unit(request.unit())
                .attributeGroup(request.attributeGroup())
                .filterable(Boolean.TRUE.equals(request.isFilterable()))
                .searchable(Boolean.TRUE.equals(request.isSearchable()))
                .comparable(Boolean.TRUE.equals(request.isComparable()))
                .allowVariant(Boolean.TRUE.equals(request.allowVariant()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();
        return ProductAttributeResponse.from(attributeRepository.save(attribute));
    }

    public ProductAttributeResponse update(UUID id, ProductAttributeRequest request) {
        ProductAttribute attribute = findOrThrow(id);
        attribute.setName(request.name());
        attribute.setDataType(parseDataType(request.dataType()));
        attribute.setInputType(parseInputType(request.inputType()));
        attribute.setUnit(request.unit());
        attribute.setAttributeGroup(request.attributeGroup());
        attribute.setFilterable(Boolean.TRUE.equals(request.isFilterable()));
        attribute.setSearchable(Boolean.TRUE.equals(request.isSearchable()));
        attribute.setComparable(Boolean.TRUE.equals(request.isComparable()));
        attribute.setAllowVariant(Boolean.TRUE.equals(request.allowVariant()));
        attribute.setSortOrder(request.sortOrder() == null ? attribute.getSortOrder() : request.sortOrder());
        return ProductAttributeResponse.from(attributeRepository.save(attribute));
    }

    public List<AttributeValueResponse> listValues(UUID attributeId) {
        findOrThrow(attributeId);
        return attributeValueRepository.findByAttributeId(attributeId, SORT_ORDER_THEN_VALUE).stream().map(AttributeValueResponse::from).toList();
    }

    public AttributeValueResponse createValue(UUID attributeId, AttributeValueRequest request) {
        findOrThrow(attributeId);
        if (attributeValueRepository.existsByAttributeIdAndCode(attributeId, request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã giá trị đã tồn tại trong thuộc tính này");
        }
        AttributeValue value = AttributeValue.builder()
                .attributeId(attributeId)
                .code(request.code())
                .value(request.value())
                .displayValue(request.displayValue())
                .colorCode(request.colorCode())
                .imageUrl(request.imageUrl())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();
        return AttributeValueResponse.from(attributeValueRepository.save(value));
    }

    public AttributeValueResponse updateValue(UUID attributeId, UUID valueId, AttributeValueRequest request) {
        findOrThrow(attributeId);
        AttributeValue value = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND));
        if (!value.getAttributeId().equals(attributeId)) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
        }
        value.setValue(request.value());
        value.setDisplayValue(request.displayValue());
        value.setColorCode(request.colorCode());
        value.setImageUrl(request.imageUrl());
        value.setSortOrder(request.sortOrder() == null ? value.getSortOrder() : request.sortOrder());
        return AttributeValueResponse.from(attributeValueRepository.save(value));
    }

    private ProductAttribute findOrThrow(UUID id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND));
    }

    private AttributeDataType parseDataType(String value) {
        try {
            return AttributeDataType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "dataType không hợp lệ");
        }
    }

    private AttributeInputType parseInputType(String value) {
        try {
            return AttributeInputType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "inputType không hợp lệ");
        }
    }
}
