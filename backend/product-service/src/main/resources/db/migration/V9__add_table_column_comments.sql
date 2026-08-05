-- Chi them COMMENT ON TABLE/COLUMN de tu tai lieu hoa schema ngay trong
-- database (xem qua \d+ ten_bang hoac cong cu ERD/GUI bat ky) - khong doi
-- cau truc/du lieu.

-- ===================== brands =====================
COMMENT ON TABLE brands IS 'Thuong hieu san pham (VD: Apple, Samsung) - 1 san pham chi thuoc ve toi da 1 brand, tham chieu qua products.brand_id.';
COMMENT ON COLUMN brands.code IS 'Ma thuong hieu, duy nhat, khong doi sau khi tao (dung trong URL/import).';
COMMENT ON COLUMN brands.slug IS 'Chuoi dung cho URL than thien (VD: /brands/apple).';
COMMENT ON COLUMN brands.logo_url IS 'URL logo hien thi o trang brand/listing.';
COMMENT ON COLUMN brands.banner_url IS 'URL anh banner o trang chi tiet brand.';
COMMENT ON COLUMN brands.website_url IS 'Website chinh thuc cua brand (hien thi tham khao, khong bat buoc).';
COMMENT ON COLUMN brands.country_code IS 'Ma quoc gia xuat xu brand (VD: US, KR, VN) - hien thi/loc, khong validate ISO.';
COMMENT ON COLUMN brands.status IS 'ACTIVE/INACTIVE - brand INACTIVE khong hien thi len san nhung san pham cu van giu lien ket.';
COMMENT ON COLUMN brands.sort_order IS 'Thu tu hien thi trong danh sach brand (so nho hien truoc).';
COMMENT ON COLUMN brands.seo_title IS 'Tieu de SEO rieng cho trang brand, ghi de tieu de mac dinh.';
COMMENT ON COLUMN brands.seo_description IS 'Meta description SEO cho trang brand.';
COMMENT ON COLUMN brands.deleted_at IS 'Soft-delete: khac NULL nghia la brand da bi xoa, van giu row de khong vo lien ket voi products cu.';

-- ===================== categories =====================
COMMENT ON TABLE categories IS 'Danh muc san pham dang cay (self-reference qua parent_id) - phuc vu dieu huong/SEO, KHONG con quyet dinh bo thuoc tinh cua san pham (viec do la cua product_types/attribute_sets, xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 3).';
COMMENT ON COLUMN categories.parent_id IS 'FK toi categories.id cua danh muc cha - NULL nghia la danh muc goc (level 0). Khong dung FK constraint/JPA relationship, tu duyet cay bang native query.';
COMMENT ON COLUMN categories.level IS 'Do sau trong cay, tinh tu 0 (goc) - luu san de truy van/hien thi khong can duyet de quy moi lan.';
COMMENT ON COLUMN categories.path IS 'Chuoi id cha noi tiep (VD: "rootId/childId") de truy van "tat ca con chau" bang LIKE thay vi recursive CTE.';
COMMENT ON COLUMN categories.sort_order IS 'Thu tu hien thi giua cac danh muc cung cap cha.';
COMMENT ON COLUMN categories.status IS 'ACTIVE/INACTIVE - danh muc INACTIVE an khoi menu nhung san pham da gan van giu lien ket.';
COMMENT ON COLUMN categories.deleted_at IS 'Soft-delete: khac NULL nghia la danh muc da bi xoa.';

-- ===================== product_types =====================
COMMENT ON TABLE product_types IS 'Loai san pham theo BAN CHAT KY THUAT (VD: SMARTPHONE, T_SHIRT) - quyet dinh bo thuoc tinh mac dinh khi tao san pham. Khac categories (categories chi phuc vu dieu huong/SEO). Xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 3 va 42.';
COMMENT ON COLUMN product_types.code IS 'Ma loai san pham, duy nhat, khong doi sau khi tao (VD: SMARTPHONE).';
COMMENT ON COLUMN product_types.default_attribute_set_id IS 'FK toi attribute_sets.id - bo thuoc tinh se duoc gan cho MOI san pham thuoc loai nay tai thoi diem tao (san pham da tao khong tu doi theo khi doi gia tri nay).';
COMMENT ON COLUMN product_types.icon_url IS 'URL icon hien thi trong man chon loai san pham luc tao san pham (admin UI).';
COMMENT ON COLUMN product_types.status IS 'ACTIVE/INACTIVE - loai INACTIVE khong con chon duoc khi tao san pham moi.';
COMMENT ON COLUMN product_types.sort_order IS 'Thu tu hien thi trong danh sach loai san pham.';
COMMENT ON COLUMN product_types.created_by IS 'ID nguoi tao (tu X-User-Id header) - khong co FK vi users nam o database rieng cua user-service.';
COMMENT ON COLUMN product_types.updated_by IS 'ID nguoi sua lan gan nhat - tuong tu created_by.';
COMMENT ON COLUMN product_types.deleted_at IS 'Soft-delete: khac NULL nghia la loai san pham da bi xoa (chi xoa duoc khi khong con san pham nao dang dung).';

