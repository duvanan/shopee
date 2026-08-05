// Khop voi backend product-service sau khi nang cap model dong (xem
// docs/features/03-dynamic-product-catalog.md).

export interface CategoryResponse {
  id: string;
  parentId: string | null;
  code: string;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  level: number;
  path: string | null;
  sortOrder: number;
  status: string;
}

export interface CategoryPayload {
  parentId?: string | null;
  code: string;
  name: string;
  slug: string;
  description?: string | null;
  imageUrl?: string | null;
  sortOrder?: number;
  seoTitle?: string | null;
  seoDescription?: string | null;
}

export interface BrandResponse {
  id: string;
  code: string;
  name: string;
  slug: string;
  logoUrl: string | null;
  bannerUrl: string | null;
  description: string | null;
  websiteUrl: string | null;
  countryCode: string | null;
  status: string;
  sortOrder: number;
  seoTitle: string | null;
  seoDescription: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BrandPayload {
  code: string;
  name: string;
  slug: string;
  logoUrl?: string | null;
  bannerUrl?: string | null;
  description?: string | null;
  websiteUrl?: string | null;
  countryCode?: string | null;
  sortOrder?: number;
  seoTitle?: string | null;
  seoDescription?: string | null;
}

export interface ProductAttributeResponse {
  id: string;
  code: string;
  name: string;
  dataType: string;
  inputType: string;
  unit: string | null;
  attributeGroup: string | null;
  isFilterable: boolean;
  isSearchable: boolean;
  isComparable: boolean;
  allowVariant: boolean;
  status: string;
  sortOrder: number;
}

export interface ProductAttributePayload {
  code: string;
  name: string;
  dataType: string;
  inputType: string;
  unit?: string | null;
  attributeGroup?: string | null;
  isFilterable?: boolean;
  isSearchable?: boolean;
  isComparable?: boolean;
  allowVariant?: boolean;
  sortOrder?: number;
}

export interface AttributeValueResponse {
  id: string;
  attributeId: string;
  code: string;
  value: string;
  displayValue: string | null;
  colorCode: string | null;
  imageUrl: string | null;
  sortOrder: number;
  status: string;
}

export interface AttributeValuePayload {
  code: string;
  value: string;
  displayValue?: string | null;
  colorCode?: string | null;
  imageUrl?: string | null;
  sortOrder?: number;
}

export interface VariantAttributeValueResponse {
  attributeId: string;
  attributeCode: string | null;
  attributeName: string | null;
  valueId: string | null;
  value: string | null;
  displayValue: string | null;
  colorCode: string | null;
}

export interface ProductVariantResponse {
  id: string;
  sku: string;
  barcode: string | null;
  name: string;
  isDefault: boolean;
  status: string;
  listPrice: number;
  salePrice: number;
  attributeValues: VariantAttributeValueResponse[];
  weight: number | null;
  weightUnit: string | null;
  length: number | null;
  width: number | null;
  height: number | null;
  dimensionUnit: string | null;
  quantityAvailable: number;
  imageUrl: string | null;
}

export interface ProductAttributeConfigResponse {
  attributeId: string;
  code: string;
  name: string;
  inputType: string;
  isVariantAttribute: boolean;
  isVisible: boolean;
  values: AttributeValueResponse[];
  valueText: string | null;
}

export interface ProductSummaryResponse {
  id: string;
  code: string;
  name: string;
  slug: string;
  thumbnailUrl: string | null;
  minPrice: number | null;
  maxPrice: number | null;
  brandName: string | null;
  primaryCategoryName: string | null;
  status: string;
  isFeatured: boolean;
}

export interface ProductDetailResponse {
  id: string;
  code: string;
  name: string;
  slug: string;
  structureType: "SIMPLE" | "VARIABLE";
  productTypeId: string | null;
  productTypeName: string | null;
  attributeSetId: string | null;
  attributeSetName: string | null;
  attributeSetVersion: number | null;
  brandId: string | null;
  brandName: string | null;
  categories: CategoryResponse[];
  shortDescription: string | null;
  description: string | null;
  status: string;
  minPrice: number | null;
  maxPrice: number | null;
  thumbnailUrl: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  seoKeywords: string | null;
  canonicalUrl: string | null;
  isFeatured: boolean;
  imageUrls: string[];
  attributeConfigs: ProductAttributeConfigResponse[];
  variants: ProductVariantResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductListParams {
  categoryId?: string;
  search?: string;
  /** 1-based, khop voi quy uoc PageData - xem docs/architecture/api-response-standard.md muc 3. */
  page?: number;
  size?: number;
}

/** Gia tri 1 thuoc tinh mo ta (khong phai variant) - chi dung 1 field value phu hop voi data_type cua attribute. */
export interface ProductAttributeValuePayload {
  attributeId: string;
  attributeValueId?: string | null;
  valueText?: string | null;
  valueNumber?: number | null;
  valueBoolean?: boolean | null;
  valueDate?: string | null;
}

export interface VariantInitialStockPayload {
  warehouseId?: string | null;
  quantity: number;
}

export interface ProductVariantPayload {
  sku: string;
  barcode?: string | null;
  name: string;
  attributeValueIds?: string[];
  listPrice: number;
  salePrice: number;
  costPrice?: number | null;
  weight?: number | null;
  weightUnit?: string | null;
  length?: number | null;
  width?: number | null;
  height?: number | null;
  dimensionUnit?: string | null;
  initialStocks?: VariantInitialStockPayload[];
}

export interface ProductSeoPayload {
  title?: string | null;
  description?: string | null;
  keywords?: string | null;
  canonicalUrl?: string | null;
}

export interface ProductCreatePayload {
  code: string;
  name: string;
  slug: string;
  productTypeId: string;
  structureType?: "SIMPLE" | "VARIABLE";
  brandId?: string | null;
  categoryIds: string[];
  primaryCategoryId?: string | null;
  shortDescription?: string | null;
  description?: string | null;
  status?: string;
  isFeatured?: boolean;
  seo?: ProductSeoPayload;
  attributeValues?: ProductAttributeValuePayload[];
  variants: ProductVariantPayload[];
}

export interface ProductUpdatePayload {
  name: string;
  slug: string;
  brandId?: string | null;
  categoryIds: string[];
  primaryCategoryId?: string | null;
  shortDescription?: string | null;
  description?: string | null;
  status?: string;
  isFeatured?: boolean;
  seo?: ProductSeoPayload;
  attributeValues?: ProductAttributeValuePayload[];
}

export interface AttributeValueGroup {
  attributeId: string;
  valueIds: string[];
}

export interface VariantCombinationResponse {
  signature: string;
  suggestedName: string;
  attributeValues: VariantAttributeValueResponse[];
}

export interface InventoryImportPayload {
  warehouseId?: string | null;
  quantity: number;
  note?: string | null;
}

export interface InventoryResponse {
  variantId: string;
  warehouseId: string;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  version: number;
}

// --- Product Type / Attribute Set (kien truc dong - xem
// docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md) ---

export interface ProductTypeResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  defaultAttributeSetId: string | null;
  defaultAttributeSetName: string | null;
  iconUrl: string | null;
  status: string;
  sortOrder: number;
  productCount: number;
}

export interface ProductTypePayload {
  code: string;
  name: string;
  description?: string | null;
  defaultAttributeSetId?: string | null;
  iconUrl?: string | null;
  sortOrder?: number;
}

export interface AttributeSetResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  status: string;
  versionNo: number;
  isSystem: boolean;
  groupCount: number;
  attributeCount: number;
  productTypeCount: number;
}

