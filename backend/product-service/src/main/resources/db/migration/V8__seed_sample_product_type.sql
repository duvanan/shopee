-- Du lieu mau de test luong Product Type -> Attribute Set ngay (tai su dung
-- attribute COLOR/SIZE da seed o V4).

INSERT INTO attribute_sets (id, code, name, description)
VALUES ('11111111-1111-1111-1111-111111111111', 'SMARTPHONE_DEFAULT', 'Bo thuoc tinh Smartphone', 'Mau cho dien thoai thong minh');

INSERT INTO attribute_set_groups (id, attribute_set_id, code, name, sort_order)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'VARIANT', 'Bien the', 1);

INSERT INTO attribute_set_attributes (attribute_set_id, group_id, attribute_id, is_required, is_variant_attribute, is_filterable, sort_order)
SELECT '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', id, TRUE, TRUE, TRUE, 1
FROM product_attributes WHERE code = 'COLOR';

INSERT INTO attribute_set_attributes (attribute_set_id, group_id, attribute_id, is_required, is_variant_attribute, is_filterable, sort_order)
SELECT '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', id, FALSE, TRUE, TRUE, 2
FROM product_attributes WHERE code = 'SIZE';

INSERT INTO product_types (id, code, name, description, default_attribute_set_id)
VALUES ('33333333-3333-3333-3333-333333333333', 'SMARTPHONE', 'Dien thoai thong minh', 'Cac dong dien thoai thong minh', '11111111-1111-1111-1111-111111111111');