-- ===================== attribute_sets =====================
COMMENT ON TABLE attribute_sets IS 'Bo thuoc tinh mau (VD: "Bo thuoc tinh Smartphone") - tap hop cac nhom (attribute_set_groups) va cau hinh thuoc tinh cu the (attribute_set_attributes) duoc gan cho 1 hay nhieu Product Type. Xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 7.';
COMMENT ON COLUMN attribute_sets.code IS 'Ma bo thuoc tinh, duy nhat, khong doi sau khi tao.';
COMMENT ON COLUMN attribute_sets.status IS 'ACTIVE/INACTIVE - bo thuoc tinh INACTIVE khong con gan duoc cho Product Type moi.';
COMMENT ON COLUMN attribute_sets.version_no IS 'Tang moi lan luu cau hinh (setSchema) - san pham da tao luu lai attribute_set_version cua minh de biet dang dung schema phien ban nao (khong tu doi theo khi Set thay doi sau).';
COMMENT ON COLUMN attribute_sets.is_system IS 'true = bo thuoc tinh he thong (khong cho xoa/sua tu do qua UI thuong) - hien tai chua co logic khoa, du phong cho tuong lai.';
COMMENT ON COLUMN attribute_sets.created_by IS 'ID nguoi tao (tu X-User-Id header do Gateway forward) - khong co FK vi bang users nam o database rieng cua user-service (moi microservice 1 database, khong join cross-service).';
COMMENT ON COLUMN attribute_sets.updated_by IS 'ID nguoi sua lan gan nhat - tuong tu created_by.';
COMMENT ON COLUMN attribute_sets.deleted_at IS 'Soft-delete: khac NULL nghia la bo thuoc tinh da bi xoa (chi xoa duoc khi khong con Product Type/san pham nao dang dung).';

-- ===================== attribute_set_groups =====================
COMMENT ON TABLE attribute_set_groups IS 'Nhom hien thi cac thuoc tinh trong 1 Attribute Set (VD: "Thong so ky thuat", "Bien the") - phuc vu gom nhom tren form nhap san pham, khong anh huong logic nghiep vu.';
COMMENT ON COLUMN attribute_set_groups.attribute_set_id IS 'FK toi attribute_sets.id - nhom thuoc ve dung 1 bo thuoc tinh.';
COMMENT ON COLUMN attribute_set_groups.code IS 'Ma nhom, duy nhat TRONG PHAM VI 1 attribute_set (khong duy nhat toan bang).';
COMMENT ON COLUMN attribute_set_groups.is_collapsible IS 'true = cho phep thu gon/mo rong nhom tren form (UI hint, khong anh huong du lieu).';
COMMENT ON COLUMN attribute_set_groups.is_default_expanded IS 'true = nhom mo san khi vao form lan dau (UI hint).';
COMMENT ON COLUMN attribute_set_groups.sort_order IS 'Thu tu hien thi cac nhom trong 1 bo thuoc tinh.';

