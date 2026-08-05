package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.PageData;
import com.shopee.product.dto.*;
import com.shopee.product.entity.*;
import com.shopee.product.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Luong tao san pham dong: chon Product Type -> he thong tu lay Attribute
 * Set mac dinh -> validate/luu theo dung schema cua set do, khong con cho
 * admin tu chon attribute thu cong nhu truoc (xem
 * docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md).
 * Khong dung JPA relationship - moi lookup/join deu qua repository +
 * batch-fetch thu cong.
 */
@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_VARIANTS_PER_PRODUCT = 200;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductVariantRepository variantRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final AttributeValueRepository attributeValueOptionRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductTypeRepository productTypeRepository;
    private final AttributeSetRepository attributeSetRepository;
    private final AttributeSetAttributeRepository attributeSetAttributeRepository;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository productCategoryRepository,
                           ProductAttributeValueRepository attributeValueRepository,
                           ProductVariantRepository variantRepository,
                           VariantAttributeValueRepository variantAttributeValueRepository,
                           BrandRepository brandRepository, CategoryRepository categoryRepository,
                           ProductAttributeRepository productAttributeRepository,
                           AttributeValueRepository attributeValueOptionRepository,
                           WarehouseRepository warehouseRepository, InventoryRepository inventoryRepository,
                           InventoryTransactionRepository inventoryTransactionRepository,
                           ProductTypeRepository productTypeRepository, AttributeSetRepository attributeSetRepository,
                           AttributeSetAttributeRepository attributeSetAttributeRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.variantRepository = variantRepository;
        this.variantAttributeValueRepository = variantAttributeValueRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.attributeValueOptionRepository = attributeValueOptionRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productTypeRepository = productTypeRepository;
        this.attributeSetRepository = attributeSetRepository;
        this.attributeSetAttributeRepository = attributeSetAttributeRepository;
    }

    // ---------- Public listing ----------

    public PageData<ProductSummaryResponse> listPublic(UUID categoryId, String search, int page, int size) {
        Pageable pageable = toPageable(page, size);
        boolean hasCategory = categoryId != null;
        boolean hasSearch = search != null && !search.isBlank();

        Page<Product> result;
        if (hasCategory && hasSearch) {
            result = productRepository.findPublicByCategoryIdAndSearch(categoryId, search, pageable);
        } else if (hasCategory) {
            result = productRepository.findPublicByCategoryId(categoryId, pageable);
        } else if (hasSearch) {
            result = productRepository.findByStatusAndDeletedAtIsNullAndNameContainingIgnoreCase(
                    ProductStatus.ACTIVE, search, pageable);
        } else {
            result = productRepository.findByStatusAndDeletedAtIsNull(ProductStatus.ACTIVE, pageable);
        }
        return toSummaryPage(result);
    }

    public PageData<ProductSummaryResponse> listAdmin(String search, int page, int size) {
        Pageable pageable = toPageable(page, size);
        Page<Product> result = (search == null || search.isBlank())
                ? productRepository.findByDeletedAtIsNull(pageable)
                : productRepository.findByDeletedAtIsNullAndNameContainingIgnoreCase(search, pageable);
        return toSummaryPage(result);
    }

    private PageData<ProductSummaryResponse> toSummaryPage(Page<Product> result) {
        List<Product> products = result.getContent();
        Map<UUID, Brand> brandsById = fetchBrandsFor(products);
        return PageData.from(result, product -> ProductSummaryResponse.from(
                product,
                product.getBrandId() != null && brandsById.containsKey(product.getBrandId())
                        ? brandsById.get(product.getBrandId()).getName() : null,
                productCategoryRepository.findPrimaryCategoryNameByProductId(product.getId())
        ));
    }

    // ---------- Detail ----------

    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getSlug().equals(slug) && p.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return toDetail(product);
    }

    public ProductDetailResponse getById(UUID id) {
        Product product = findOrThrow(id);
        return toDetail(product);
    }

    private ProductDetailResponse toDetail(Product product) {
        Brand brand = product.getBrandId() == null ? null : brandRepository.findById(product.getBrandId()).orElse(null);
        ProductType productType = product.getProductTypeId() == null ? null
                : productTypeRepository.findById(product.getProductTypeId()).orElse(null);
        AttributeSet attributeSet = product.getAttributeSetId() == null ? null
                : attributeSetRepository.findById(product.getAttributeSetId()).orElse(null);

        List<ProductCategory> productCategories = productCategoryRepository.findByProductId(product.getId());
        Map<UUID, Category> categoriesById = categoryRepository
                .findAllById(productCategories.stream().map(ProductCategory::getCategoryId).toList())
                .stream().collect(Collectors.toMap(Category::getId, c -> c));
        List<CategoryResponse> categories = productCategories.stream()
                .map(pc -> categoriesById.get(pc.getCategoryId()))
                .filter(Objects::nonNull)
                .map(CategoryResponse::from)
                .toList();

        List<AttributeSetAttribute> setAttrs = product.getAttributeSetId() == null ? List.of()
                : attributeSetAttributeRepository.findByAttributeSetId(product.getAttributeSetId(), Sort.by("sortOrder"));
        Map<UUID, ProductAttribute> attributesById = productAttributeRepository
                .findAllById(setAttrs.stream().map(AttributeSetAttribute::getAttributeId).toList())
                .stream().collect(Collectors.toMap(ProductAttribute::getId, a -> a));
        List<ProductAttributeValue> attrValues = attributeValueRepository.findByProductId(product.getId());
        Map<UUID, String> enteredValueText = attrValues.stream()
                .collect(Collectors.toMap(ProductAttributeValue::getAttributeId, ProductAttributeValue::getDisplayValue, (a, b) -> a));

        List<ProductAttributeConfigResponse> attributeConfigs = setAttrs.stream().map(cfg -> {
            ProductAttribute attr = attributesById.get(cfg.getAttributeId());
            if (attr == null) {
                return null;
            }
            List<AttributeValueResponse> values = cfg.isVariantAttribute()
                    ? attributeValueOptionRepository.findByAttributeId(attr.getId()).stream().map(AttributeValueResponse::from).toList()
                    : List.of();
            return new ProductAttributeConfigResponse(
                    attr.getId(), attr.getCode(), attr.getName(), attr.getInputType().name(),
                    cfg.isVariantAttribute(), cfg.isVisible(), values, enteredValueText.get(attr.getId())
            );
        }).filter(Objects::nonNull).toList();

        List<ProductVariant> variants = variantRepository.findByProductIdAndDeletedAtIsNullOrderBySortOrder(product.getId());
        List<UUID> variantIds = variants.stream().map(ProductVariant::getId).toList();
        List<VariantAttributeValue> allVav = variantAttributeValueRepository.findByVariantIdIn(variantIds);
        Map<UUID, List<VariantAttributeValue>> vavByVariant = allVav.stream()
                .collect(Collectors.groupingBy(VariantAttributeValue::getVariantId));
        Map<UUID, AttributeValue> allValuesById = attributeValueOptionRepository.findAllById(
                allVav.stream().map(VariantAttributeValue::getAttributeValueId).distinct().toList()
        ).stream().collect(Collectors.toMap(AttributeValue::getId, v -> v));
        Warehouse defaultWarehouse = warehouseRepository.findByIsDefaultTrue().orElse(null);
        Map<UUID, Inventory> inventoryByVariant = defaultWarehouse == null ? Map.of()
                : inventoryRepository.findByVariantIdIn(variantIds).stream()
                        .filter(inv -> inv.getWarehouseId().equals(defaultWarehouse.getId()))
                        .collect(Collectors.toMap(Inventory::getVariantId, i -> i));

        List<ProductVariantResponse> variantResponses = variants.stream().map(v -> {
            List<VariantAttributeValueResponse> avs = vavByVariant.getOrDefault(v.getId(), List.of()).stream()
                    .map(vav -> {
                        AttributeValue val = allValuesById.get(vav.getAttributeValueId());
                        ProductAttribute attr = attributesById.get(vav.getAttributeId());
                        return new VariantAttributeValueResponse(
                                vav.getAttributeId(), attr != null ? attr.getCode() : null, attr != null ? attr.getName() : null,
                                vav.getAttributeValueId(), val != null ? val.getValue() : null,
                                val != null ? val.getDisplayValue() : null, val != null ? val.getColorCode() : null
                        );
                    }).toList();
            Inventory inv = inventoryByVariant.get(v.getId());
            Integer available = inv == null ? 0 : inv.getQuantityOnHand() - inv.getQuantityReserved();
            return new ProductVariantResponse(
                    v.getId(), v.getSku(), v.getBarcode(), v.getName(), v.isDefault(), v.getStatus().name(),
                    v.getListPrice(), v.getSalePrice(), avs, v.getWeight(), v.getWeightUnit(),
                    v.getLength(), v.getWidth(), v.getHeight(), v.getDimensionUnit(), available, product.getThumbnailUrl()
            );
        }).toList();

        return new ProductDetailResponse(
                product.getId(), product.getCode(), product.getName(), product.getSlug(),
                product.getStructureType().name(),
                product.getProductTypeId(), productType != null ? productType.getName() : null,
                product.getAttributeSetId(), attributeSet != null ? attributeSet.getName() : null,
                product.getAttributeSetVersion(),
                product.getBrandId(), brand != null ? brand.getName() : null, categories,
                product.getShortDescription(), product.getDescription(), product.getStatus().name(),
                product.getMinPrice(), product.getMaxPrice(), product.getThumbnailUrl(),
                product.getSeoTitle(), product.getSeoDescription(), product.getSeoKeywords(), product.getCanonicalUrl(),
                product.isFeatured(), List.of(), attributeConfigs, variantResponses,
                product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    // ---------- Variant combination preview (khong ghi DB) ----------

    public List<VariantCombinationResponse> generatePreview(VariantGeneratePreviewRequest request) {
        Map<UUID, ProductAttribute> attributesById = productAttributeRepository
                .findAllById(request.attributeGroups().stream().map(AttributeValueGroup::attributeId).toList())
                .stream().collect(Collectors.toMap(ProductAttribute::getId, a -> a));

        List<List<VariantAttributeValueResponse>> perAttributeOptions = new ArrayList<>();
        for (AttributeValueGroup group : request.attributeGroups()) {
            ProductAttribute attribute = attributesById.get(group.attributeId());
            if (attribute == null) {
                throw new BusinessException(ErrorCode.ATTRIBUTE_NOT_FOUND);
            }
            if (!attribute.isAllowVariant()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Thuộc tính " + attribute.getName() + " không được phép tạo variant");
            }
            List<AttributeValue> values = attributeValueOptionRepository.findAllById(group.valueIds());
            if (values.size() != group.valueIds().size()) {
                throw new BusinessException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
            }
            perAttributeOptions.add(values.stream()
                    .map(v -> new VariantAttributeValueResponse(attribute.getId(), attribute.getCode(), attribute.getName(),
                            v.getId(), v.getValue(), v.getDisplayValue(), v.getColorCode()))
                    .toList());
        }

        List<List<VariantAttributeValueResponse>> combinations = cartesianProduct(perAttributeOptions);
        if (combinations.size() > MAX_VARIANTS_PER_PRODUCT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Vượt giới hạn tối đa " + MAX_VARIANTS_PER_PRODUCT + " variant/sản phẩm");
        }
        return combinations.stream()
                .map(combo -> new VariantCombinationResponse(buildSignature(combo), buildSuggestedName(combo), combo))
                .toList();
    }

    // ---------- Create ----------

    @Transactional
    public ProductDetailResponse create(UUID actorId, ProductCreateRequest request) {
        if (productRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã sản phẩm đã tồn tại");
        }
        if (productRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Slug sản phẩm đã tồn tại");
        }
        ProductStructureType structureType = parseStructureType(request.structureTypeOrDefault());
        Brand brand = resolveBrand(request.brandId());
        resolveCategories(request.categoryIds());

        ProductType productType = productTypeRepository.findByIdAndDeletedAtIsNull(request.productTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy loại sản phẩm"));
        if (productType.getDefaultAttributeSetId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Loại sản phẩm chưa gán bộ thuộc tính mặc định");
        }
        AttributeSet attributeSet = attributeSetRepository.findByIdAndDeletedAtIsNull(productType.getDefaultAttributeSetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bộ thuộc tính"));

        List<AttributeSetAttribute> setAttrs = attributeSetAttributeRepository.findByAttributeSetId(attributeSet.getId(), Sort.unsorted());
        Map<UUID, AttributeSetAttribute> setAttrsByAttributeId = setAttrs.stream()
                .collect(Collectors.toMap(AttributeSetAttribute::getAttributeId, a -> a));
        Map<UUID, ProductAttribute> attributesById = productAttributeRepository
                .findAllById(setAttrsByAttributeId.keySet()).stream()
                .collect(Collectors.toMap(ProductAttribute::getId, a -> a));

        if (request.variants().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Sản phẩm phải có ít nhất 1 variant");
        }
        if (request.variants().size() > MAX_VARIANTS_PER_PRODUCT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Vượt giới hạn tối đa " + MAX_VARIANTS_PER_PRODUCT + " variant/sản phẩm");
        }

        validateVariantSkuUniqueWithinRequest(request.variants());
        validateVariantSkuNotExisting(request.variants());
        Map<UUID, AttributeValue> allowedValuesById = validateVariantAttributeValues(request, setAttrsByAttributeId);
        validateVariantSignaturesUnique(request.variants(), attributesById, allowedValuesById);
        validatePrices(request.variants());
        validateAttributeValuesAgainstSchema(request.attributeValues(), setAttrsByAttributeId, attributesById);

        ProductStatus status = request.status() == null || request.status().isBlank()
                ? ProductStatus.DRAFT : parseProductStatus(request.status());
        ProductSeoRequest seo = request.seo();

        Product product = Product.builder()
                .code(request.code())
                .name(request.name())
                .slug(request.slug())
                .structureType(structureType)
                .productTypeId(productType.getId())
                .attributeSetId(attributeSet.getId())
                .attributeSetVersion(attributeSet.getVersionNo())
                .brandId(brand != null ? brand.getId() : null)
                .shortDescription(request.shortDescription())
                .description(request.description())
                .status(status)
                .seoTitle(seo != null ? seo.title() : null)
                .seoDescription(seo != null ? seo.description() : null)
                .seoKeywords(seo != null ? seo.keywords() : null)
                .canonicalUrl(seo != null ? seo.canonicalUrl() : null)
                .featured(request.featured())
                .build();
        product = productRepository.save(product);

        saveProductCategories(product.getId(), request.categoryIds(), request.primaryCategoryId());
        saveProductAttributeValues(product.getId(), request.attributeValues());
        BigDecimal[] minMax = saveVariants(product.getId(), request.variants(), attributesById, allowedValuesById);

        product.setMinPrice(minMax[0]);
        product.setMaxPrice(minMax[1]);
        product = productRepository.save(product);

        return toDetail(product);
    }

    @Transactional
    public ProductDetailResponse update(UUID id, ProductUpdateRequest request) {
        Product product = findOrThrow(id);
        Brand brand = resolveBrand(request.brandId());
        resolveCategories(request.categoryIds());

        product.setName(request.name());
        product.setSlug(request.slug());
        product.setBrandId(brand != null ? brand.getId() : null);
        product.setShortDescription(request.shortDescription());
        product.setDescription(request.description());
        if (request.status() != null && !request.status().isBlank()) {
            product.setStatus(parseProductStatus(request.status()));
        }
        product.setFeatured(request.featured());
        if (request.seo() != null) {
            product.setSeoTitle(request.seo().title());
            product.setSeoDescription(request.seo().description());
            product.setSeoKeywords(request.seo().keywords());
            product.setCanonicalUrl(request.seo().canonicalUrl());
        }
        product = productRepository.save(product);

        saveProductCategories(product.getId(), request.categoryIds(), request.primaryCategoryId());

        if (request.attributeValues() != null && product.getAttributeSetId() != null) {
            List<AttributeSetAttribute> setAttrs = attributeSetAttributeRepository
                    .findByAttributeSetId(product.getAttributeSetId(), Sort.unsorted());
            Map<UUID, AttributeSetAttribute> setAttrsByAttributeId = setAttrs.stream()
                    .collect(Collectors.toMap(AttributeSetAttribute::getAttributeId, a -> a));
            Map<UUID, ProductAttribute> attributesById = productAttributeRepository
                    .findAllById(setAttrsByAttributeId.keySet()).stream()
                    .collect(Collectors.toMap(ProductAttribute::getId, a -> a));
            validateAttributeValuesAgainstSchema(request.attributeValues(), setAttrsByAttributeId, attributesById);
            saveProductAttributeValues(product.getId(), request.attributeValues());
        }

        return toDetail(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = findOrThrow(id);
        product.setDeletedAt(Instant.now());
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
    }

    // ---------- Helpers: validation ----------

    private Brand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId).orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    private List<Category> resolveCategories(List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != new HashSet<>(categoryIds).size()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categories;
    }

    private void validateVariantSkuUniqueWithinRequest(List<ProductVariantRequest> variants) {
        Set<String> seen = new HashSet<>();
        for (ProductVariantRequest v : variants) {
            if (!seen.add(v.sku())) {
                throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS, "SKU \"" + v.sku() + "\" bị lặp lại trong cùng request");
            }
        }
    }

    private void validateVariantSkuNotExisting(List<ProductVariantRequest> variants) {
        for (ProductVariantRequest v : variants) {
            if (variantRepository.existsBySku(v.sku())) {
                throw new BusinessException(ErrorCode.SKU_ALREADY_EXISTS, "SKU \"" + v.sku() + "\" đã tồn tại");
            }
        }
    }

    /** Attribute dung cho variant phai duoc danh dau is_variant_attribute=true trong Attribute Set dang gan cho san pham (khong con check tu do nhu truoc). */
    private Map<UUID, AttributeValue> validateVariantAttributeValues(ProductCreateRequest request,
                                                                       Map<UUID, AttributeSetAttribute> setAttrsByAttributeId) {
        Set<UUID> allValueIds = request.variants().stream()
                .flatMap(v -> v.attributeValueIds() == null ? List.<UUID>of().stream() : v.attributeValueIds().stream())
                .collect(Collectors.toSet());
        if (allValueIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AttributeValue> valuesById = attributeValueOptionRepository.findAllById(allValueIds).stream()
                .collect(Collectors.toMap(AttributeValue::getId, v -> v));
        if (valuesById.size() != allValueIds.size()) {
            throw new BusinessException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
        }
        for (ProductVariantRequest v : request.variants()) {
            if (v.attributeValueIds() == null) {
                continue;
            }
            Set<UUID> attributeIdsUsed = new HashSet<>();
            for (UUID valueId : v.attributeValueIds()) {
                AttributeValue value = valuesById.get(valueId);
                AttributeSetAttribute setAttr = setAttrsByAttributeId.get(value.getAttributeId());
                if (setAttr == null || !setAttr.isVariantAttribute()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Thuộc tính không thuộc nhóm tạo variant của bộ thuộc tính này");
                }
                if (!attributeIdsUsed.add(value.getAttributeId())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Variant \"" + v.name() + "\" có 2 giá trị cùng 1 thuộc tính");
                }
            }
        }
        return valuesById;
    }

    private void validateVariantSignaturesUnique(List<ProductVariantRequest> variants,
                                                  Map<UUID, ProductAttribute> attributesById,
                                                  Map<UUID, AttributeValue> valuesById) {
        Set<String> signatures = new HashSet<>();
        for (ProductVariantRequest v : variants) {
            String signature = buildSignatureFromIds(v.attributeValueIds(), attributesById, valuesById);
            if (!signatures.add(signature)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Variant \"" + v.name() + "\" bị trùng tổ hợp thuộc tính với variant khác");
            }
        }
    }

    private void validatePrices(List<ProductVariantRequest> variants) {
        for (ProductVariantRequest v : variants) {
            if (v.salePrice().compareTo(v.listPrice()) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Variant \"" + v.name() + "\": giá bán không được lớn hơn giá niêm yết");
            }
        }
    }

    /** Rule #11 (khong cho nhap attribute ngoai schema) + rule bat buoc phai co gia tri neu is_required va khong phai variant attribute. */
    private void validateAttributeValuesAgainstSchema(List<ProductAttributeValueInput> inputs,
                                                        Map<UUID, AttributeSetAttribute> setAttrsByAttributeId,
                                                        Map<UUID, ProductAttribute> attributesById) {
        List<ProductAttributeValueInput> safeInputs = inputs == null ? List.of() : inputs;
        Map<UUID, ProductAttributeValueInput> inputsByAttributeId = safeInputs.stream()
                .collect(Collectors.toMap(ProductAttributeValueInput::attributeId, i -> i, (a, b) -> a));

        for (ProductAttributeValueInput input : safeInputs) {
            if (!setAttrsByAttributeId.containsKey(input.attributeId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Thuộc tính không nằm trong bộ thuộc tính của sản phẩm này");
            }
        }

        for (AttributeSetAttribute cfg : setAttrsByAttributeId.values()) {
            if (cfg.isVariantAttribute() || !cfg.isRequired()) {
                continue;
            }
            ProductAttributeValueInput input = inputsByAttributeId.get(cfg.getAttributeId());
            boolean hasValue = input != null && (input.attributeValueId() != null || input.valueText() != null
                    || input.valueNumber() != null || input.valueBoolean() != null || input.valueDate() != null);
            if (!hasValue) {
                ProductAttribute attr = attributesById.get(cfg.getAttributeId());
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Thuộc tính \"" + (attr != null ? attr.getName() : cfg.getAttributeId()) + "\" là bắt buộc");
            }
        }
    }

    // ---------- Helpers: persist ----------

    /** Thay toan bo lien ket category cua product - xoa lien ket cu truoc de tranh vi pham unique constraint khi update (VD giu nguyen categoryIds cu). */
    private void saveProductCategories(UUID productId, List<UUID> categoryIds, UUID primaryCategoryId) {
        productCategoryRepository.deleteByProductId(productId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        UUID primary = primaryCategoryId != null && categoryIds.contains(primaryCategoryId)
                ? primaryCategoryId : categoryIds.get(0);
        List<ProductCategory> links = categoryIds.stream()
                .map(catId -> ProductCategory.builder()
                        .productId(productId)
                        .categoryId(catId)
                        .primary(catId.equals(primary))
                        .build())
                .toList();
        productCategoryRepository.saveAll(links);
    }

    private void saveProductAttributeValues(UUID productId, List<ProductAttributeValueInput> inputs) {
        attributeValueRepository.deleteByProductId(productId);
        if (inputs == null) {
            return;
        }
        for (ProductAttributeValueInput input : inputs) {
            String displayValue = input.valueText();
            if (displayValue == null && input.attributeValueId() != null) {
                displayValue = attributeValueOptionRepository.findById(input.attributeValueId())
                        .map(AttributeValue::getDisplayValue).orElse(null);
            }
            if (displayValue == null && input.valueNumber() != null) {
                displayValue = input.valueNumber().toString();
            }
            if (displayValue == null && input.valueBoolean() != null) {
                displayValue = input.valueBoolean().toString();
            }
            if (displayValue == null && input.valueDate() != null) {
                displayValue = input.valueDate().toString();
            }
            attributeValueRepository.save(ProductAttributeValue.builder()
                    .productId(productId)
                    .attributeId(input.attributeId())
                    .attributeValueId(input.attributeValueId())
                    .valueText(input.valueText())
                    .valueNumber(input.valueNumber())
                    .valueBoolean(input.valueBoolean())
                    .valueDate(input.valueDate())
                    .displayValue(displayValue)
                    .build());
        }
    }

    private BigDecimal[] saveVariants(UUID productId, List<ProductVariantRequest> variants,
                                       Map<UUID, ProductAttribute> attributesById, Map<UUID, AttributeValue> valuesById) {
        Warehouse defaultWarehouse = warehouseRepository.findByIsDefaultTrue().orElse(null);
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (int i = 0; i < variants.size(); i++) {
            ProductVariantRequest req = variants.get(i);
            ProductVariant variant = ProductVariant.builder()
                    .productId(productId)
                    .sku(req.sku())
                    .barcode(req.barcode())
                    .name(req.name())
                    .variantSignature(buildSignatureFromIds(req.attributeValueIds(), attributesById, valuesById))
                    .isDefault(i == 0)
                    .listPrice(req.listPrice())
                    .salePrice(req.salePrice())
                    .costPrice(req.costPrice())
                    .weight(req.weight())
                    .weightUnit(req.weightUnit())
                    .length(req.length())
                    .width(req.width())
                    .height(req.height())
                    .dimensionUnit(req.dimensionUnit())
                    .sortOrder(i)
                    .build();
            variant = variantRepository.save(variant);

            if (req.attributeValueIds() != null) {
                for (UUID valueId : req.attributeValueIds()) {
                    AttributeValue value = valuesById.get(valueId);
                    variantAttributeValueRepository.save(VariantAttributeValue.builder()
                            .variantId(variant.getId())
                            .attributeId(value.getAttributeId())
                            .attributeValueId(valueId)
                            .build());
                }
            }

            if (defaultWarehouse != null) {
                int initialQty = req.initialStocks() == null ? 0 : req.initialStocks().stream()
                        .filter(s -> s.warehouseId() == null || s.warehouseId().equals(defaultWarehouse.getId()))
                        .mapToInt(VariantInitialStockRequest::quantity).sum();
                Inventory inventory = Inventory.builder()
                        .warehouseId(defaultWarehouse.getId())
                        .variantId(variant.getId())
                        .quantityOnHand(initialQty)
                        .build();
                inventoryRepository.save(inventory);

                if (initialQty > 0) {
                    inventoryTransactionRepository.save(InventoryTransaction.builder()
                            .warehouseId(defaultWarehouse.getId())
                            .variantId(variant.getId())
                            .transactionType(InventoryTransactionType.IMPORT)
                            .quantity(initialQty)
                            .quantityBefore(0)
                            .quantityAfter(initialQty)
                            .referenceType("PRODUCT_CREATE")
                            .build());
                }
            }

            minPrice = minPrice == null ? req.salePrice() : minPrice.min(req.salePrice());
            maxPrice = maxPrice == null ? req.salePrice() : maxPrice.max(req.salePrice());
        }
        return new BigDecimal[]{minPrice, maxPrice};
    }

    // ---------- Small helpers ----------

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Map<UUID, Brand> fetchBrandsFor(List<Product> products) {
        Set<UUID> ids = products.stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return brandRepository.findAllById(ids).stream().collect(Collectors.toMap(Brand::getId, b -> b));
    }

    private Pageable toPageable(int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ProductStructureType parseStructureType(String value) {
        try {
            return ProductStructureType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "structureType không hợp lệ");
        }
    }

    private ProductStatus parseProductStatus(String value) {
        try {
            return ProductStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status không hợp lệ");
        }
    }

    private String buildSignatureFromIds(List<UUID> attributeValueIds, Map<UUID, ProductAttribute> attributesById,
                                          Map<UUID, AttributeValue> valuesById) {
        if (attributeValueIds == null || attributeValueIds.isEmpty()) {
            return "";
        }
        return attributeValueIds.stream()
                .map(valuesById::get)
                .filter(Objects::nonNull)
                .map(v -> {
                    ProductAttribute attr = attributesById.get(v.getAttributeId());
                    String code = attr != null ? attr.getCode() : v.getAttributeId().toString();
                    return code + "=" + v.getCode();
                })
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String buildSignature(List<VariantAttributeValueResponse> combo) {
        return combo.stream()
                .map(v -> v.attributeCode() + "=" + v.value())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String buildSuggestedName(List<VariantAttributeValueResponse> combo) {
        return combo.stream()
                .map(v -> v.displayValue() != null ? v.displayValue() : v.value())
                .collect(Collectors.joining(" / "));
    }

    private List<List<VariantAttributeValueResponse>> cartesianProduct(List<List<VariantAttributeValueResponse>> lists) {
        List<List<VariantAttributeValueResponse>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<VariantAttributeValueResponse> list : lists) {
            List<List<VariantAttributeValueResponse>> next = new ArrayList<>();
            for (List<VariantAttributeValueResponse> partial : result) {
                for (VariantAttributeValueResponse option : list) {
                    List<VariantAttributeValueResponse> combo = new ArrayList<>(partial);
                    combo.add(option);
                    next.add(combo);
                }
            }
            result = next;
        }
        return result;
    }
}
