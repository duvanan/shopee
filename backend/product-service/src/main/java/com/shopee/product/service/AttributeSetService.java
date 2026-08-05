package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.*;
import com.shopee.product.entity.*;
import com.shopee.product.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quan ly Attribute Set (bo thuoc tinh mau) va "schema" (group + attribute
 * config trong set) - phan backend cua Attribute Set Builder. Xem
 * docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md
 * muc 7, 11, 15, 17, 41.
 */
@Service
public class AttributeSetService {

    private static final Sort SORT_ORDER = Sort.by("sortOrder");

    private final AttributeSetRepository attributeSetRepository;
    private final AttributeSetGroupRepository groupRepository;
    private final AttributeSetAttributeRepository attributeRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductRepository productRepository;

    public AttributeSetService(AttributeSetRepository attributeSetRepository, AttributeSetGroupRepository groupRepository,
                                AttributeSetAttributeRepository attributeRepository,
                                ProductAttributeRepository productAttributeRepository,
                                AttributeValueRepository attributeValueRepository,
                                ProductTypeRepository productTypeRepository, ProductRepository productRepository) {
        this.attributeSetRepository = attributeSetRepository;
        this.groupRepository = groupRepository;
        this.attributeRepository = attributeRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.productTypeRepository = productTypeRepository;
        this.productRepository = productRepository;
    }

    // ---------- CRUD ----------

    public List<AttributeSetResponse> list() {
        List<AttributeSet> sets = attributeSetRepository.findAll(Sort.by("name")).stream()
                .filter(s -> s.getDeletedAt() == null)
                .toList();
        Map<UUID, List<AttributeSetAttribute>> attrsBySet = attributeRepository
                .findByAttributeSetIdIn(sets.stream().map(AttributeSet::getId).toList())
                .stream().collect(Collectors.groupingBy(AttributeSetAttribute::getAttributeSetId));

        return sets.stream().map(set -> {
            List<AttributeSetAttribute> attrs = attrsBySet.getOrDefault(set.getId(), List.of());
            long groupCount = attrs.stream().map(AttributeSetAttribute::getGroupId).filter(Objects::nonNull).distinct().count();
            long productTypeCount = productTypeRepository.findAll().stream()
                    .filter(t -> t.getDeletedAt() == null && set.getId().equals(t.getDefaultAttributeSetId())).count();
            return AttributeSetResponse.from(set, (int) groupCount, attrs.size(), productTypeCount);
        }).toList();
    }

    public AttributeSetResponse get(UUID id) {
        AttributeSet set = findOrThrow(id);
        List<AttributeSetAttribute> attrs = attributeRepository.findByAttributeSetId(id, SORT_ORDER);
        long groupCount = attrs.stream().map(AttributeSetAttribute::getGroupId).filter(Objects::nonNull).distinct().count();
        long productTypeCount = productTypeRepository.findAll().stream()
                .filter(t -> t.getDeletedAt() == null && id.equals(t.getDefaultAttributeSetId())).count();
        return AttributeSetResponse.from(set, (int) groupCount, attrs.size(), productTypeCount);
    }

    public AttributeSetResponse create(AttributeSetRequest request) {
        if (attributeSetRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã bộ thuộc tính đã tồn tại");
        }
        AttributeSet set = AttributeSet.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .build();
        set = attributeSetRepository.save(set);
        return AttributeSetResponse.from(set, 0, 0, 0);
    }

    public AttributeSetResponse update(UUID id, AttributeSetRequest request) {
        AttributeSet set = findOrThrow(id);
        set.setName(request.name());
        set.setDescription(request.description());
        set = attributeSetRepository.save(set);
        return get(set.getId());
    }

    public void delete(UUID id) {
        findOrThrow(id);
        if (productRepository.existsByAttributeSetIdAndDeletedAtIsNull(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Bộ thuộc tính đang được sản phẩm sử dụng, không thể xoá");
        }
        boolean usedByProductType = productTypeRepository.findAll().stream()
                .anyMatch(t -> t.getDeletedAt() == null && id.equals(t.getDefaultAttributeSetId()));
        if (usedByProductType) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Bộ thuộc tính đang được loại sản phẩm sử dụng, không thể xoá");
        }
        AttributeSet set = findOrThrow(id);
        set.setDeletedAt(Instant.now());
        attributeSetRepository.save(set);
    }

    @Transactional
    public AttributeSetResponse clone(UUID id, String newCode, String newName) {
        AttributeSet source = findOrThrow(id);
        if (attributeSetRepository.existsByCode(newCode)) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã bộ thuộc tính đã tồn tại");
        }
        AttributeSet clone = attributeSetRepository.save(AttributeSet.builder()
                .code(newCode)
                .name(newName)
                .description(source.getDescription())
                .build());

        Map<UUID, UUID> oldGroupIdToNew = new HashMap<>();
        for (AttributeSetGroup group : groupRepository.findByAttributeSetId(id, SORT_ORDER)) {
            AttributeSetGroup newGroup = groupRepository.save(AttributeSetGroup.builder()
                    .attributeSetId(clone.getId())
                    .code(group.getCode())
                    .name(group.getName())
                    .description(group.getDescription())
                    .collapsible(group.isCollapsible())
                    .defaultExpanded(group.isDefaultExpanded())
                    .sortOrder(group.getSortOrder())
                    .build());
            oldGroupIdToNew.put(group.getId(), newGroup.getId());
        }