-- ===================== attribute_set_attributes =====================
COMMENT ON TABLE attribute_set_attributes IS 'Bang quan trong nhat cua kien truc dong: xac dinh 1 Attribute Set gom NHUNG thuoc tinh nao va cau hinh cu the cho tung thuoc tinh trong set do (bat buoc/tao variant/loc/hien thi...). Day chinh la nguon sinh ra form nhap lieu dong khi tao san pham. Xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 11.';
COMMENT ON COLUMN attribute_set_attributes.attribute_set_id IS 'FK toi attribute_sets.id.';
COMMENT ON COLUMN attribute_set_attributes.group_id IS 'FK toi attribute_set_groups.id - NULL nghia la thuoc tinh khong thuoc nhom nao (hien thi rieng, "ungrouped").';
COMMENT ON COLUMN attribute_set_attributes.attribute_id IS 'FK toi product_attributes.id - thuoc tinh GOC (dinh nghia chung, dung lai duoc giua nhieu Attribute Set khac nhau).';
COMMENT ON COLUMN attribute_set_attributes.label_override IS 'Nhan hien thi rieng cho thuoc tinh TRONG set nay, ghi de len product_attributes.name mac dinh (VD: doi "Mau sac" thanh "Mau ao").';
COMMENT ON COLUMN attribute_set_attributes.help_text IS 'Doan giai thich them hien duoi o nhap tren form (VD: "Chon mau theo anh thuc te").';
COMMENT ON COLUMN attribute_set_attributes.placeholder IS 'Placeholder cho o nhap (chi ap dung voi input dang text/number).';
COMMENT ON COLUMN attribute_set_attributes.is_required IS 'true = bat buoc nhap gia tri khi tao/sua san pham thuoc set nay - chi ap dung voi thuoc tinh KHONG phai variant (is_variant_attribute=false), variant thi bat buoc qua viec phai co it nhat 1 gia tri duoc chon khi sinh to hop.';
COMMENT ON COLUMN attribute_set_attributes.is_variant_attribute IS 'true = thuoc tinh nay dung de TAO BIEN THE (VD: mau sac, kich thuoc) - cac gia tri duoc chon se dua vao variant_attribute_values cua tung ProductVariant, khac voi false thi gia tri nam o product_attribute_values (thuoc tinh mo ta chung cho ca san pham).';
COMMENT ON COLUMN attribute_set_attributes.is_filterable IS 'true = hien trong bo loc (facet) o trang danh sach san pham.';
COMMENT ON COLUMN attribute_set_attributes.is_searchable IS 'true = gia tri thuoc tinh duoc dua vao chi muc tim kiem full-text.';
COMMENT ON COLUMN attribute_set_attributes.is_comparable IS 'true = hien trong bang so sanh san pham (tinh nang so sanh, chua trien khai FE).';
COMMENT ON COLUMN attribute_set_attributes.is_visible IS 'true = hien thi cong khai o trang chi tiet san pham (false = chi dung noi bo/loc, khong hien ra ngoai).';
COMMENT ON COLUMN attribute_set_attributes.is_editable IS 'true = admin duoc sua gia tri nay sau khi san pham da tao (false = chi nhap 1 lan luc tao, VD thuoc tinh he thong).';
COMMENT ON COLUMN attribute_set_attributes.default_value_id IS 'FK toi attribute_values.id - gia tri mac dinh goi y khi tao san pham moi (chi ap dung thuoc tinh co danh sach gia tri dinh san).';
COMMENT ON COLUMN attribute_set_attributes.default_value_text IS 'Gia tri text mac dinh goi y (danh cho thuoc tinh nhap tu do, khong co danh sach gia tri dinh san).';
COMMENT ON COLUMN attribute_set_attributes.validation_override IS 'JSON quy tac validate rieng cho thuoc tinh TRONG set nay, ghi de len product_attributes.validation_rules mac dinh (VD gioi han min/max rieng).';
COMMENT ON COLUMN attribute_set_attributes.sort_order IS 'Thu tu hien thi thuoc tinh trong nhom.';

-- ===================== product_attributes =====================
COMMENT ON TABLE product_attributes IS 'Dinh nghia GOC cua 1 thuoc tinh san pham (VD: Mau sac, Chat lieu, RAM) - dung chung, duoc tham chieu/cau hinh lai boi nhieu Attribute Set khac nhau qua attribute_set_attributes. Ban than bang nay KHONG quyet dinh thuoc tinh co bat buoc/tao variant hay khong cho tung san pham cu the (viec do thuoc ve attribute_set_attributes).';
COMMENT ON COLUMN product_attributes.code IS 'Ma thuoc tinh, duy nhat, khong doi sau khi tao (VD: COLOR).';
COMMENT ON COLUMN product_attributes.data_type IS 'Kieu du lieu luu tru: STRING/INTEGER/DECIMAL/BOOLEAN/DATE/DATETIME/JSON - quyet dinh field nao trong product_attribute_values duoc dung (value_text/value_integer/value_number/...).';
COMMENT ON COLUMN product_attributes.input_type IS 'Kieu input hien thi tren form: TEXT/TEXTAREA/NUMBER/SELECT/MULTI_SELECT/RADIO/CHECKBOX/SWITCH/COLOR/DATE/DATETIME - chi anh huong UI, khong anh huong luu tru.';
COMMENT ON COLUMN product_attributes.unit IS 'Don vi tinh hien thi kem gia tri (VD: "GB", "inch", "kg").';
COMMENT ON COLUMN product_attributes.attribute_group IS 'Nhan gom nhom don gian (legacy, truoc khi co attribute_set_groups) - khong con dung cho form dong, giu lai de tuong thich nguoc.';
COMMENT ON COLUMN product_attributes.is_filterable IS 'Co the loc theo thuoc tinh nay (flag mac dinh, co the bi ghi de boi attribute_set_attributes.is_filterable trong tung set).';
COMMENT ON COLUMN product_attributes.is_searchable IS 'Co dua vao tim kiem full-text (flag mac dinh, tuong tu is_filterable).';
COMMENT ON COLUMN product_attributes.is_comparable IS 'Co dua vao bang so sanh san pham (flag mac dinh, tuong tu is_filterable).';
COMMENT ON COLUMN product_attributes.allow_variant IS 'true = thuoc tinh NAY VE NGUYEN TAC duoc phep dung de tao bien the (kiem tra tang toan cuc, doc lap voi is_variant_attribute cua tung Attribute Set - 1 thuoc tinh phai allow_variant=true THI moi duoc phep bat is_variant_attribute=true o bat ky set nao).';
COMMENT ON COLUMN product_attributes.allow_predefined_values IS 'true = admin duoc chon gia tri tu danh sach dinh san (bang attribute_values) khi nhap cho san pham.';
COMMENT ON COLUMN product_attributes.allow_custom_value IS 'true = admin duoc tu go gia tri tuy y (khong bat buoc chon tu danh sach dinh san).';
COMMENT ON COLUMN product_attributes.validation_rules IS 'JSON quy tac validate mac dinh (VD min/max, regex) - co the bi ghi de boi attribute_set_attributes.validation_override trong tung set.';
COMMENT ON COLUMN product_attributes.status IS 'ACTIVE/INACTIVE - thuoc tinh INACTIVE khong con chon duoc khi cau hinh Attribute Set moi.';
COMMENT ON COLUMN product_attributes.sort_order IS 'Thu tu hien thi mac dinh (khi khong nam trong 1 Attribute Set co sort_order rieng).';