export interface AttributeSetPayload {
  code: string;
  name: string;
  description?: string | null;
}

export interface AttributeSchemaFieldResponse {
  attributeId: string;
  code: string;
  name: string;
  label: string;
  dataType: string;
  inputType: string;
  unit: string | null;
  required: boolean;
  variantAttribute: boolean;
  filterable: boolean;
  searchable: boolean;
  comparable: boolean;
  visible: boolean;
  editable: boolean;
  placeholder: string | null;
  helpText: string | null;
  defaultValueId: string | null;
  defaultValueText: string | null;
  values: AttributeValueResponse[];
}

export interface AttributeSchemaGroupResponse {
  id: string;
  code: string;
  name: string;
  sortOrder: number | null;
  collapsible: boolean;
  defaultExpanded: boolean;
  attributes: AttributeSchemaFieldResponse[];
}

export interface AttributeSchemaResponse {
  productTypeId: string;
  productTypeCode: string;
  productTypeName: string;
  attributeSetId: string;
  attributeSetCode: string;
  attributeSetName: string;
  attributeSetVersion: number;
  groups: AttributeSchemaGroupResponse[];
}

export interface AttributeSetSchemaAttributePayload {
  attributeId: string;
  labelOverride?: string | null;
  helpText?: string | null;
  placeholder?: string | null;
  isRequired?: boolean;
  isVariantAttribute?: boolean;
  isFilterable?: boolean;
  isSearchable?: boolean;
  isComparable?: boolean;
  isVisible?: boolean;
  isEditable?: boolean;
  defaultValueId?: string | null;
  defaultValueText?: string | null;
  sortOrder?: number;
}

export interface AttributeSetSchemaGroupPayload {
  id?: string | null;
  code: string;
  name: string;
  description?: string | null;
  isCollapsible?: boolean;
  isDefaultExpanded?: boolean;
  sortOrder?: number;
  attributes: AttributeSetSchemaAttributePayload[];
}

export interface SetAttributeSetSchemaPayload {
  groups: AttributeSetSchemaGroupPayload[];
}

export interface CloneAttributeSetPayload {
  code: string;
  name: string;
}