        for (AttributeSetAttribute attr : attributeRepository.findByAttributeSetId(id, SORT_ORDER)) {
            attributeRepository.save(AttributeSetAttribute.builder()
                    .attributeSetId(clone.getId())
                    .groupId(attr.getGroupId() == null ? null : oldGroupIdToNew.get(attr.getGroupId()))
                    .attributeId(attr.getAttributeId())
                    .labelOverride(attr.getLabelOverride())
                    .helpText(attr.getHelpText())
                    .placeholder(attr.getPlaceholder())
                    .required(attr.isRequired())
                    .variantAttribute(attr.isVariantAttribute())
                    .filterable(attr.isFilterable())
                    .searchable(attr.isSearchable())
                    .comparable(attr.isComparable())
                    .visible(attr.isVisible())
                    .editable(attr.isEditable())
                    .defaultValueId(attr.getDefaultValueId())
                    .defaultValueText(attr.getDefaultValueText())
                    .sortOrder(attr.getSortOrder())
                    .build());
        }

        return get(clone.getId());
    }

    // ---------- Schema (Attribute Set Builder) ----------

    public AttributeSchemaResponse getSchema(UUID attributeSetId) {
        AttributeSet set = findOrThrow(attributeSetId);
        return buildSchema(null, set);
    }

    /** Dung khi tao san pham: tu Product Type -> lay Attribute Set mac dinh -> tra ve schema day du de FE tu sinh form. */
    public AttributeSchemaResponse getSchemaByProductType(UUID productTypeId) {
        ProductType type = productTypeRepository.findByIdAndDeletedAtIsNull(productTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại sản phẩm"));
        if (type.getDefaultAttributeSetId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Loại sản phẩm chưa gán bộ thuộc tính mặc định");
        }
        AttributeSet set = attributeSetRepository.findByIdAndDeletedAtIsNull(type.getDefaultAttributeSetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bộ thuộc tính"));
        return buildSchema(type, set);
    }

    private AttributeSchemaResponse buildSchema(ProductType type, AttributeSet set) {
        List<AttributeSetGroup> groups = groupRepository.findByAttributeSetId(set.getId(), SORT_ORDER);
        List<AttributeSetAttribute> attrs = attributeRepository.findByAttributeSetId(set.getId(), SORT_ORDER);

        Map<UUID, ProductAttribute> attributesById = productAttributeRepository
                .findAllById(attrs.stream().map(AttributeSetAttribute::getAttributeId).toList())
                .stream().collect(Collectors.toMap(ProductAttribute::getId, a -> a));
        Map<UUID, List<AttributeValueResponse>> valuesByAttribute = attributeValueRepository
                .findByAttributeIdIn(attributesById.keySet()).stream()
                .collect(Collectors.groupingBy(AttributeValue::getAttributeId,
                        Collectors.mapping(AttributeValueResponse::from, Collectors.toList())));

        Map<UUID, List<AttributeSetAttribute>> attrsByGroup = attrs.stream()
                .collect(Collectors.groupingBy(a -> a.getGroupId() == null ? NO_GROUP : a.getGroupId()));

        List<AttributeSchemaGroupResponse> groupResponses = new ArrayList<>();
        for (AttributeSetGroup group : groups) {
            groupResponses.add(toGroupResponse(group.getId(), group.getCode(), group.getName(), group.getSortOrder(),
                    group.isCollapsible(), group.isDefaultExpanded(),
                    attrsByGroup.getOrDefault(group.getId(), List.of()), attributesById, valuesByAttribute));
        }
        if (attrsByGroup.containsKey(NO_GROUP)) {
            groupResponses.add(toGroupResponse(null, "UNGROUPED", "Khác", 999, true, true,
                    attrsByGroup.get(NO_GROUP), attributesById, valuesByAttribute));
        }

        return new AttributeSchemaResponse(
                type != null ? type.getId() : null, type != null ? type.getCode() : null, type != null ? type.getName() : null,
                set.getId(), set.getCode(), set.getName(), set.getVersionNo(), groupResponses
        );
    }

    private static final UUID NO_GROUP = new UUID(0, 0);

    private AttributeSchemaGroupResponse toGroupResponse(UUID groupId, String code, String name, Integer sortOrder,
                                                           boolean collapsible, boolean defaultExpanded,
                                                           List<AttributeSetAttribute> attrs,
                                                           Map<UUID, ProductAttribute> attributesById,
                                                           Map<UUID, List<AttributeValueResponse>> valuesByAttribute) {
        List<AttributeSchemaFieldResponse> fields = attrs.stream()
                .sorted(Comparator.comparing(AttributeSetAttribute::getSortOrder))
                .map(a -> {
                    ProductAttribute attr = attributesById.get(a.getAttributeId());
                    if (attr == null) {
                        return null;
                    }
                    return new AttributeSchemaFieldResponse(
                            attr.getId(), attr.getCode(), attr.getName(),
                            a.getLabelOverride() != null ? a.getLabelOverride() : attr.getName(),
                            attr.getDataType().name(), attr.getInputType().name(), attr.getUnit(),
                            a.isRequired(), a.isVariantAttribute(), a.isFilterable(), a.isSearchable(), a.isComparable(),
                            a.isVisible(), a.isEditable(), a.getPlaceholder(), a.getHelpText(),
                            a.getDefaultValueId(), a.getDefaultValueText(),
                            a.isVariantAttribute() || attr.isAllowPredefinedValues()
                                    ? valuesByAttribute.getOrDefault(attr.getId(), List.of()) : List.of()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
        return new AttributeSchemaGroupResponse(groupId, code, name, sortOrder, collapsible, defaultExpanded, fields);
    }

    @Transactional
    public AttributeSchemaResponse setSchema(UUID attributeSetId, SetAttributeSetSchemaRequest request) {
        AttributeSet set = findOrThrow(attributeSetId);
        List<AttributeSetSchemaGroupRequest> groups = request.groups() == null ? List.of() : request.groups();

        validateSchema(groups);

        // Xoa CON (attribute_set_attributes) truoc khi xoa CHA (attribute_set_groups) de khong vi pham FK.
        attributeRepository.deleteByAttributeSetId(attributeSetId);
        groupRepository.deleteByAttributeSetId(attributeSetId);

        int groupOrder = 0;
        for (AttributeSetSchemaGroupRequest groupReq : groups) {
            AttributeSetGroup group = groupRepository.save(AttributeSetGroup.builder()
                    .attributeSetId(attributeSetId)
                    .code(groupReq.code())
                    .name(groupReq.name())
                    .description(groupReq.description())
                    .collapsible(groupReq.collapsible())
                    .defaultExpanded(groupReq.defaultExpanded())
                    .sortOrder(groupReq.sortOrder() == null ? groupOrder : groupReq.sortOrder())
                    .build());
            groupOrder++;

            List<AttributeSetSchemaAttributeRequest> attrs = groupReq.attributes() == null ? List.of() : groupReq.attributes();
            int attrOrder = 0;
            for (AttributeSetSchemaAttributeRequest attrReq : attrs) {
                attributeRepository.save(AttributeSetAttribute.builder()
                        .attributeSetId(attributeSetId)
                        .groupId(group.getId())
                        .attributeId(attrReq.attributeId())
                        .labelOverride(attrReq.labelOverride())
                        .helpText(attrReq.helpText())
                        .placeholder(attrReq.placeholder())
                        .required(attrReq.required())
                        .variantAttribute(attrReq.variantAttribute())
                        .filterable(attrReq.filterable())
                        .searchable(attrReq.searchable())
                        .comparable(attrReq.comparable())
                        .visible(attrReq.visible())
                        .editable(attrReq.editable())
                        .defaultValueId(attrReq.defaultValueId())
                        .defaultValueText(attrReq.defaultValueText())
                        .sortOrder(attrReq.sortOrder() == null ? attrOrder : attrReq.sortOrder())
                        .build());
                attrOrder++;
            }
        }

        set.setVersionNo(set.getVersionNo() + 1);
        attributeSetRepository.save(set);

        return getSchema(attributeSetId);
    }

    private void validateSchema(List<AttributeSetSchemaGroupRequest> groups) {
        Set<String> groupCodes = new HashSet<>();
        Set<UUID> allAttributeIds = new HashSet<>();
        for (AttributeSetSchemaGroupRequest group : groups) {
            if (!groupCodes.add(group.code())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã nhóm \"" + group.code() + "\" bị trùng");
            }
            List<AttributeSetSchemaAttributeRequest> attrs = group.attributes() == null ? List.of() : group.attributes();
            for (AttributeSetSchemaAttributeRequest attr : attrs) {
                if (!allAttributeIds.add(attr.attributeId())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Một thuộc tính không được gán trùng vào cùng bộ thuộc tính");
                }
            }
        }
        if (!allAttributeIds.isEmpty()) {
            Map<UUID, ProductAttribute> found = productAttributeRepository.findAllById(allAttributeIds).stream()
                    .collect(Collectors.toMap(ProductAttribute::getId, a -> a));
            if (found.size() != allAttributeIds.size()) {
                throw new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND);
            }
            for (AttributeSetSchemaGroupRequest group : groups) {
                List<AttributeSetSchemaAttributeRequest> attrs = group.attributes() == null ? List.of() : group.attributes();
                for (AttributeSetSchemaAttributeRequest attr : attrs) {
                    ProductAttribute attribute = found.get(attr.attributeId());
                    if (attr.variantAttribute() && !attribute.isAllowVariant()) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                "Thuộc tính \"" + attribute.getName() + "\" không được phép tạo variant (allow_variant=false)");
                    }
                }
            }
        }
    }

    private AttributeSet findOrThrow(UUID id) {
        return attributeSetRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bộ thuộc tính"));
    }
}