-- ===================== attribute_values =====================
COMMENT ON TABLE attribute_values IS 'Danh sach gia tri dinh san cho 1 product_attributes (VD: thuoc tinh COLOR co cac gia tri BLACK/WHITE/RED) - dung chung cho ca thuoc tinh mo ta va thuoc tinh tao bien the.';
COMMENT ON COLUMN attribute_values.attribute_id IS 'FK toi product_attributes.id.';
COMMENT ON COLUMN attribute_values.code IS 'Ma gia tri, duy nhat TRONG PHAM VI 1 attribute_id (VD: BLACK).';
COMMENT ON COLUMN attribute_values.value IS 'Gia tri luu tru/dung de so sanh (VD: "BLACK") - khac display_value la gia tri hien thi cho nguoi dung.';
COMMENT ON COLUMN attribute_values.display_value IS 'Gia tri hien thi cho nguoi dung, co the co dau/khac voi value (VD: "Den" cho value="BLACK").';
COMMENT ON COLUMN attribute_values.color_code IS 'Ma mau hex (VD: #000000) - chi co y nghia voi thuoc tinh input_type=COLOR, dung ve swatch mau tren UI.';
COMMENT ON COLUMN attribute_values.image_url IS 'Anh minh hoa cho gia tri (VD: anh mau san pham) - dung cho input_type dang chon anh.';
COMMENT ON COLUMN attribute_values.metadata IS 'JSON du lieu mo rong tuy thuoc tinh, khong co cau truc co dinh.';
COMMENT ON COLUMN attribute_values.sort_order IS 'Thu tu hien thi cac gia tri cua cung 1 thuoc tinh.';
COMMENT ON COLUMN attribute_values.status IS 'ACTIVE/INACTIVE - gia tri INACTIVE khong con chon duoc them nhung san pham/variant da dung van giu nguyen.';

