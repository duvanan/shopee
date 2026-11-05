# ĐẶC TẢ TRẢI NGHIỆM NGƯỜI DÙNG — TÀI KHOẢN VÀ XÁC THỰC (MVP-ACC)

| Thuộc tính | Nội dung |
|---|---|
| Sản phẩm | Shopee Clone — MVP R0 |
| Mã đặc tả | `MVP-ACC/UX` |
| Phiên bản | **1.0 — 05/11/2026** |
| Trạng thái | **APPROVED — UX v1.0 đã được chủ dự án duyệt ngày 05/11/2026**; phần kiểm chứng Figma/mobile và triển khai code vẫn chưa hoàn tất |
| Người phụ trách | Chủ dự án + ChatGPT, áp dụng Skill `.agents/skills/ui-ux-design/SKILL.md` |
| Nguồn nghiệp vụ | [`BA_SPEC.md`](BA_SPEC.md) — 8 tính năng, `BR-ACC-01..62`, `AC-ACC-01..66`, `EC-ACC-01..34`; `BR-CART-001` |
| Tài liệu chuẩn trong repo | `docs/features/MVP-ACC/UX_SPEC.md` — bản approved trên nhánh bàn giao |
| Figma | [Shopee Clone — Account & Authentication](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I) — 27 khung desktop + 6 khung mobile đã ghi vào file; 15 desktop mới chưa được xác nhận bằng kiểm tra ảnh cuối do Figma MCP Starter hết lượt |
| Source hiện trạng tham chiếu | `frontend/src/views/LoginView.vue`, `RegisterView.vue`, `frontend/src/stores/auth.ts`, `frontend/src/router/index.ts`, `frontend/src/style.css`, `frontend/tailwind.config.js` trên `main` |
| Phạm vi bàn giao | Đặc tả tương tác, điều hướng, lỗi/khôi phục, bố cục responsive, trợ năng, tiêu chí nghiệm thu UX; **không** phải bản xác nhận code đã đáp ứng |
| Người duyệt / bằng chứng | Chủ dự án, cuộc trao đổi ngày 05/11/2026: **"tôi thấy phần UX Spec ổn rồi, hãy thực hiện các bước tiếp theo cho tôi"** |
| Issue bàn giao Architect | [#43](https://github.com/duvanan/shopee/issues/43) |

> **Nguyên tắc nguồn dữ liệu:** Các thời hạn, giới hạn OTP, cách đăng ký, phiên, quyền và liên kết tài khoản là **quyết định BA đã duyệt**, không được UX tự đổi. Những phần trình bày/điều hướng chưa được BA xác định được ghi là **Đề xuất UX** và cần chủ dự án phê duyệt khi duyệt tài liệu này. Thiết kế Figma hiện tại là bản tĩnh, chưa phải prototype tương tác hoàn chỉnh.

---

## 1. Người dùng, mục tiêu và phạm vi

### 1.1. Vai trò

| Vai trò | Công việc cần hoàn thành | Giới hạn nghiệp vụ |
|---|---|---|
| **Khách chưa đăng nhập (Guest)** | Xem sản phẩm, đăng ký, đăng nhập | Không được thêm sản phẩm vào giỏ hay đặt hàng (`BR-CART-001`) |
| **Khách hàng (Customer)** | Đăng nhập đúng tài khoản, quản lý hồ sơ, tiếp tục mua hàng | Chỉ truy cập dữ liệu và phiên của chính tài khoản đó (`BR-ACC-05`, `31`) |
| **Quản trị viên (Admin)** | Sử dụng phần quản trị và vô hiệu hóa tài khoản theo quyền | Dùng tài khoản quản trị riêng, không dùng tài khoản Admin để mua như Customer (`BR-ACC-52`, `53`) |

### 1.2. Mục tiêu trải nghiệm

1. Người mới có thể hiểu ngay có **hai lựa chọn**: *Email + mật khẩu* hoặc *Số điện thoại + OTP*.
2. Email đăng ký phải qua **liên kết xác minh hiệu lực 24 giờ** và **thiết lập mật khẩu chính thức sau xác minh**, rồi mới đăng nhập (`BR-ACC-37`, `39`, `48`).
3. Số điện thoại Việt Nam phải được xác minh bằng **OTP SMS 6 số**, hiệu lực **5 phút**; tài khoản chỉ được tạo sau OTP đăng ký hợp lệ và **tự đăng nhập ngay** (`BR-ACC-42`, `45`, `49`).
4. Mọi lỗi có thông báo rõ ràng, không làm mất dữ liệu nhập không cần thiết và có đường quay lại/thử lại an toàn.
5. Không bao giờ tự liên kết tài khoản email và số điện thoại; không tự thêm vào giỏ sau đăng nhập (`BR-ACC-22`, `31`, `59`).
6. Phân biệt rõ: **chưa đăng nhập**, **hết hạn phiên**, **không đủ quyền**, **tài khoản bị vô hiệu hóa**.

### 1.3. Phạm vi và ngoài phạm vi

**Trong UX MVP:** Đăng ký email, chờ xác minh, xử lý liên kết và thiết lập mật khẩu; đăng ký điện thoại, đăng nhập bằng OTP; đăng nhập email, quên/đặt lại mật khẩu, hồ sơ (chỉ sửa tên hiển thị), đăng xuất một phiên, xử lý hết hạn/thu hồi phiên, thông báo không đủ quyền và ranh giới Guest → giỏ hàng. Với Admin: trạng thái/phản hồi khi quản trị viên được phép vô hiệu hóa tài khoản.

**Không thiết kế nút hay màn hình cho MVP:** Google/Facebook, liên kết/gộp tài khoản, đổi email/số đăng nhập, tự khôi phục khi mất SIM, tự xóa tài khoản (bản demo), Seller, đăng nhập bằng mật khẩu số điện thoại, mua hàng bằng tài khoản Admin. Không mở rộng phạm vi sang Checkout/Order.

---

## 2. Sơ đồ hành trình và điều hướng (User Flows)

### FL-01 — Đăng ký bằng email

`Trang đăng ký → Chọn Email → Nhập email + mật khẩu 12–128 ký tự → Gửi yêu cầu → Kiểm tra hộp thư → Mở liên kết hợp lệ ≤24 giờ → Thiết lập lại mật khẩu chính thức → Thành công → Trang đăng nhập`.

- **Trước gửi:** Kiểm tra định dạng email và độ dài mật khẩu; hiển thị lỗi ngay dưới trường tương ứng khi người dùng rời trường hoặc gửi form.
- **Trong khi gửi:** Nút chính đổi thành **“Đang xử lý…”**, vô hiệu hóa gửi lặp; không xóa giá trị email đã nhập.
- **Sau khi yêu cầu được tiếp nhận:** Chỉ thông báo **“Nếu yêu cầu hợp lệ, chúng tôi sẽ gửi hướng dẫn xác minh đến email của bạn.”**; không cam kết email đã đến hộp thư và không cho biết tài khoản có tồn tại (`D-ACC-22`).
- **Sau khi nhấn liên kết:** Nếu hợp lệ, mở màn hình thiết lập mật khẩu. Không chỉ đơn thuần nhấn link rồi tự kích hoạt với mật khẩu đã nhập ở bước trước.
- **Hoàn tất:** Sau khi tạo mật khẩu chính thức hợp lệ, hiển thị trạng thái thành công và nút **“Đến trang đăng nhập”**, **không tự đăng nhập**.
- **Đăng ký lại khi email đang chờ:** Tiếp tục xác minh, không tạo tài khoản mới, không chấp nhận mật khẩu mới từ yêu cầu công khai trước khi xác minh (`D-ACC-12`, `13`).

### FL-02 — Gửi lại email / liên kết lỗi

`Kiểm tra hộp thư / Liên kết lỗi → Yêu cầu gửi lại → Chờ tối thiểu 60 giây → Tiếp tục kiểm tra email → Link mới thay thế link cũ`.

- **Link lỗi/hết hạn/đã dùng:** Không thay đổi trạng thái tài khoản, hiển thị thông báo phù hợp và đưa ra hành động an toàn.
- **Đang trong thời gian chờ:** Nút gửi lại bị vô hiệu hóa, hiển thị số giây còn lại; chỉ cho gửi lại khi hệ thống xác nhận đủ điều kiện.
- **Giới hạn 5 lần/giờ:** Hiển thị thông báo giới hạn và thời điểm có thể thử tiếp theo **nếu hệ thống cung cấp**; không tự suy đoán thời điểm.
- **Gửi lại thành công:** Nếu có liên kết mới phát hành, liên kết cũ không còn hiệu lực. Tránh hướng dẫn người dùng nhấn vào cả hai.
- **Sau 7 ngày chưa xác minh:** Cần một trạng thái “Yêu cầu đăng ký đã hết thời gian chờ” và hướng dẫn bắt đầu quy trình đăng ký hợp lệ; không tuyên bố dữ liệu đã bị xóa nếu chưa có xác nhận (`BR-ACC-38`).

### FL-03 — Đăng ký số điện thoại

`Đăng ký → Chọn Số điện thoại → Nhập số Việt Nam → Gửi OTP → Nhập 6 chữ số → Xác minh hợp lệ → Tạo Customer → Tự đăng nhập → Trang tiếp tục hợp lệ`.

- Hiển thị tiền tố quốc gia **+84**; chấp nhận cách nhập được hệ thống hỗ trợ và thông báo định dạng hợp lệ. Không nhận số ngoài Việt Nam (`BR-ACC-47`).
- Chỉ tạo **yêu cầu đăng ký đang chờ** khi gửi OTP, không nói “Tài khoản đã được tạo” trước khi OTP đúng (`BR-ACC-45`).
- Sau OTP hợp lệ, hiển thị trạng thái thành công ngắn rồi tự đăng nhập vào tài khoản vừa tạo (`BR-ACC-49`).
- Nếu bắt đầu từ trang sản phẩm, sau tự đăng nhập quay lại sản phẩm; **không tự thêm giỏ**.

### FL-04 — Đăng nhập điện thoại

`Đăng nhập → Chọn Số điện thoại → Gửi OTP mục đích ĐĂNG_NHẬP → Nhập 6 số hợp lệ → Đăng nhập vào đúng tài khoản điện thoại → Trang tiếp tục`.

- Nếu số chưa đăng ký, **không tự tạo tài khoản**; phản hồi trung tính và cung cấp đường sang trang đăng ký (`BR-ACC-46`, `60`).
- OTP đăng nhập khác mục đích OTP đăng ký: chỉ hiển thị màn tương tự, không được chấp nhận lẫn (`BR-ACC-21`).

### FL-05 — Đăng nhập email

`Đăng nhập → Chọn Email → Email + mật khẩu chính thức → Kiểm tra hợp lệ → Đăng nhập vào tài khoản email → Trang tiếp tục`.

- Nếu chưa xác minh: không tạo phiên Customer; hướng dẫn kiểm tra/gửi lại xác minh bằng thông báo an toàn.
- Sai thông tin: không chỉ rõ email hay mật khẩu sai khi việc đó có thể tiết lộ sự tồn tại tài khoản.
- Quên mật khẩu: mở quy trình FL-06.

### FL-06 — Quên/đặt lại mật khẩu

`Quên mật khẩu → Nhập email → Gửi yêu cầu → Thông báo trung tính → Nhấn liên kết đặt lại hợp lệ → Nhập mật khẩu mới 12–128 ký tự + xác nhận → Hoàn tất → Đăng nhập`.

- Form yêu cầu luôn phản hồi trung tính, không xác nhận email có tài khoản hay không.
- Liên kết lỗi/hết hạn/dùng lại: không đổi mật khẩu, có hành động yêu cầu liên kết mới.
- **BA chưa chốt thời hạn cụ thể cho liên kết đặt lại mật khẩu**, cũng chưa chốt chính sách thu hồi tất cả phiên cũ sau reset; UX hiển thị hướng dẫn chung, **không đưa số phút/hứa thu hồi phiên** khi Architect chưa xác nhận. Đây là hai điểm cần handoff.

### FL-07 — Guest thêm vào giỏ

`Trang sản phẩm → Thêm vào giỏ khi là Guest → Chuyển Login kèm ngữ cảnh trang sản phẩm → Đăng nhập/đăng ký → Trở về trang sản phẩm → Customer tự nhấn “Thêm vào giỏ”`.

- Khi chuyển hướng, chưa thay đổi giỏ hàng.
- Đường quay lại chỉ được dùng khi là địa chỉ nội bộ được cho phép; nếu không hợp lệ thì về trang chủ/danh sách sản phẩm, không chuyển sang miền tùy ý.
- Khi người dùng nhấn “Quay lại” trước khi đăng nhập, trở lại trang sản phẩm trong trạng thái Guest.

### FL-08 — Phiên và quyền

- **Đăng xuất:** Người dùng chọn **“Đăng xuất khỏi thiết bị này”** → chờ hệ thống xác nhận vô hiệu hóa phiên → chuyển về Guest. Chỉ xóa trạng thái giao diện trong khi API chưa thu hồi **không đủ** cho yêu cầu BA (`BR-ACC-50`). Nếu lỗi xác nhận, không được tuyên bố phiên đã thu hồi; cần xử lý theo trạng thái an toàn do Architect cung cấp.
- **Hết hạn phiên:** Không tiếp tục thao tác được bảo vệ; hướng dẫn đăng nhập lại; sau đó chỉ quay về điểm tiếp tục hợp lệ.
- **Không đủ quyền 403:** Hiển thị “Bạn không có quyền thực hiện thao tác này” và hành động **“Về trang trước”** / **“Về trang chủ”**. Không dùng thông báo “Bạn cần đăng nhập lại” khi tài khoản vẫn đăng nhập hợp lệ.
- **Admin vô hiệu hóa tài khoản:** Các phiên bị chặn ngay; hiển thị thông báo tài khoản không còn được phép sử dụng, không hứa rằng đăng nhập lại sẽ giải quyết.

---

## 3. Danh sách màn hình, Figma và mức độ hoàn thiện

**Baseline khi soạn UX v1.0:** Figma có 12 khung desktop (D01–D12) và 6 khung mobile (M01–M06). **Sau bổ sung:** đã ghi thêm 15 khung desktop (D13–D27), tổng 27 desktop + 6 mobile. Bảng phía dưới giữ trạng thái baseline để đối chiếu; xem phụ lục §13 cho link khung mới. Các khung là minh họa tĩnh, không phải tương tác đã hoạt động.

| Mã UX | Màn hình / mục đích | Figma desktop | Figma mobile | Trạng thái |
|---|---|---|---|---|
| UX-S01 | Đăng nhập email | [D01](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-8) | [M01](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-367) | Có khung chính |
| UX-S02 | Đăng nhập điện thoại, yêu cầu OTP | [D02](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-42) | Chưa có khung chuyên biệt | Desktop có |
| UX-S03 | Đăng ký email | [D03](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-73) | Chưa có khung chuyên biệt | Desktop có |
| UX-S04 | Đăng ký số điện thoại | [D04](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-107) | [M02](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-391) | Có khung chính |
| UX-S05 | Chờ email / gửi lại xác minh | [D05](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-138) | [M04](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-439) | Có khung chính |
| UX-S06 | Nhấn link xác minh / thiết lập mật khẩu chính thức | [D06](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-164) | [M05](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-454) | Có khung chính |
| UX-S07 | Nhập OTP điện thoại (dùng lại bố cục cho đăng ký & đăng nhập, **phân biệt mục đích**) | [D07](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-195) | [M03](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-412) | Có khung chính, cần hai trạng thái kết quả |
| UX-S08 | Yêu cầu quên mật khẩu | [D08](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-233) | Chưa có khung chuyên biệt | Desktop có |
| UX-S09 | Hồ sơ cá nhân | [D09](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-259) | [M06](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-474) | Có khung chính |
| UX-S10 | Liên kết email không hợp lệ/hết hạn | [D10](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-291) | Chưa có khung chuyên biệt | Desktop có |
| UX-S11 | Hết hạn phiên / yêu cầu đăng nhập lại | [D11](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-316) | Chưa có khung chuyên biệt | **Figma D11 đang gộp nhầm 401 và 403**, cần tách |
| UX-S12 | Giới hạn OTP / lỗi gửi SMS | [D12](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=2-341) | Chưa có khung chuyên biệt | Desktop có |
| UX-S13 | Nhập mật khẩu mới từ link **quên mật khẩu** | **Chưa vẽ** | **Chưa vẽ** | **Thiếu P0** |
| UX-S14 | Không đủ quyền (403) | **Chưa tách** | **Chưa vẽ** | **Thiếu P0** |
| UX-S15 | Tài khoản bị vô hiệu hóa | **Chưa vẽ riêng** | **Chưa vẽ** | **Thiếu P0** |
| UX-S16 | Admin xem người dùng, xác nhận vô hiệu hóa/hoàn tất/lỗi | **Chưa vẽ trong file Auth** | Dùng thiết kế Admin hiện có nếu phù hợp | **Cần bổ sung/đối chiếu P0** |
| UX-S17 | Hoàn tất/xử lý lỗi **OTP đăng nhập** | Tái dùng UX-S07 với nội dung khác | Tái dùng UX-S07 | Chưa có trạng thái phân biệt rõ |
| UX-S18 | Quá hạn yêu cầu đăng ký email 7 ngày | Tái dùng mẫu lỗi có CTA bắt đầu lại | Tái dùng mẫu lỗi | Chưa có trạng thái riêng |

**Ghi chú theo baseline:** Các dòng “chưa vẽ” trong bảng trên là trạng thái khi soạn v1.0; xem §13 để biết màn hình desktop đã bổ sung. Vẫn còn biến thể mobile chưa vẽ, và phê duyệt UX Spec là phê duyệt **hành vi**, không phải tuyên bố Figma/Frontend đã hoàn tất.

### 3.1. Phác thảo cấu trúc (wireframe — minh họa, không thay thế Figma)

**Desktop — đăng ký/đăng nhập:**

```text
┌────────────────────────────────────────────────────────────┐
│ Shopee Clone                            Trợ giúp           │
├───────────────────────────┬────────────────────────────────┤
│ Khối giới thiệu thương hiệu│  ĐĂNG NHẬP / ĐĂNG KÝ          │
│ và lợi ích sản phẩm       │  [ Email | Số điện thoại ]    │
│                          │  Nhãn / Ô nhập / Lỗi tại trường│
│                          │  [ Hành động chính ]           │
│                          │  Liên kết trợ giúp / đổi luồng │
└───────────────────────────┴────────────────────────────────┘
```

**Mobile — biểu mẫu một cột:**

```text
┌───────────────────────────┐
│ ‹  Shopee                 │
│ Tiêu đề                  │
│ Hướng dẫn ngắn           │
│ [ Email | Điện thoại ]   │
│ Nhãn / Ô nhập            │
│ Lỗi gần ô nhập           │
│ [ Hành động chính ]      │
│ Gửi lại / Trợ giúp       │
└───────────────────────────┘
```

---

## 4. Đặc tả tương tác: từng hành động và phản hồi

| Màn / thành phần | Hành động | Phản hồi mong đợi | Quy tắc BA |
|---|---|---|---|
| Đăng ký — chọn Email/Điện thoại | Chọn một thẻ phương thức | Hiển thị trường thuộc phương thức đó; không trộn định danh, mật khẩu và OTP giữa hai tài khoản | `BR-ACC-22`, `29` |
| Đăng ký email | Gõ email, mật khẩu | Nhãn rõ, lỗi dữ liệu cạnh ô; mật khẩu 12–128 ký tự; không ép các loại ký tự | `BR-ACC-41`, `57` |
| Đăng ký email — Gửi | Bấm **“Tạo tài khoản”** | Loading, khóa gửi lặp, phản hồi trung tính, dẫn đến kiểm tra hộp thư khi được chấp nhận | `BR-ACC-08`, `60` |
| Chờ email | Bấm **“Gửi lại liên kết”** | Nếu đủ điều kiện, chuyển đang gửi/đã tiếp nhận; nếu chưa 60 giây, khóa nút và hiện đếm ngược | `BR-ACC-37` |
| Chờ email | Vượt 5 lần/giờ | Hiện thông báo giới hạn, không thử gửi thêm tự động | `BR-ACC-37` |
| Liên kết email | Mở link | Xác minh hợp lệ → màn đặt mật khẩu; không hợp lệ/hết 24 giờ → màn lỗi | `BR-ACC-10`, `37`, `39` |
| Thiết lập mật khẩu | Nhập mật khẩu mới & xác nhận | So trùng, lỗi trực tiếp; nút tiếp tục chỉ hoàn tất sau khi hợp lệ | `BR-ACC-39`, `57` |
| Sau xác minh email | Hoàn tất thiết lập mật khẩu | Hiện thành công → nút **“Đến đăng nhập”**, không tự đăng nhập | `BR-ACC-48` |
| Đăng ký điện thoại | Nhập số +84 | Thông báo sai định dạng/ngoài Việt Nam, không tạo tài khoản ở bước gửi OTP | `BR-ACC-45`, `47` |
| Gửi SMS | Bấm **“Gửi OTP”** | Loading; sau khi được tiếp nhận chuyển sang màn nhập OTP, không khẳng định SMS chắc chắn đã đến | `BR-ACC-42`, `43` |
| Nhập OTP | Nhập/dán 6 số | Nhận đủ 6 số, tự di chuyển con trỏ hợp lý; hỗ trợ sửa/xóa; mã hiển thị theo mục đích đăng ký/đăng nhập | `BR-ACC-17`, `21`, `42` |
| Xác minh OTP | Bấm **“Xác minh”** | Loading, khóa gửi lặp; nếu đúng: **đăng ký → tạo tài khoản & tự đăng nhập**; **đăng nhập → phiên của tài khoản đang có** | `BR-ACC-45`, `49`, `20` |
| OTP sai | Nhập mã sai | Báo lỗi ngay gần vùng nhập; thông báo số lần còn lại **chỉ nếu API cung cấp chính xác** | `BR-ACC-18`, `42` |
| OTP đủ 5 lỗi | Thử tiếp | Khóa đợt xác minh, hướng dẫn bắt đầu lại theo hạn mức gửi; không hứa mã cũ còn dùng được | `BR-ACC-42` |
| OTP hết 5 phút | Đếm ngược về 0 | Vô hiệu hóa xác minh mã cũ, hiện CTA gửi lại (nếu đủ điều kiện) | `BR-ACC-42` |
| Gửi lại OTP | Bấm gửi lại | Chờ tối thiểu 60 giây, áp dụng 5 lần/giờ và 10 lần/ngày; mã mới làm mã cũ hết hiệu lực | `BR-ACC-43`, `44` |
| Login email | Bấm Đăng nhập | Trạng thái loading; nếu sai báo **“Không thể đăng nhập. Vui lòng kiểm tra thông tin hoặc thử lại.”** | `BR-ACC-24`, `60` |
| Login điện thoại | Yêu cầu OTP số chưa đăng ký | Không đăng ký ngầm; phản hồi trung tính và CTA đăng ký tùy trạng thái được phép hiển thị | `BR-ACC-46`, `60` |
| Quên mật khẩu | Gửi email | Phản hồi trung tính, không tiết lộ tài khoản | `BR-ACC-51`, `60` |
| Reset mật khẩu | Mở link và gửi mật khẩu mới | Chỉ hoàn tất nếu link hợp lệ; không đăng nhập ngầm (đề xuất UX, cần xác nhận quy trình kỹ thuật) | `BR-ACC-51`, `57` |
| Hồ sơ | Bấm Lưu | Chỉ sửa tên hiển thị; email/số định danh chỉ đọc; thành công thông báo rõ | `BR-ACC-58` |
| Đăng xuất | Bấm Đăng xuất khỏi thiết bị này | Thu hồi phiên hiện tại ở API; chuyển Guest sau khi có kết quả an toàn | `BR-ACC-50` |
| Hết phiên | Gọi hành động bảo vệ bị từ chối vì phiên | Điều hướng đăng nhập, giữ điểm tiếp tục hợp lệ, không tự thực hiện lại hành động tạo dữ liệu | `BR-ACC-25`, `59` |
| Không đủ quyền | Mở chức năng Admin ngoài quyền | Màn 403 riêng, không gợi ý đăng nhập lại để nâng quyền | `BR-ACC-26` |
| Tài khoản bị vô hiệu hóa | Thao tác khi đã bị khóa | Thông báo không thể tiếp tục; tránh tự động retry/refresh vô hạn | `BR-ACC-54` |
| Guest thêm giỏ | Nhấn Thêm vào giỏ | Chuyển đăng nhập; sau đó về sản phẩm; **chưa thêm hàng** | `BR-CART-001`, `BR-ACC-59` |

### 4.1. Hành vi dùng chung

- **Gửi biểu mẫu:** Nhấn Enter có tác dụng tương đương nút chính khi biểu mẫu hợp lệ; không gửi hai lần khi đang xử lý.
- **Chuyển thẻ Email ↔ Điện thoại:** Giữ dữ liệu tạm của *cùng một màn* khi có thể, nhưng tuyệt đối không dùng số làm email, không gửi OTP đăng ký qua luồng đăng nhập. **Đề xuất UX**: xóa mật khẩu/OTP hiển thị khi rời luồng nhạy cảm, tránh lộ ngẫu nhiên.
- **Nút Back / điều hướng:** Quay lại màn trước mà không gửi lại yêu cầu xác minh; không tự trừ lượt gửi hoặc số lần sai khi chỉ chuyển màn.
- **Lỗi form:** Có thông báo sát trường lỗi và tóm tắt lỗi khi nhiều trường không hợp lệ; giữ dữ liệu không nhạy cảm. Không hiển thị lại OTP/mật khẩu trong thông báo lỗi.
- **Mất kết nối:** Không khẳng định thao tác đã thành công/thất bại nếu kết quả chưa rõ. Hướng dẫn **“Kiểm tra kết nối rồi thử lại”** và dùng cơ chế kiểm tra trạng thái an toàn trước khi tạo yêu cầu mới (phụ thuộc API Architect).
- **Tải/xử lý lâu:** Có nhãn trạng thái và loading; không chỉ dùng màu hoặc biểu tượng quay để truyền đạt nội dung.

---

## 5. Trạng thái giao diện và nội dung thông báo chuẩn

### 5.1. Bảng trạng thái bắt buộc

| Ngữ cảnh | Trạng thái giao diện | Người dùng nhìn thấy / phục hồi |
|---|---|---|
| Form đăng ký/login | Ban đầu / đang nhập / dữ liệu sai | Label rõ ràng; lỗi dưới trường; có thể sửa và gửi lại |
| Gửi email | Đang gửi / đã tiếp nhận / lỗi kết nối / bị giới hạn | Thông báo trung tính; nút gửi lại có cooldown; lỗi có đường thử lại an toàn |
| Liên kết email | Hợp lệ / hết hạn / đã dùng / đã bị thay mới / tài khoản đã kích hoạt | Hợp lệ → đặt mật khẩu; không hợp lệ → CTA xin liên kết mới; không kích hoạt sai |
| Email chờ 7 ngày | Chưa hết hạn / quá hạn chờ | Quá hạn → hướng dẫn đăng ký lại; không tuyên bố tự xóa dữ liệu |
| Gửi OTP | Ban đầu / đang gửi / được tiếp nhận / không đến / giới hạn / lỗi mạng | Đếm thời gian, gửi lại khi đủ hạn mức; không cam kết SMS đến |
| Nhập OTP | Chưa đủ 6 số / đủ 6 / sai / đủ 5 lần sai / hết 5 phút / mã cũ | Vô hiệu hóa xác minh khi không đủ điều kiện; thông báo kế tiếp cụ thể |
| Login | Hợp lệ / sai / chưa xác minh / tài khoản bị khóa | Thông báo đúng trường hợp mà không lộ định danh không phù hợp |
| Quên mật khẩu | Đang gửi / đã tiếp nhận / link hỏng / link hết hạn / hoàn tất | Trung tính ở yêu cầu; link hợp lệ mới mở form; thất bại không thay mật khẩu |
| Hồ sơ | Đang tải / rỗng hiếm gặp / cập nhật / lỗi / lưu thành công | Skeleton ngắn, báo lỗi, giữ tên người dùng đã nhập nếu lưu lỗi |
| Phiên | Hợp lệ / hết hạn 7 ngày idle / đủ 30 ngày / bị thu hồi / mạng lỗi | Không tiếp tục thao tác bảo vệ; đăng nhập lại khi phù hợp |
| Quyền | Chưa đăng nhập (401) / thiếu quyền (403) / tài khoản bị vô hiệu hóa | **Ba trạng thái khác nhau**, lời giải thích và CTA tương ứng |
| Trang sản phẩm → login | Guest / đang đăng nhập / đã login | Giữ ngữ cảnh sản phẩm; không tự thêm giỏ |

### 5.2. Nội dung hiển thị (microcopy) — đề xuất UX v1.0

> Bảng này là **câu chữ đề xuất**, chưa tự trở thành quyết định BA. Với các thao tác có thể tiết lộ sự tồn tại tài khoản, ưu tiên câu trung tính, không lặp lại email/số đầy đủ trong thông báo công khai.

| Mã | Trạng thái | Câu chữ đề xuất | Hành động |
|---|---|---|---|
| COPY-01 | Đăng ký email đã tiếp nhận | “Nếu yêu cầu hợp lệ, chúng tôi sẽ gửi hướng dẫn xác minh đến email của bạn.” | “Kiểm tra email” |
| COPY-02 | Gửi lại email đang chờ | “Bạn có thể gửi lại sau {n} giây.” | Nút gửi lại bị khóa |
| COPY-03 | Gửi email vượt ngưỡng | “Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau.” | “Quay về đăng nhập” |
| COPY-04 | Liên kết xác minh hết hạn | “Liên kết không còn hiệu lực. Hãy yêu cầu liên kết mới.” | “Gửi lại liên kết” |
| COPY-05 | Đặt mật khẩu sau email | “Email đã được xác minh. Hãy thiết lập mật khẩu chính thức để bảo vệ tài khoản.” | “Hoàn tất” |
| COPY-06 | Đặt mật khẩu thành công | “Tài khoản đã sẵn sàng. Vui lòng đăng nhập để tiếp tục.” | “Đến đăng nhập” |
| COPY-07 | Gửi OTP đã tiếp nhận | “Nếu yêu cầu hợp lệ, mã xác minh sẽ được gửi đến số điện thoại của bạn.” | Đến nhập OTP khi đủ điều kiện |
| COPY-08 | OTP sai | “Mã xác minh không đúng. Vui lòng kiểm tra và thử lại.” | “Xác minh” |
| COPY-09 | OTP hết hạn | “Mã xác minh đã hết hạn.” | “Gửi mã mới” nếu đủ điều kiện |
| COPY-10 | OTP vượt số lần thử | “Bạn đã nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới khi có thể.” | “Gửi mã mới” |
| COPY-11 | Gửi OTP quá thường xuyên | “Yêu cầu gửi mã đang tạm bị giới hạn. Hãy thử lại sau.” | Hiện thời gian thử tiếp nếu API cung cấp |
| COPY-12 | Đăng nhập không thành công | “Không thể đăng nhập. Vui lòng kiểm tra thông tin hoặc thử lại.” | Giữ email, xóa trường mật khẩu nếu cần |
| COPY-13 | Quên mật khẩu đã tiếp nhận | “Nếu email phù hợp, hướng dẫn đặt lại mật khẩu sẽ được gửi đến hộp thư.” | “Quay lại đăng nhập” |
| COPY-14 | Hồ sơ lưu thành công | “Đã cập nhật tên hiển thị.” | Không cần điều hướng |
| COPY-15 | Phiên hết hạn | “Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.” | “Đăng nhập” |
| COPY-16 | Không có quyền (403) | “Bạn không có quyền truy cập chức năng này.” | “Về trang trước” |
| COPY-17 | Tài khoản bị vô hiệu hóa | “Tài khoản hiện không thể sử dụng. Vui lòng liên hệ hỗ trợ nếu cần.” | “Trung tâm hỗ trợ” |
| COPY-18 | Mất SIM | “Không thể khôi phục bằng mã OTP khi bạn không còn sử dụng số điện thoại. Vui lòng liên hệ hỗ trợ.” | “Hướng dẫn hỗ trợ” (nếu có kênh hỗ trợ thật) |
| COPY-19 | Mất mạng / chưa rõ trạng thái | “Chưa thể xác nhận kết quả. Vui lòng kiểm tra kết nối và thử lại.” | “Thử lại” có kiểm tra trạng thái |
| COPY-20 | Guest muốn thêm giỏ | “Đăng nhập để thêm sản phẩm vào giỏ hàng.” | “Đăng nhập / Đăng ký” |

**Không giả lập countdown từ 05:00 như thời gian tuyệt đối của server.** Đồng hồ trên UI chỉ mang tính hiển thị; khi thao tác, server vẫn là nguồn quyết định hợp lệ. Nếu bị từ chối do khác biệt thời gian, cần chuyển đúng trạng thái và hướng dẫn nhận mã mới.

---

## 6. Thiết kế giao diện, bố cục linh hoạt và thành phần dùng chung

### 6.1. Đặc điểm đã quan sát trong Figma/source

- **Màu dự án:** `brand #EE4D2D`, `brand-dark #D7401F`, `brand-soft #FDECE7` trong `frontend/tailwind.config.js`.
- **Figma:** thiết kế desktop khung **1180 × 780**, mobile **390 × 844**, chữ **Inter**, thanh chọn phương thức và các form theo thẻ; 12 thông báo info/success/error đã được chỉnh **thu gọn chiều cao và chiều ngang theo nội dung**.
- **Source hiện trạng:** Login/Register Vue đã có email/mật khẩu; Register vẫn yêu cầu họ tên và tự đặt trạng thái đăng nhập sau đăng ký. **Đó là hành vi hiện có, không phải nghiệp vụ mới được duyệt.** Chỉ khi implement BA mới mới sửa cho khớp đặc tả.

### 6.2. Đề xuất UX bố cục để duyệt

| Hạng mục | Desktop | Mobile |
|---|---|---|
| Bố cục | Khối giới thiệu thương hiệu bên trái, form ở cột phải | Một cột; bỏ khối trang trí lớn, ưu tiên form |
| Khung form | Giới hạn chiều rộng khoảng 440–500px | Khoảng đệm hai bên 16–20px; form lấp chiều ngang hữu ích |
| Thẻ chọn Email / Điện thoại | Đủ chiều rộng form, có trạng thái đang chọn | Hai thẻ không tràn ngang, nhấn dễ dàng |
| Nút hành động chính | Rõ ràng, đủ lớn, phản hồi loading và disabled | Chiều cao tương tác đề xuất ≥44px, phù hợp thao tác chạm |
| Trường nhập | Label luôn hiện, placeholder chỉ là ví dụ | Bàn phím phù hợp email/tel/OTP, hỗ trợ autofill nếu an toàn |
| Thông báo | **Compact:** độ rộng vừa nội dung, có trần khoảng 366px; tối đa hai/ba dòng tùy nội dung | **Compact:** độ rộng vừa nội dung, có trần khoảng 310px; tự xuống dòng |
| Form dài / lỗi | Không che CTA quan trọng | Có thể cuộn dọc, nút và nội dung không bị bàn phím ảo che |
| Trợ giúp | Liên kết dưới form | Liên kết đủ dễ chạm, không bị bám sát cạnh màn |

**Lưu ý:** Các ngưỡng 44px, 366px, 310px là đề xuất/quan sát thiết kế, không phải quy tắc BA; không được giả định cố định tuyệt đối trên mọi kích thước thiết bị.

### 6.3. Hệ thành phần giao diện đề xuất cho Frontend

- `AuthMethodTabs`: chọn Email / Điện thoại với trạng thái tab được chọn, hỗ trợ bàn phím.
- `AuthCard` / `AuthPageShell`: vỏ trang và form, bố cục desktop/mobile.
- `FormField`: label, mô tả, input, validation message, trạng thái disabled/loading.
- `PasswordField`: hiển thị/ẩn mật khẩu, gợi ý độ dài, autofill hợp lý.
- `PhoneNumberField`: cố định +84, nhập và trình bày số Việt Nam, không dùng số nhận hàng làm định danh.
- `OtpInput`: một giá trị xác thực 6 số nhưng có thể biểu diễn thành sáu ô; hỗ trợ dán toàn bộ mã, xóa, điều hướng bàn phím, autocomplete OTP khi khả dụng.
- `OtpResendControl`: nút gửi lại, cooldown, trạng thái bị giới hạn.
- `Alert`: loại **Thông tin / Thành công / Lỗi**; chiều cao ôm nội dung, độ rộng tối đa theo viewport; có văn bản rõ chứ không chỉ màu.
- `SessionNotice` / `PermissionDenied`: thông báo hết hạn phiên (401) khác không đủ quyền (403).
- `LoadingActionButton`, `InlineError`, `StatusMessage`, `AppLink`.

**Tái sử dụng trước, tạo mới sau:** Source đang sử dụng Tailwind, Vue 3, Vue Router, Pinia, `authApi`/`authStore`; các thành phần mới chỉ là đề xuất về trách nhiệm UI, không tuyên bố code hiện tại đã có các component đó. Tên API/đường dẫn dự kiến phải được Architect/Frontend xác thực trước khi triển khai.

### 6.4. Rủi ro độ tương phản và trợ năng

- Với chữ nhỏ màu **trắng trên `#EE4D2D`**, tỉ lệ tương phản tính theo WCAG xấp xỉ **3,66:1**, **không đạt** ngưỡng 4,5:1 cho chữ thường. Cần dùng màu đậm hơn, ví dụ `#D7401F` (xấp xỉ **4,52:1**) hoặc kiểm thử một lựa chọn tương đương; không mặc nhiên kết luận cả trang đạt WCAG.
- Nội dung cảnh báo màu cam trên nền cam nhạt cũng cần đo độ tương phản và có biểu tượng/văn bản bổ sung.
- Không chỉ dùng viền đỏ để thể hiện lỗi; có câu thông báo và liên kết với trường nhập.

---

## 7. Khả năng tiếp cận (Accessibility) và tương tác bàn phím

1. Mỗi màn có **một tiêu đề cấp 1**, hệ thứ bậc tiêu đề đúng, nhãn cho từng trường. Placeholder không thay thế label.
2. Các thẻ Email/Điện thoại phải là nút/tab thực sự, thể hiện trạng thái được chọn cho trình đọc màn hình; dùng Tab/Shift+Tab, phím mũi tên nếu triển khai chuẩn tab.
3. Các nút và liên kết có trạng thái focus dễ nhận ra; không tự cuốn focus mất kiểm soát khi form hiển thị lỗi.
4. Lỗi được nối với trường nhập (ví dụ `aria-describedby`); tóm tắt lỗi/khôi phục được công bố qua vùng thông báo phù hợp, không lặp quá nhiều lần.
5. OTP có nhãn rõ **“Mã xác minh gồm 6 chữ số”**; người dùng có thể nhập bằng bàn phím, dán toàn bộ mã và xóa/sửa được; không buộc 6 lần tab cho 6 ô trang trí.
6. Đồng hồ đếm ngược OTP/cooldown **không công bố mỗi giây** tới trình đọc màn hình; công bố khi sắp hết hoặc đã hết và cập nhật nhãn nút gửi lại.
7. Không tự động chuyển màn ngay sau *nhập đủ 6 số* nếu chưa có thao tác/điều kiện xác minh rõ ràng; khi OTP được chấp nhận có thông báo tiến trình.
8. Thông báo lỗi, loading, thành công luôn có chữ; không dùng màu làm cách truyền tải duy nhất.
9. Nội dung không bị cắt khi thu phóng chữ lớn, giao diện vẫn dùng được trên màn 320px; chạm không bị quá sát nhau.
10. Khi điều hướng thành công/thất bại, chuyển focus hợp lý tới tiêu đề hoặc thông báo quan trọng; giữ mạch điều hướng bàn phím.
11. Mật khẩu/OTP mặc định được che; nút hiện/ẩn mật khẩu có tên dễ hiểu và không đưa giá trị nhạy cảm vào log hoặc thông báo.
12. **Mục tiêu:** áp dụng nguyên tắc WCAG 2.2 AA; **chưa xác nhận đạt** cho tới khi có kiểm thử thủ công, tự động và thực tế trên trình đọc màn hình.

---

## 8. Tiêu chí nghiệm thu trải nghiệm (UX Acceptance Criteria)

Các tiêu chí sau dùng mã **`UX-ACC-xx`** để phân biệt với tiêu chí nghiệp vụ **`AC-ACC-xx`**. Mỗi tiêu chí phải có kiểm thử UI tương ứng.

| Mã | Cho trước — Khi — Thì | BA tham chiếu | Màn hình |
|---|---|---|---|
| UX-ACC-01 | **Cho trước** người dùng mở Đăng ký; **khi** chọn Email hoặc Điện thoại; **thì** chỉ các trường và hướng dẫn đúng phương thức được hiển thị rõ. | `AC-ACC-01`, `26` | S03, S04 |
| UX-ACC-02 | Cho trước form đăng ký email; khi nhập mật khẩu <12 hoặc >128 ký tự; thì có lỗi dễ hiểu bên cạnh trường và không gửi form. | `AC-ACC-57` | S03 |
| UX-ACC-03 | Cho trước đăng ký email đang gửi; khi bấm nút nhiều lần; thì chỉ có một lượt submit trên giao diện và trạng thái xử lý rõ. | `AC-ACC-01`, `22` | S03 |
| UX-ACC-04 | Cho trước yêu cầu email đã tiếp nhận; khi hiển thị kết quả; thì thông báo trung tính và hướng dẫn kiểm tra hộp thư. | `AC-ACC-24`, `61` | S05 |
| UX-ACC-05 | Cho trước chưa qua 60 giây; khi người dùng muốn gửi lại email; thì nút không cho gửi mới và hiển thị thời gian còn lại. | `AC-ACC-43` | S05 |
| UX-ACC-06 | Cho trước vượt 5 lượt gửi lại/giờ; khi cố gửi; thì hiện thông báo giới hạn, không giả vờ đã gửi. | `AC-ACC-43` | S05 |
| UX-ACC-07 | Cho trước link email hợp lệ; khi mở; thì trang thiết lập mật khẩu chính thức xuất hiện; không tự đăng nhập. | `AC-ACC-17`, `52` | S06 |
| UX-ACC-08 | Cho trước link hết hạn/đã dùng; khi mở; thì hiển thị trạng thái thất bại và CTA xin link mới hợp lệ. | `AC-ACC-18..20` | S10 |
| UX-ACC-09 | Cho trước mật khẩu chính thức nhập không hợp lệ/không trùng; khi hoàn tất; thì có lỗi cạnh trường và không kích hoạt sai. | `AC-ACC-17`, `57` | S06 |
| UX-ACC-10 | Cho trước đặt mật khẩu email thành công; khi kết thúc; thì hiển thị nút đến Login, không tự tạo phiên. | `AC-ACC-52` | S06, S01 |
| UX-ACC-11 | Cho trước đăng ký bằng số Việt Nam; khi nhập dữ liệu; thì giao diện nhận diện +84, từ chối dữ liệu ngoài phạm vi. | `AC-ACC-51` | S04 |
| UX-ACC-12 | Cho trước yêu cầu OTP đăng ký đã tiếp nhận; khi mở màn OTP; thì có 6 ô/giá trị xác minh, thông tin 5 phút và hướng dẫn gửi lại. | `AC-ACC-26`, `29` | S07 |
| UX-ACC-13 | Cho trước người dùng nhập OTP thiếu/không phải số; khi xác minh; thì báo lỗi hoặc chặn submit thích hợp và cho sửa. | `AC-ACC-28` | S07 |
| UX-ACC-14 | Cho trước OTP sai; khi trả lỗi; thì hiển thị lỗi gần OTP, giữ đường nhập lại hợp lệ. | `AC-ACC-28`, `46` | S07 |
| UX-ACC-15 | Cho trước đủ 5 lần nhập sai; khi nhận phản hồi; thì khóa đợt xác minh và chỉ dẫn đường gửi mã mới theo giới hạn. | `AC-ACC-46` | S07, S12 |
| UX-ACC-16 | Cho trước OTP đã quá 5 phút; khi bấm xác minh; thì UI không chấp nhận mã cũ, hướng dẫn gửi mã mới. | `AC-ACC-29` | S07 |
| UX-ACC-17 | Cho trước người dùng gửi OTP mới; khi xác nhận; thì UI thông báo mã cũ không còn hiệu lực. | `AC-ACC-48` | S07 |
| UX-ACC-18 | Cho trước chưa đủ 60 giây hoặc vượt 5 lần/giờ, 10 lần/ngày; khi gửi lại OTP; thì hiển thị trạng thái bị giới hạn và không gửi lặp. | `AC-ACC-47` | S07, S12 |
| UX-ACC-19 | Cho trước OTP **đăng ký** được chấp nhận; khi hoàn tất; thì hiển thị thành công và tự đăng nhập đúng tài khoản mới. | `AC-ACC-27`, `49` | S04, S07 |
| UX-ACC-20 | Cho trước OTP **đăng nhập** được chấp nhận; khi hoàn tất; thì vào đúng tài khoản đã đăng ký, không tạo tài khoản mới. | `AC-ACC-30`, `50` | S02, S07, S17 |
| UX-ACC-21 | Cho trước email/mật khẩu không hợp lệ; khi đăng nhập; thì có thông báo trung tính, không chỉ rõ định danh có tồn tại. | `AC-ACC-06`, `61` | S01 |
| UX-ACC-22 | Cho trước tài khoản email chưa xác minh; khi thử login; thì không có trải nghiệm sử dụng Customer và có hướng xác minh/gửi lại an toàn. | `AC-ACC-16`, `23` | S01, S05 |
| UX-ACC-23 | Cho trước người dùng quên mật khẩu; khi gửi yêu cầu; thì có loading và thông báo trung tính bất kể tài khoản có tồn tại. | `AC-ACC-56`, `61` | S08 |
| UX-ACC-24 | Cho trước link đặt lại hợp lệ; khi nhập mật khẩu mới đạt 12–128 ký tự; thì có thể hoàn tất và có đường đăng nhập; link hỏng không làm thay đổi mật khẩu. | `AC-ACC-56`, `57` | S13 |
| UX-ACC-25 | Cho trước Customer có hai tài khoản độc lập; khi đăng nhập một tài khoản; thì hồ sơ/giỏ/đơn thể hiện đúng tài khoản đang dùng, không có hành động gộp. | `AC-ACC-34..38`, `64` | S09 |
| UX-ACC-26 | Cho trước Customer xem hồ sơ; khi sửa; thì chỉ tên hiển thị có thể chỉnh; định danh đăng nhập chỉ đọc. | `AC-ACC-59` | S09 |
| UX-ACC-27 | Cho trước Guest nhấn thêm giỏ trên trang sản phẩm; khi đăng nhập thành công; thì về đúng sản phẩm và **chưa** có mặt hàng được tự thêm. | `AC-ACC-12`, `60` | S01/S02 + Product |
| UX-ACC-28 | Cho trước một Customer có phiên A/B; khi đăng xuất A; thì UX chỉ thông báo thành công sau khi A được thu hồi theo chính sách; không tự đăng xuất B. | `AC-ACC-53` | S09 |
| UX-ACC-29 | Cho trước phiên hết hạn/bị thu hồi; khi truy cập chức năng bảo vệ; thì hiện trạng thái cần đăng nhập lại (401), không tiếp tục hành động cũ. | `AC-ACC-08`, `54` | S11 |
| UX-ACC-30 | Cho trước Customer còn đăng nhập nhưng không đủ quyền; khi truy cập Admin; thì hiện 403 riêng, không thông báo nhầm là hết phiên. | `AC-ACC-13` | S14 |
| UX-ACC-31 | Cho trước tài khoản bị vô hiệu hóa; khi gọi chức năng được bảo vệ; thì hiển thị trạng thái bị chặn và hướng dẫn hỗ trợ, không tự làm mới phiên vô hạn. | `AC-ACC-55` | S15 |
| UX-ACC-32 | Cho trước màn mobile; khi tăng kích thước chữ / dùng bàn phím ảo; thì không mất label, thông báo lỗi, nút chính và khả năng cuộn. | Yêu cầu UX | S01..S15 |
| UX-ACC-33 | Cho trước người dùng dùng bàn phím; khi đi qua tab/field/OTP/nút; thì focus rõ, thứ tự hợp lý, không phải tab 6 lần để nhập một OTP. | Yêu cầu UX | S01..S15 |
| UX-ACC-34 | Cho trước thông báo lỗi/thành công; khi trình đọc màn hình công bố; thì có câu chữ mô tả, không chỉ màu và không đọc countdown mỗi giây. | Yêu cầu UX | S01..S15 |
| UX-ACC-35 | Cho trước Admin được phép vô hiệu hóa một Customer; khi chọn hành động và xác nhận; thì UI phản ánh trạng thái đang xử lý/kết quả và không tuyên bố thành công nếu API chưa xác nhận. | `AC-ACC-55` | S16 |
| UX-ACC-36 | Cho trước tài khoản email đang chờ xác minh; khi đăng ký lại; thì có thể tiếp tục quy trình xác minh, không khiến người dùng hiểu rằng đã tạo tài khoản thứ hai. | `AC-ACC-40` | S03, S05 |
| UX-ACC-37 | Cho trước email chờ qua 7 ngày; khi tiếp tục xác minh; thì có trạng thái quá hạn và CTA đăng ký lại, không tuyên bố đã xóa dữ liệu nếu chưa biết. | `AC-ACC-44` | S18 |
| UX-ACC-38 | Cho trước người dùng muốn khôi phục bằng số điện thoại đã mất SIM; khi yêu cầu; thì không có nút tự cấp quyền; có hướng liên hệ hỗ trợ nếu kênh đó thực sự tồn tại. | `AC-ACC-62` | S02/S17 |
| UX-ACC-39 | Cho trước form thông báo dài/ngắn; khi hiển thị trên desktop/mobile; thì hộp thông báo ôm nội dung, tự xuống dòng, không tràn/che thành phần khác. | Yêu cầu UX | Các màn có Alert |
| UX-ACC-40 | Cho trước hệ thống bị lỗi mạng khi gửi email/SMS; khi trả kết quả không rõ; thì UI không hiển thị “đã xác minh” hay “đã gửi đến thiết bị” một cách sai sự thật. | `AC-ACC-24`, `32` | S03–S08, S12 |

---

## 9. Chênh lệch đã phát hiện ở baseline: BA ↔ Figma ↔ Frontend (xem cập nhật §13)

| ID | Mức | Chênh lệch có bằng chứng | Cần xử lý trước khi tuyên bố UX đã hoàn chỉnh |
|---|---|---|---|
| GAP-01 | P0 | Figma D11 ghi **“Hết hạn phiên và sai quyền”** nhưng nội dung hiện chỉ hướng dẫn đăng nhập lại; thiếu trải nghiệm 403 độc lập. | Vẽ UX-S14 (403) riêng, giữ UX-S11 (401). |
| GAP-02 | P0 | Figma có màn **yêu cầu quên mật khẩu** (D08), chưa có **form mật khẩu mới sau khi mở link**. | Vẽ UX-S13 và trạng thái link reset lỗi/thành công. |
| GAP-03 | P0 | Figma D07/M03 dùng chung OTP, chưa có biến thể phân biệt **đăng ký tự đăng nhập** và **đăng nhập tài khoản có sẵn**. | Bổ sung nội dung và điều hướng theo nguồn OTP, tránh tạo tài khoản ngầm. |
| GAP-04 | P0 | Figma không có trạng thái **tài khoản bị vô hiệu hóa** riêng. | Vẽ UX-S15 và hành động hỗ trợ phù hợp. |
| GAP-05 | P0 | Figma chưa thể hiện màn **Admin vô hiệu hóa tài khoản** trong bộ Auth; repo có các trang Admin Users cần đối chiếu. | Bổ sung handoff UX-S16 hoặc liên kết/biến thể đúng trong thiết kế Admin hiện có. |
| GAP-06 | P1 | Mobile Figma chỉ có 6 màn minh họa, chưa có từng màn/biến thể mobile tương ứng cho mọi luồng desktop. | Hoàn thiện các khung mobile cần thiết hoặc ghi rõ component responsive tái dùng và kiểm thử. |
| GAP-07 | P1 | Figma chưa thể hiện đầy đủ: link đã dùng/link mới thay thế, email chờ quá 7 ngày, giới hạn SMS 10 lần/ngày, thông báo mất mạng/không chắc email/SMS đã đến. | Thêm states/variants hoặc mẫu dùng chung trước nghiệm thu thiết kế. |
| GAP-08 | P0 | `RegisterView.vue` hiện yêu cầu **họ tên**, dùng **mật khẩu ≥6 ký tự**, đăng ký xong `auth.register` tạo phiên và chuyển trang chủ. BA mới chỉ bắt buộc email+mật khẩu **12–128**, link xác minh + mật khẩu chính thức, không auto login email. | Giao Architect/Frontend cập nhật hành vi theo BA; không coi code cũ là quy tắc. |
| GAP-09 | P0 | `LoginView.vue` hiện chỉ có email/password; `authStore` hiện login/register email; chưa thấy phone/SMS trong các file đã đọc. | Bổ sung trải nghiệm Phone + OTP, giữ tuân thủ API BA. |
| GAP-10 | P0 | `authStore.logout()` hiện xóa phiên giao diện ngay và gọi `authApi.logout()` không chờ, lỗi bị bỏ qua. BA yêu cầu **phiên hiện tại vô hiệu ngay ở API**. | Architect/Security xác minh cơ chế, UI cần phân biệt kết quả xác nhận hoặc chưa xác nhận; không tự khẳng định đã thu hồi. |
| GAP-11 | P1 | Nút chữ trắng trên nền thương hiệu `#EE4D2D` có tương phản khoảng 3,66:1, thấp hơn ngưỡng 4,5:1 của chữ thường. | Kiểm tra điều chỉnh màu CTA/chữ, trạng thái focus/disabled và đo lại trong thiết kế thực tế. |
| GAP-12 | P1 | Figma D03 yêu cầu mật khẩu đăng ký rồi D06 yêu cầu **thiết lập mật khẩu lại** sau xác minh — đúng phạm vi BA nhưng có nguy cơ làm người dùng khó hiểu. | Ghi rõ “mật khẩu ban đầu chưa có hiệu lực” và bước tạo mật khẩu thực tế; *nếu muốn bỏ bước đầu*, phải sửa BA bằng quyết định mới, không tự ý bỏ. |
| GAP-13 | P1 | Chưa có đường dẫn UI tới hỗ trợ mất SIM chắc chắn tồn tại; BA chỉ cho khóa/chờ hỗ trợ trong demo. | Chỉ hiện CTA tới trung tâm hỗ trợ nếu thực tế có kênh được vận hành, nếu chưa có thì chỉ thông báo trung thực. |

**Tính chất kiểm tra:** Bảng trên là kết quả **đối chiếu tĩnh** giữa BA, Figma và các file frontend đã xem; không phải báo cáo chạy E2E hay kiểm thử khả dụng thực tế.

---

## 10. Hợp đồng bàn giao cho Architect, Frontend và QA

### 10.1. Architect phải xác nhận dữ liệu/trạng thái UI cần biết (không tự ấn định API)

| Nhóm | Điều UI cần nhận từ hệ thống | Lý do |
|---|---|---|
| Đăng ký/xác minh email | Trạng thái tiếp nhận yêu cầu, đường xử lý link hợp lệ/hết hạn/đã dùng, điều kiện đặt mật khẩu, cooldown và hạn gửi lại | Hiển thị đúng FL-01, FL-02, tránh lộ tài khoản |
| OTP SMS | `purpose` (đăng ký/đăng nhập), trạng thái yêu cầu, hạn hiệu lực, cooldown/thời điểm retry hợp lệ, số lần còn lại nếu an toàn cung cấp | Đếm thời gian và điều hướng chính xác, không dựng giả số lượt |
| Kết quả tạo tài khoản/đăng nhập | Danh tính Customer đã xác thực, trạng thái cần đăng nhập hay tự đăng nhập theo loại đăng ký | Phân biệt email vs điện thoại |
| Phiên | Hết hạn do idle/absolute, bị thu hồi, tài khoản bị vô hiệu hóa; kết quả thu hồi phiên hiện tại | Không hiển thị “đã đăng xuất an toàn” khi API chưa xác nhận |
| Quyền | Trạng thái chưa xác thực (401) và không đủ quyền (403) riêng biệt | Chọn đúng màn hình khôi phục |
| Khôi phục mật khẩu | Kết quả yêu cầu trung tính, link reset hợp lệ/không hợp lệ, điều kiện đặt mật khẩu, trạng thái sau reset | Không công khai tồn tại tài khoản; không tự phát hành phiên |
| Ngữ cảnh điều hướng | Đường quay lại sản phẩm hợp lệ, kiểm tra redirect an toàn | Không chuyển sang trang bên ngoài tùy ý |
| Hồ sơ | Tên hiển thị và định danh email/phone chỉ đọc | Không tự đổi/login-link định danh |
| Giới hạn/chống lạm dụng | Thời gian gửi lại cho phép/đã bị chặn do hệ thống thực sự, ngữ cảnh lỗi không tiết lộ danh tính | UI không suy đoán hoặc lệch chính sách |

**Không kết luận tên endpoint, mã lỗi riêng hay response JSON trước khi có Technical Design.** Các tên trường như `purpose`, `retryAfter` ở đây chỉ là *khái niệm dữ liệu cần có*, không phải hợp đồng API bắt buộc.

### 10.2. Frontend cần triển khai

- Tạo routing/trang tương ứng UX-S01..S18 hoặc tài liệu ánh xạ tương đương; những route ví dụ dưới đây chỉ là đề xuất cần rà soát với Vue Router hiện tại: `/login`, `/register`, `/verify-email`, `/auth/email/setup-password`, `/auth/phone/verify`, `/forgot-password`, `/reset-password`, `/account`, `/forbidden`.
- Xây dựng các thành phần chia sẻ ở §6.3, không sao chép 18 form tĩnh như 18 trang code rời nếu có thể dùng component/variant.
- Phối hợp Architect để thực hiện 401/403/disabled, thu hồi session ở API và trạng thái bất định khi mạng lỗi.
- Điều hướng an toàn tới sản phẩm; tuyệt đối không tự thêm giỏ sau xác thực.
- Giữ tiếng Việt nhất quán, quản lý nhập liệu, trạng thái form, thông báo và xác minh OTP.
- Dùng tiêu chí UX-ACC-xx và AC-ACC-xx để viết tests; không tự thêm Google/Facebook/Account Linking.

### 10.3. QA kiểm thử

- **Kiểm thử hành trình**: FL-01..FL-08, cả email/điện thoại, nút Back/refresh, gửi trùng, mạng lỗi, đã đăng nhập/hết phiên.
- **Kiểm thử tương tác**: bộ đếm, giới hạn, link cũ, mật khẩu không khớp, OTP hết 5 phút, dùng lại mã, nhập sai 5 lần.
- **Kiểm thử tính riêng tư**: nội dung phản hồi trung tính, không lộ tài khoản, không gộp dữ liệu, không tự thêm giỏ.
- **Kiểm thử responsive/accessibility**: 320/390px và desktop, zoom chữ lớn, keyboard, screen reader, đo contrast.
- Gắn ít nhất một bài kiểm thử vào mỗi **UX-ACC-01..40**; không báo “Pass” nếu chưa chạy thực tế.

### 10.4. Checklist bàn giao UX và phần việc còn lại

- [x] Áp dụng Skill `ui-ux-design`; đối chiếu BA v1.0, Figma đã tạo và source frontend hiện trạng.
- [x] Có hành trình chính/lỗi, thao tác CTA, trạng thái UI, microcopy, responsive, accessibility, UX AC và handoff.
- [x] Chỉ rõ phần **đã có trong Figma** và các biến thể **chưa vẽ**.
- [x] Đã bổ sung **khung desktop** cho các GAP P0 (UX-S13..S17, phân biệt 401/403); 15 khung desktop mới cần kiểm tra ảnh hoàn chỉnh sau khi có lại quyền Figma MCP.
- [ ] Bổ sung đủ biến thể **mobile** và kiểm chứng responsive/accessibility thực tế.
- [x] Chủ dự án **đã duyệt** nội dung UX v1.0 và các đề xuất trình bày/microcopy (05/11/2026).
- [ ] Architect xác nhận dữ liệu/contract, cơ chế thu hồi phiên và các rủi ro P0 từ BA trước khi thực hiện code.
- [ ] Frontend/QA kiểm thử thực tế sau khi triển khai; không tự xem đây là kết quả đã hoàn thành.

---

## 11. Bảng thuật ngữ

| Thuật ngữ | Nghĩa tiếng Việt / giải thích |
|---|---|
| UX (User Experience) | Trải nghiệm người dùng: luồng thao tác, phản hồi, dễ dùng, cách khôi phục khi lỗi |
| UI (User Interface) | Giao diện: màn hình, nút bấm, form, màu, font, bố cục |
| UX Spec | Đặc tả trải nghiệm: hành vi, điều hướng, trạng thái UI và tiêu chí nghiệm thu |
| BA Spec | Đặc tả nghiệp vụ: hệ thống cần làm gì, các điều kiện/phạm vi đã được chốt |
| Figma Frame | Khung màn hình trong Figma; có thể chỉnh sửa, không đồng nghĩa có tương tác thật |
| Wireframe | Phác thảo cấu trúc màn hình, thứ tự nội dung và hành động |
| CTA (Call To Action) | Nút/hành động chính hướng người dùng sang bước tiếp theo |
| OTP | Mã xác thực dùng một lần gửi qua SMS, đúng mục đích và thời hạn |
| Cooldown | Thời gian chờ trước khi được gửi lại mã/liên kết |
| Microcopy | Câu chữ ngắn hướng dẫn/thông báo trong giao diện |
| Loading / Disabled | Đang xử lý / tạm không cho thao tác |
| Inline error | Thông báo lỗi xuất hiện ngay gần trường nhập |
| Empty / Success / Error | Trạng thái không có dữ liệu / thành công / lỗi |
| Responsive | Bố cục thích ứng với kích thước màn hình |
| Accessibility | Khả năng tiếp cận cho người dùng bàn phím, trình đọc màn hình, nhu cầu khác nhau |
| WCAG | Bộ hướng dẫn về khả năng tiếp cận nội dung web; ở đây dùng làm mục tiêu thiết kế, chưa xác minh đạt |
| 401 / 403 | Cần xác thực hợp lệ / Đã xác thực nhưng không có đủ quyền |
| Account enumeration | Việc suy ra tài khoản/email/số có tồn tại hay không từ thông báo/hành vi |
| Session | Phiên đăng nhập; có thể bị hết hạn hoặc thu hồi |
| Source of truth | Nguồn quyết định chính thức; ví dụ hạn OTP phải được máy chủ kiểm tra, không dựa riêng đồng hồ UI |

---

## 12. Ghi nhận phê duyệt

- **Nguồn BA:** Chủ dự án đã phê duyệt các quyết định Account & Authentication trong BA Spec v1.0, gồm 29 phương án A và các quyết định cũ.
- **UX v1.0:** **APPROVED** theo xác nhận trực tiếp từ chủ dự án.
- **Người duyệt UX:** Chủ dự án.
- **Ngày / bằng chứng duyệt UX:** 05/11/2026 — "tôi thấy phần UX Spec ổn rồi, hãy thực hiện các bước tiếp theo cho tôi" (trao đổi dự án Shopee Clone).
- **Phạm vi duyệt:** Nội dung hành vi và tiêu chí UX; **không** xác nhận toàn bộ frame mobile đã vẽ, màn hình đã kiểm thử, hoặc Technical Design đã được duyệt.
- **Yêu cầu trước khi giao Codex Frontend:** Duyệt UX Spec, giải quyết GAP P0 trong Figma và xác nhận yêu cầu data/contract với Architect; sau đó đặt bản đã duyệt tại `docs/features/MVP-ACC/UX_SPEC.md` và liên kết tài liệu Figma.

---

## 13. Phụ lục cập nhật Figma sau khi viết UX v1.0 (05/11/2026)

Đã tạo **15 màn hình desktop bổ sung** trực tiếp trong [file Figma hiện tại](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I), gồm 12 màn Auth/Customer và 3 màn Admin. Màn hình cũ được giữ nguyên, ngoại trừ việc đổi tên một số khung để tách mục đích 401/OTP. **Chỉ xác nhận API đã trả về ID khung được tạo**, chưa có kiểm chứng screenshot cuối cho khung mới do gói Starter đạt giới hạn công cụ.

| Khung | Trạng thái | UX tham chiếu | Figma |
|---|---|---|---|
| D13 | Đặt mật khẩu mới từ reset link | UX-S13 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-38) |
| D14 | Link reset lỗi/đã hết hạn | UX-S13 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-67) |
| D15 | Reset mật khẩu thành công | UX-S13 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-89) |
| D16 | Không đủ quyền 403 (tách khỏi 401) | UX-S14 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-111) |
| D17 | Tài khoản bị vô hiệu hóa | UX-S15 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-134) |
| D18 | OTP đăng nhập tài khoản có sẵn | UX-S17 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-156) |
| D19 | OTP đăng nhập thành công | UX-S17 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-192) |
| D20 | OTP đăng ký thành công (tự đăng nhập) | UX-S07 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-214) |
| D21 | Email chờ đăng ký quá 7 ngày | UX-S18 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-236) |
| D22 | OTP sai hoặc hết hạn | UX-S07 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-258) |
| D23 | Lỗi mạng, chưa rõ kết quả | UX-S07/UX-ACC-40 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-294) |
| D24 | Admin xác nhận khóa tài khoản | UX-S16 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=23-53) |
| D25 | Admin khóa tài khoản thành công | UX-S16 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=23-110) |
| D26 | Admin khóa tài khoản thất bại | UX-S16 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=23-158) |
| D27 | Mất SIM, hướng dẫn hỗ trợ an toàn | UX-S15 | [Mở](https://www.figma.com/design/tq0Fvp8Ub2rpFZAbxT8B1I?node-id=21-318) |

**Chưa hoàn tất:** 21 biến thể mobile tương ứng chưa tạo do Figma MCP Starter đạt giới hạn. GAP-01..05 có **khung desktop** được bổ sung nhưng không được tự đánh dấu QA pass; GAP-06 còn mở; GAP-08..10 vẫn là chênh lệch code cần Architect/Implementers xử lý. Khi thực hiện Frontend, dùng yêu cầu responsive đã duyệt trong §5/§7 ngay cả khi thiếu khung mobile.
