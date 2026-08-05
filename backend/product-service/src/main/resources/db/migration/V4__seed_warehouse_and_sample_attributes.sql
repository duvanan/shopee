-- Warehouse mac dinh (giai doan dau chi 1 kho, xem
-- docs/features/03-dynamic-product-catalog.md muc 7) va vai attribute mau
-- de co du lieu test tao san pham dong ngay.

INSERT INTO warehouses (code, name, is_default)
VALUES ('MAIN', 'Kho mac dinh', TRUE);

INSERT INTO product_attributes (id, code, name, data_type, input_type, allow_variant, is_filterable, is_searchable, sort_order)
VALUES
    (gen_random_uuid(), 'COLOR', 'Mau sac', 'STRING', 'COLOR', TRUE, TRUE, FALSE, 1),
    (gen_random_uuid(), 'SIZE', 'Kich thuoc', 'STRING', 'SELECT', TRUE, TRUE, FALSE, 2),
    (gen_random_uuid(), 'MATERIAL', 'Chat lieu', 'STRING', 'TEXT', FALSE, FALSE, TRUE, 3);

INSERT INTO attribute_values (attribute_id, code, value, display_value, color_code, sort_order)
SELECT id, 'BLACK', 'BLACK', 'Den', '#000000', 1 FROM product_attributes WHERE code = 'COLOR'
UNION ALL
SELECT id, 'WHITE', 'WHITE', 'Trang', '#FFFFFF', 2 FROM product_attributes WHERE code = 'COLOR';

INSERT INTO attribute_values (attribute_id, code, value, display_value, sort_order)
SELECT id, 'M', 'M', 'M', 1 FROM product_attributes WHERE code = 'SIZE'
UNION ALL
SELECT id, 'L', 'L', 'L', 2 FROM product_attributes WHERE code = 'SIZE';