-- ===================== products =====================
COMMENT ON TABLE products IS 'San pham o muc CHA - 1 san pham co the co nhieu bien the (product_variants), moi bien the moi co SKU/gia/ton kho rieng. Gia hien thi (min_price/max_price) la GIA TRI TONG HOP tu cac variant, khong phai gia goc.';
COMMENT ON COLUMN products.code IS 'Ma san pham noi bo, duy nhat, khong doi sau khi tao.';
COMMENT ON COLUMN products.slug IS 'Chuoi dung cho URL than thien, duy nhat (VD: /products/iphone-18).';
COMMENT ON COLUMN products.product_type IS 'CAU TRUC san pham: SIMPLE (1 bien the mac dinh) hay VARIABLE (nhieu bien the theo thuoc tinh) - LUU Y: ten cot giu nguyen "product_type" vi ly do tuong thich schema, nhung entity Java anh xa field nay la "structureType" de khong nham voi khai niem Product Type (bang product_types) moi them sau nay.';
COMMENT ON COLUMN products.product_type_id IS 'FK toi product_types.id - loai san pham (VD: SMARTPHONE), quyet dinh attribute_set_id duoc gan luc tao. Khong doi duoc sau khi tao san pham.';
COMMENT ON COLUMN products.attribute_set_id IS 'FK toi attribute_sets.id - bo thuoc tinh dang ap dung cho san pham nay, duoc chot 1 LAN DUY NHAT luc tao (lay tu product_types.default_attribute_set_id tai thoi diem do), khong tu doi khi Attribute Set thay doi sau nay.';
COMMENT ON COLUMN products.attribute_set_version IS 'Snapshot gia tri attribute_sets.version_no TAI THOI DIEM TAO san pham - dung de biet san pham dang "lech" bao nhieu phien ban so voi cau hinh Attribute Set hien tai (san pham cu KHONG tu dong cap nhat schema moi).';
COMMENT ON COLUMN products.brand_id IS 'FK toi brands.id - NULL neu san pham khong gan thuong hieu.';
COMMENT ON COLUMN products.short_description IS 'Mo ta ngan hien o card/listing san pham.';
COMMENT ON COLUMN products.description IS 'Mo ta chi tiet day du, hien o trang chi tiet san pham (co the chua HTML).';
COMMENT ON COLUMN products.status IS 'DRAFT (nhap, chua ban cong khai) / ACTIVE (dang ban) / INACTIVE (ngung ban, van con trong he thong).';
COMMENT ON COLUMN products.min_price IS 'Gia thap nhat trong cac variant ACTIVE - tinh lai moi lan tao/sua variant, dung de hien thi nhanh (khong query lai variants moi lan list).';
COMMENT ON COLUMN products.max_price IS 'Gia cao nhat trong cac variant ACTIVE - tinh lai tuong tu min_price.';
COMMENT ON COLUMN products.thumbnail_url IS 'Anh dai dien hien o card/listing san pham.';
COMMENT ON COLUMN products.seo_title IS 'Tieu de SEO rieng, ghi de tieu de mac dinh o trang chi tiet.';
COMMENT ON COLUMN products.seo_description IS 'Meta description SEO cho trang chi tiet san pham.';
COMMENT ON COLUMN products.seo_keywords IS 'Danh sach tu khoa SEO, cach nhau boi dau phay.';
COMMENT ON COLUMN products.canonical_url IS 'URL canonical rieng (dung khi san pham co nhieu URL trung noi dung, tranh SEO duplicate content).';
COMMENT ON COLUMN products.is_featured IS 'true = san pham noi bat, uu tien hien thi o trang chu/khu vuc noi bat.';
COMMENT ON COLUMN products.published_at IS 'Thoi diem san pham duoc cong bo (chuyen sang ACTIVE lan dau) - du phong cho tinh nang lich xuat ban, hien chua co logic tu dong gan.';
COMMENT ON COLUMN products.new_until IS 'Thoi diem het han gan nhan "Moi" tren san pham - du phong, hien chua co logic tu dong.';
COMMENT ON COLUMN products.sales_count IS 'Tong so luong da ban (cong don tu tat ca variant) - cache de sap xep/hien thi, khong phai nguon du lieu goc.';
COMMENT ON COLUMN products.view_count IS 'Tong luot xem trang chi tiet san pham - cache dem, khong phai nguon du lieu goc.';
COMMENT ON COLUMN products.rating_average IS 'Diem danh gia trung binh (0-5) - cache tinh tu cac danh gia, chua co bang danh gia rieng nen hien luon la 0.';
COMMENT ON COLUMN products.rating_count IS 'So luong danh gia - cache dem, tuong tu rating_average.';
COMMENT ON COLUMN products.created_by IS 'ID nguoi tao (tu X-User-Id header) - khong co FK vi users nam o database rieng cua user-service.';
COMMENT ON COLUMN products.updated_by IS 'ID nguoi sua lan gan nhat - tuong tu created_by.';
COMMENT ON COLUMN products.deleted_at IS 'Soft-delete: khac NULL nghia la san pham da bi xoa (an khoi moi listing/search nhung van giu du lieu lich su don hang).';
COMMENT ON COLUMN products.version IS 'Cot @Version cua Hibernate dung optimistic locking - tu dong tang moi lan update, KHONG tu tay gan.';

-- ===================== product_categories =====================
COMMENT ON TABLE product_categories IS 'Bang lien ket nhieu-nhieu giua products va categories (1 san pham co the thuoc nhieu danh muc).';
COMMENT ON COLUMN product_categories.product_id IS 'FK toi products.id.';
COMMENT ON COLUMN product_categories.category_id IS 'FK toi categories.id.';
COMMENT ON COLUMN product_categories.is_primary IS 'true = day la danh muc CHINH cua san pham (dung khi hien thi breadcrumb/danh muc dai dien) - moi san pham chi nen co dung 1 dong is_primary=true, quy uoc o tang service khong enforce bang constraint.';
COMMENT ON COLUMN product_categories.sort_order IS 'Thu tu hien thi khi 1 danh muc co nhieu san pham (tuy chinh vi tri thu cong).';

-- ===================== product_variants =====================
COMMENT ON TABLE product_variants IS 'Bien the co the ban cua 1 san pham (VD: iPhone mau Den 128GB) - moi variant co SKU/gia/kich thuoc/ton kho RIENG. Day la don vi thuc su duoc mua/ban, khac products chi la nhom hien thi.';
COMMENT ON COLUMN product_variants.product_id IS 'FK toi products.id.';
COMMENT ON COLUMN product_variants.sku IS 'Ma SKU, duy nhat TOAN HE THONG (khong chi trong 1 san pham) - dung de tra cuu ton kho/don hang.';
COMMENT ON COLUMN product_variants.barcode IS 'Ma vach (EAN/UPC...) - tuy chon, dung cho quet kho vat ly.';
COMMENT ON COLUMN product_variants.variant_signature IS 'Chuoi chu ky duy nhat sinh tu to hop gia tri cac thuoc tinh-tao-bien-the (VD "COLOR=BLACK|SIZE=M") - dung de phat hien trung to hop khi tao variant, KHONG phai khoa chinh nhung nen la duy nhat trong pham vi 1 san pham.';
COMMENT ON COLUMN product_variants.is_default IS 'true = day la variant mac dinh duoc chon san khi khach vao trang chi tiet san pham - moi san pham nen co dung 1 variant is_default=true.';
COMMENT ON COLUMN product_variants.status IS 'ACTIVE/INACTIVE - variant INACTIVE an khoi trang mua hang nhung van giu du lieu don hang cu.';
COMMENT ON COLUMN product_variants.list_price IS 'Gia niem yet (gia goc, chua giam gia).';
COMMENT ON COLUMN product_variants.sale_price IS 'Gia ban thuc te - phai <= list_price (validate o tang service).';
COMMENT ON COLUMN product_variants.cost_price IS 'Gia von nhap hang - chi hien thi noi bo (admin/bao cao loi nhuan), khong tra ve API public.';
COMMENT ON COLUMN product_variants.sale_start_at IS 'Thoi diem bat dau ap dung sale_price - du phong cho tinh nang flash sale, hien chua co job tu dong kich hoat.';
COMMENT ON COLUMN product_variants.sale_end_at IS 'Thoi diem ket thuc ap dung sale_price - tuong tu sale_start_at.';
COMMENT ON COLUMN product_variants.weight IS 'Khoi luong variant - dung tinh phi van chuyen.';
COMMENT ON COLUMN product_variants.weight_unit IS 'Don vi khoi luong (VD: "g", "kg") di kem weight.';
COMMENT ON COLUMN product_variants.length IS 'Chieu dai dong goi - dung tinh phi van chuyen.';
COMMENT ON COLUMN product_variants.width IS 'Chieu rong dong goi.';
COMMENT ON COLUMN product_variants.height IS 'Chieu cao dong goi.';
COMMENT ON COLUMN product_variants.dimension_unit IS 'Don vi kich thuoc (VD: "cm") di kem length/width/height.';
COMMENT ON COLUMN product_variants.sort_order IS 'Thu tu hien thi cac variant trong 1 san pham (variant dau tien mac dinh la is_default).';
COMMENT ON COLUMN product_variants.deleted_at IS 'Soft-delete: khac NULL nghia la variant da bi xoa (giu lai de khong vo du lieu don hang/ton kho lich su).';
COMMENT ON COLUMN product_variants.version IS 'Cot @Version cua Hibernate dung optimistic locking, dac biet quan trong voi thao tac tru/cong ton kho dong thoi.';

-- ===================== product_attribute_values =====================
COMMENT ON TABLE product_attribute_values IS 'Gia tri THUOC TINH MO TA (khong phai thuoc tinh tao bien the) cua 1 san pham - VD "Chat lieu = Cotton" ap dung chung cho ca san pham, khac voi variant_attribute_values la gia tri rieng cua tung bien the. Chi 1 trong cac cot value_* duoc dung tuy theo data_type cua thuoc tinh.';
COMMENT ON COLUMN product_attribute_values.product_id IS 'FK toi products.id.';
COMMENT ON COLUMN product_attribute_values.attribute_id IS 'FK toi product_attributes.id.';
COMMENT ON COLUMN product_attribute_values.attribute_value_id IS 'FK toi attribute_values.id - dung khi thuoc tinh cho chon tu danh sach gia tri dinh san (allow_predefined_values=true).';
COMMENT ON COLUMN product_attribute_values.value_text IS 'Gia tri kieu chuoi - dung khi product_attributes.data_type=STRING va khong chon tu danh sach dinh san (nhap tu do).';
COMMENT ON COLUMN product_attribute_values.value_number IS 'Gia tri so thap phan - dung khi data_type=DECIMAL.';
COMMENT ON COLUMN product_attribute_values.value_integer IS 'Gia tri so nguyen - dung khi data_type=INTEGER.';
COMMENT ON COLUMN product_attribute_values.value_boolean IS 'Gia tri dung/sai - dung khi data_type=BOOLEAN.';
COMMENT ON COLUMN product_attribute_values.value_date IS 'Gia tri ngay (khong gio) - dung khi data_type=DATE.';
COMMENT ON COLUMN product_attribute_values.value_datetime IS 'Gia tri ngay-gio day du - dung khi data_type=DATETIME.';
COMMENT ON COLUMN product_attribute_values.value_json IS 'Gia tri JSON tu do - dung khi data_type=JSON, khong co cau truc co dinh.';
COMMENT ON COLUMN product_attribute_values.display_value IS 'Chuoi hien thi da duoc "render san" (VD lay tu attribute_values.display_value hoac format tu value_number) - tranh phai join/format lai moi lan hien thi.';
COMMENT ON COLUMN product_attribute_values.sort_order IS 'Thu tu hien thi khi 1 san pham co nhieu gia tri thuoc tinh mo ta.';

-- ===================== variant_attribute_values =====================
COMMENT ON TABLE variant_attribute_values IS 'Gia tri THUOC TINH TAO BIEN THE (VD Mau sac=Den, Size=M) gan cho 1 ProductVariant cu the - moi variant co 1 to hop gia tri rieng, dung de phan biet cac variant voi nhau va hien thi bo chon (VD nut chon mau) o trang chi tiet san pham.';
COMMENT ON COLUMN variant_attribute_values.variant_id IS 'FK toi product_variants.id.';
COMMENT ON COLUMN variant_attribute_values.attribute_id IS 'FK toi product_attributes.id.';
COMMENT ON COLUMN variant_attribute_values.attribute_value_id IS 'FK toi attribute_values.id - BAT BUOC phai la gia tri dinh san (khac product_attribute_values, thuoc tinh tao bien the luon chon tu danh sach, khong nhap tu do).';

-- ===================== product_images =====================
COMMENT ON TABLE product_images IS 'Anh cua san pham/bien the - 1 san pham co nhieu anh, moi anh co the gan rieng cho 1 variant (VD anh mau Den chi hien khi chon variant mau Den) hoac dung chung cho ca san pham (variant_id NULL).';
COMMENT ON COLUMN product_images.product_id IS 'FK toi products.id.';
COMMENT ON COLUMN product_images.variant_id IS 'FK toi product_variants.id - NULL nghia la anh dung chung cho ca san pham, khong rieng cho variant nao.';
COMMENT ON COLUMN product_images.file_id IS 'ID file tren he thong luu tru/CDN ben ngoai (neu co tich hop) - du phong, hien chua co service quan ly file rieng.';
COMMENT ON COLUMN product_images.image_url IS 'URL anh kich thuoc day du.';
COMMENT ON COLUMN product_images.thumbnail_url IS 'URL anh thu nho (thumbnail), dung o listing de tai nhanh hon anh goc.';
COMMENT ON COLUMN product_images.image_type IS 'Loai anh: GALLERY (anh thuong trong bo suu tap) hoac loai khac du phong mo rong (VD banner, size-chart...).';
COMMENT ON COLUMN product_images.alt_text IS 'Text thay the cho SEO/accessibility khi anh khong tai duoc.';
COMMENT ON COLUMN product_images.title IS 'Tieu de anh, hien khi hover (tooltip) tren mot so giao dien.';
COMMENT ON COLUMN product_images.is_primary IS 'true = anh dai dien chinh, hien dau tien o gallery/thumbnail - moi san pham (hoac moi variant) nen co dung 1 anh is_primary=true.';
COMMENT ON COLUMN product_images.sort_order IS 'Thu tu hien thi anh trong gallery.';
COMMENT ON COLUMN product_images.status IS 'ACTIVE/INACTIVE - anh INACTIVE bi an khoi gallery ma khong can xoa han.';

-- ===================== warehouses =====================
COMMENT ON TABLE warehouses IS 'Kho hang vat ly - ton kho (inventories) va lich su xuat/nhap (inventory_transactions) deu gan voi 1 warehouse cu the. He thong hien tai dung 1 kho mac dinh (is_default=true) cho moi thao tac ton kho tu dong.';
COMMENT ON COLUMN warehouses.code IS 'Ma kho, duy nhat, khong doi sau khi tao.';
COMMENT ON COLUMN warehouses.address IS 'Dia chi vat ly cua kho - chi de hien thi/tham khao, khong dung tinh toan van chuyen.';
COMMENT ON COLUMN warehouses.status IS 'ACTIVE/INACTIVE - kho INACTIVE khong con duoc chon lam noi nhap/xuat moi.';
COMMENT ON COLUMN warehouses.is_default IS 'true = kho mac dinh duoc he thong tu dong dung khi tao ton kho cho variant moi (chi nen co DUNG 1 kho is_default=true, khong co constraint DB enforce dieu nay).';

-- ===================== inventories =====================
COMMENT ON TABLE inventories IS 'Ton kho hien tai cua 1 variant tai 1 warehouse - moi cap (warehouse_id, variant_id) chi co dung 1 dong. So luong kha dung (available) = quantity_on_hand - quantity_reserved, TINH O TANG SERVICE, khong luu cot rieng.';
COMMENT ON COLUMN inventories.warehouse_id IS 'FK toi warehouses.id.';
COMMENT ON COLUMN inventories.variant_id IS 'FK toi product_variants.id.';
COMMENT ON COLUMN inventories.quantity_on_hand IS 'Tong so luong VAT LY dang co trong kho (bao gom ca phan da bi giu cho don hang chua hoan tat).';
COMMENT ON COLUMN inventories.quantity_reserved IS 'So luong dang bi GIU CHO (VD: da dat trong gio hang/don hang cho thanh toan) - chua tru khoi quantity_on_hand cho den khi xuat kho thuc su.';
COMMENT ON COLUMN inventories.safety_stock IS 'Nguong ton kho an toan - du phong canh bao "sap het hang", chua co job tu dong kiem tra.';
COMMENT ON COLUMN inventories.reorder_level IS 'Nguong goi y dat hang lai (nhap them hang) - du phong, tuong tu safety_stock.';
COMMENT ON COLUMN inventories.version IS 'Cot @Version cua Hibernate dung optimistic locking - BAT BUOC de tranh mat cap nhat khi nhieu request tru/cong ton kho dong thoi.';

-- ===================== inventory_transactions =====================
COMMENT ON TABLE inventory_transactions IS 'Lich su moi lan thay doi ton kho (nhap/xuat/dieu chinh) - bang append-only (khong update/delete), dung de audit va doi soat ton kho, KHONG phai nguon du lieu ton kho hien tai (nguon that su la bang inventories).';
COMMENT ON COLUMN inventory_transactions.warehouse_id IS 'FK toi warehouses.id noi xay ra giao dich.';
COMMENT ON COLUMN inventory_transactions.variant_id IS 'FK toi product_variants.id lien quan.';
COMMENT ON COLUMN inventory_transactions.transaction_type IS 'Loai giao dich (VD: IMPORT nhap kho, EXPORT xuat kho, ADJUSTMENT dieu chinh thu cong...).';
COMMENT ON COLUMN inventory_transactions.quantity IS 'So luong thay doi trong giao dich nay (co the am neu la xuat/giam).';
COMMENT ON COLUMN inventory_transactions.quantity_before IS 'quantity_on_hand TRUOC khi giao dich nay xay ra - phuc vu audit/doi soat, khong dung de tinh toan nghiep vu.';
COMMENT ON COLUMN inventory_transactions.quantity_after IS 'quantity_on_hand SAU khi giao dich nay xay ra.';
COMMENT ON COLUMN inventory_transactions.reference_type IS 'Loai doi tuong lien quan gay ra giao dich (VD: "PRODUCT_CREATE", "ORDER"...) - tham chieu mem, khong co FK constraint vi co the tro toi bang o service khac.';
COMMENT ON COLUMN inventory_transactions.reference_id IS 'ID cua doi tuong lien quan (VD id don hang) - dung kem reference_type.';
COMMENT ON COLUMN inventory_transactions.note IS 'Ghi chu tu do cua nguoi thuc hien giao dich (VD ly do dieu chinh thu cong).';
COMMENT ON COLUMN inventory_transactions.created_by IS 'ID nguoi dung thuc hien giao dich (tu X-User-Id header do API Gateway forward xuong, khong co FK vi users nam o database rieng cua user-service).';
