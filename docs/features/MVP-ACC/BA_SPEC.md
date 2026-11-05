# ĐẶC TẢ NGHIỆP VỤ — TÀI KHOẢN VÀ XÁC THỰC (MVP-ACC)

| Thuộc tính | Giá trị |
|---|---|
| Sản phẩm | Shopee Clone — MVP R0 |
| Phiên bản | **1.0**, ngày 05/11/2026 |
| Trạng thái | **APPROVED — Nghiệp vụ đã được chủ dự án duyệt**; chưa xác nhận khả thi kỹ thuật hoặc nghiệm thu code |
| Phạm vi vai trò | Khách chưa đăng nhập (Guest), Khách hàng (Customer), Quản trị viên (Admin) |
| Ngoài MVP | Seller, Google/Facebook, liên kết/gộp tài khoản |
| Căn cứ | Các quyết định trong cuộc trao đổi BA; 29 lựa chọn A mới nhất và các quyết định đã duyệt trước đó |
| Phụ trách | Chủ dự án (phê duyệt nghiệp vụ); ChatGPT (tổng hợp bản đặc tả) |
| Người phê duyệt / bằng chứng | Chủ dự án; phê duyệt toàn bộ 29 phương án A và D-ACC-12 cùng quyết định trước đó trong trao đổi BA trước 05/11/2026; bản BA v1.0 đã được chốt |
| GitHub Issue (Architect) | [#43](https://github.com/duvanan/shopee/issues/43) |
| UX Spec kèm theo | [`UX_SPEC.md`](UX_SPEC.md) |

> **Đính chính số lượng:** Danh sách được giới thiệu là 27 lựa chọn thực tế có **29 dòng**: 7 email + 7 điện thoại + 9 phiên/tài khoản + 6 dữ liệu/khôi phục. Phê duyệt “tất cả phương án A” được áp dụng cho **cả 29 dòng**, cộng với `D-ACC-12` và các quyết định đã chốt từ trước. Đây là bản đặc tả **nghiệp vụ**, không tự ấn định database, JWT, Redis, kiến trúc microservices hay nhà cung cấp SMS.

## 1. Mục tiêu, phạm vi và ranh giới

**Mục tiêu:** Người dùng có thể đăng ký, chứng minh quyền kiểm soát email hoặc số điện thoại, đăng nhập đúng tài khoản, mua hàng an toàn và không truy cập trái phép dữ liệu của người khác.

**Trong MVP:**
- Email + mật khẩu: đăng ký, xác minh email bằng **liên kết**, đăng nhập, quên/đặt lại mật khẩu.
- Số điện thoại Việt Nam: đăng ký và đăng nhập bằng **SMS OTP** (mã xác thực dùng một lần).
- Hai tài khoản tạo bằng hai phương thức **độc lập**; không tự liên kết, không tự chuyển giỏ/địa chỉ/đơn hàng.
- Đăng nhập nhiều thiết bị, đăng xuất riêng từng phiên; vô hiệu hóa tài khoản có hiệu lực lên mọi phiên.
- Khách chưa đăng nhập xem được sản phẩm nhưng **không** thêm vào giỏ. Admin quản lý theo quyền được cấp; tài khoản quản trị tách khỏi tài khoản mua hàng.

**Ngoài MVP:** đăng nhập Google/Facebook, Seller, liên kết/gộp tài khoản, đổi email/số điện thoại định danh, tự phục hồi khi mất SIM, tự xóa tài khoản trong bản demo, đăng nhập phone bằng mật khẩu. Trước khi phát hành công khai phải giải quyết quy trình khôi phục khi mất SIM và yêu cầu quyền riêng tư/xóa dữ liệu; việc loại khỏi MVP demo **không** có nghĩa bỏ nghĩa vụ pháp lý hay an toàn.

**Ranh giới nghiệp vụ:** Account quản lý danh tính, xác minh, phiên và quyền; Cart, Address, Order quản lý dữ liệu của **một mã tài khoản**, không suy ra danh tính từ số giao hàng/email liên hệ. Công nghệ thực hiện là trách nhiệm của Architect.

## 2. Danh mục tính năng và ưu tiên

| Mã | Tính năng | Mức | Điều kiện nghiệm thu chính |
|---|---|---|---|
| MVP-ACC-01 | Đăng ký email và mật khẩu | P0 | Không trùng email; chưa kích hoạt trước xác minh |
| MVP-ACC-05 | Gửi, xác minh, gửi lại liên kết email; hoàn tất mật khẩu | P0 | Một lần sử dụng; 24 giờ; giới hạn gửi lại |
| MVP-ACC-06 | Đăng ký số điện thoại với OTP SMS | P0 | Chỉ tạo Customer sau OTP hợp lệ |
| MVP-ACC-07 | Đăng nhập qua số điện thoại và OTP SMS | P0 | Đúng số, đúng mục đích, không tự đăng ký |
| MVP-ACC-02 | Đăng nhập email, đăng xuất, vòng đời phiên | P0 | Thu hồi phiên, đa thiết bị, thời hạn 7/30 ngày |
| MVP-ACC-08 | Quên và đặt lại mật khẩu email | P0 | Quy trình xác minh an toàn, không để lộ tài khoản |
| MVP-ACC-04 | Phân quyền Customer/Admin, vô hiệu hóa tài khoản | P0 | Không vượt quyền; thu hồi tất cả phiên khi vô hiệu hóa |
| MVP-ACC-03 | Xem hồ sơ, đổi tên hiển thị | P1 | Không sửa định danh đăng nhập hay vai trò |

**P0:** bắt buộc trước khi nghiệm thu MVP. **P1:** có thể hoàn thiện sau luồng mua hàng đầu tiên, không được phá vỡ bảo vệ dữ liệu.

## 3. Vai trò và quyền hạn

| Hành động | Guest | Email chưa xác minh / yêu cầu phone chưa hoàn tất | Customer hợp lệ | Admin |
|---|---|---|---|---|
| Xem sản phẩm | Có | Có như Guest | Có | Có |
| Đăng ký, nhận/gửi lại xác minh | Có | Có trong phạm vi quy trình | Không cần | Không áp dụng |
| Đăng nhập Customer | Không | Không | Có | **Không dùng tài khoản Admin để mua** |
| Thêm vào giỏ, checkout | Không | Không | Có | Không bằng tài khoản Admin |
| Xem/sửa giỏ, địa chỉ, đơn riêng | Không | Không | Chỉ của chính tài khoản | Không bằng vai trò Customer |
| Xem hồ sơ, sửa tên hiển thị | Không | Không | Chỉ của chính mình | Theo quyền Admin |
| Quản lý sản phẩm/danh mục | Không | Không | Không | Theo quyền được cấp |
| Tự cấp quyền Admin hoặc gộp tài khoản | Không | Không | Không | Không qua các luồng công khai |

## 4. Câu chuyện người dùng (User Stories)

*Mẫu đọc:* “Với vai trò là ..., tôi muốn ..., để ...”.

| Mã | Câu chuyện người dùng |
|---|---|
| US-ACC-01 | Là khách chưa có tài khoản, tôi muốn đăng ký bằng email và mật khẩu để bắt đầu mua sắm. |
| US-ACC-02 | Là người vừa đăng ký bằng email, tôi muốn nhận liên kết xác minh để chứng minh email thuộc quyền kiểm soát của mình. |
| US-ACC-03 | Là người không nhận được hoặc có liên kết hết hạn, tôi muốn yêu cầu gửi lại để hoàn tất đăng ký. |
| US-ACC-04 | Là khách chưa có tài khoản, tôi muốn đăng ký bằng số điện thoại Việt Nam để không cần email. |
| US-ACC-05 | Là người đăng ký bằng điện thoại, tôi muốn nhận OTP qua SMS để xác minh số của mình. |
| US-ACC-06 | Là người nhận OTP, tôi muốn nhập mã hợp lệ để kích hoạt tài khoản. |
| US-ACC-07 | Là người không nhận được OTP, tôi muốn gửi lại theo giới hạn để tiếp tục đăng ký. |
| US-ACC-08 | Là Customer đã xác minh email, tôi muốn đăng nhập bằng email và mật khẩu để mua hàng. |
| US-ACC-09 | Là Customer đã đăng ký bằng điện thoại, tôi muốn đăng nhập bằng SMS OTP để sử dụng đúng tài khoản. |
| US-ACC-10 | Là người đang đăng nhập, tôi muốn đăng xuất khỏi thiết bị hiện tại để kết thúc phiên đó. |
| US-ACC-11 | Là Customer, tôi muốn được yêu cầu đăng nhập lại khi phiên hết hiệu lực để tránh thao tác không an toàn. |
| US-ACC-12 | Là Customer, tôi muốn xem hồ sơ của mình để kiểm tra thông tin. |
| US-ACC-13 | Là Customer, tôi muốn sửa tên hiển thị mà không ảnh hưởng định danh đăng nhập. |
| US-ACC-14 | Là Admin, tôi muốn sử dụng đúng các chức năng quản trị được cấp để quản lý sản phẩm. |
| US-ACC-15 | Là Customer, tôi muốn giỏ hàng, địa chỉ và đơn hàng của mình không bị tài khoản khác đọc hoặc sửa. |
| US-ACC-16 | Là Guest muốn thêm sản phẩm vào giỏ, tôi muốn được dẫn đến đăng nhập rồi trở lại sản phẩm để tiếp tục mua. |
| US-ACC-17 | Là người sở hữu tài khoản email, tôi muốn đăng nhập vào đúng dữ liệu tài khoản email của mình. |
| US-ACC-18 | Là người sở hữu tài khoản số điện thoại, tôi muốn đăng nhập vào đúng dữ liệu tài khoản điện thoại của mình. |
| US-ACC-19 | Là người có hai tài khoản độc lập, tôi không muốn hệ thống tự ý gộp giỏ hàng và lịch sử đơn. |
| US-ACC-20 | Là người đã quên mật khẩu email, tôi muốn đặt lại mật khẩu qua email đã xác minh để lấy lại quyền truy cập. |
| US-ACC-21 | Là người đăng ký email còn chờ xác minh, tôi muốn tiếp tục quy trình mà không tạo tài khoản trùng hay bị người khác chọn sẵn mật khẩu. |
| US-ACC-22 | Là người mất SIM, tôi muốn biết tài khoản có thể bị khóa chờ xác minh danh tính thay vì bị người lạ tự khôi phục. |
| US-ACC-23 | Là Admin được ủy quyền, tôi muốn vô hiệu hóa tài khoản vi phạm và chặn ngay mọi phiên đang sử dụng. |

## 5. Các quyết định nghiệp vụ đã phê duyệt

Tất cả lựa chọn dưới đây đã được chủ dự án chốt. Các mức thời gian/số lần là **ngưỡng chính sách của MVP**; Architect được đề xuất bổ sung bảo vệ kỹ thuật nhưng không được âm thầm thay đổi trải nghiệm hoặc ngưỡng đã duyệt.

### 5.1. Quyết định chốt trước lượt này

| Mã | Quyết định |
|---|---|
| BR-CART-001 | Guest phải đăng nhập bằng tài khoản Customer mới được thêm vào giỏ. |
| D-ACC-01 | Đăng ký email bắt buộc xác minh email. |
| D-ACC-07 | MVP có email/mật khẩu và phone/SMS OTP. |
| D-ACC-08 | Google/Facebook và liên kết phương thức đăng nhập thuộc V1. |
| D-ACC-10 | Hai phương thức đăng ký độc lập, không tự động liên kết/gộp tài khoản. |
| D-ACC-12 | Đăng ký lại khi đang chờ xác minh thì tiếp tục quy trình cũ, không tạo tài khoản trùng; có kiểm tra chống chiếm tài khoản. |

### 5.2. 29 lựa chọn A mới được duyệt

| Nhóm | Mã | Quyết định đã chốt |
|---|---|---|
| Email | D-ACC-01A | Xác minh bằng **liên kết trong email**. |
| Email | D-ACC-01B | Liên kết có hiệu lực **24 giờ**. |
| Email | D-ACC-01C | Gửi lại cách nhau **ít nhất 60 giây**, tối đa **5 lần gửi lại/giờ**; liên kết mới vô hiệu hóa liên kết cũ. |
| Email | D-ACC-01D | Tài khoản email chờ xác minh tối đa **7 ngày**, sau đó đủ điều kiện dọn dữ liệu chờ theo chính sách. |
| Email | D-ACC-13 | Tiếp tục tài khoản email đang chờ; người chứng minh quyền kiểm soát email **tự thiết lập mật khẩu sau xác minh**, không bị ràng buộc bởi mật khẩu người khác đã nhập trước đó. |
| Email | D-ACC-14 | Kiểm tra email trùng không phân biệt chữ hoa/thường, chuẩn hóa nhất quán. |
| Email | D-ACC-15 | Đăng ký email chỉ yêu cầu email và mật khẩu; dữ liệu cá nhân thêm sau. |
| Điện thoại | D-ACC-09A | OTP **6 chữ số**, hiệu lực **5 phút**, tối đa **5 lần nhập sai/đợt xác minh**. |
| Điện thoại | D-ACC-09B | Chờ **60 giây** giữa các lần gửi OTP; tối đa **5 lần/giờ và 10 lần/ngày cho mỗi số**, có bảo vệ chống lạm dụng bổ sung. |
| Điện thoại | D-ACC-09C | OTP mới phát hành làm mã cũ hết hiệu lực. |
| Điện thoại | D-ACC-09D | **Chỉ tạo tài khoản điện thoại sau OTP đăng ký hợp lệ**; trước đó chỉ có yêu cầu đăng ký đang chờ. |
| Điện thoại | D-ACC-09E | Đăng nhập bằng số chưa đăng ký **không tự tạo tài khoản**; hướng dẫn đăng ký bằng thông báo an toàn. |
| Điện thoại | D-ACC-09F | SMS OTP là phương thức đăng nhập của MVP demo; ghi nhận rủi ro SIM bị chiếm/đổi chủ, đánh giá lại trước phát hành công khai. |
| Điện thoại | D-ACC-09G | Chỉ hỗ trợ số Việt Nam trong MVP; chuẩn hóa định danh về **+84**. |
| Phiên/tài khoản | D-ACC-02 | Xác minh email xong → đăng nhập; xác minh OTP **đăng ký điện thoại** xong → **đăng nhập luôn**. |
| Phiên/tài khoản | D-ACC-03 | Đăng xuất **vô hiệu hóa ngay phiên hiện tại tại cả giao diện và API**. |
| Phiên/tài khoản | D-ACC-04 | MVP có quên và đặt lại mật khẩu của tài khoản email. |
| Phiên/tài khoản | D-ACC-05 | Admin được cấp qua quy trình quản trị tin cậy; không tự đăng ký công khai. |
| Phiên/tài khoản | D-ACC-06 | Tách tài khoản Admin khỏi tài khoản mua hàng Customer. |
| Phiên/tài khoản | D-ACC-16 | Cho phép đăng nhập đa thiết bị; mỗi thiết bị/phiên riêng; đăng xuất chỉ phiên hiện tại. |
| Phiên/tài khoản | D-ACC-17 | Phiên hết hạn sau **7 ngày không hoạt động**, và không vượt **30 ngày từ khi tạo**. |
| Phiên/tài khoản | D-ACC-18 | Khi Admin vô hiệu hóa tài khoản, **mọi phiên** của tài khoản đó ngừng được sử dụng chức năng bảo vệ. |
| Phiên/tài khoản | D-ACC-19 | Mật khẩu tối thiểu **12**, tối đa **128 ký tự**; không bắt buộc phối hợp chữ hoa/số/ký tự đặc biệt. |
| Dữ liệu/UX | D-ACC-11 | Email/số điện thoại liên hệ hoặc giao hàng không tự trở thành định danh đăng nhập. |
| Dữ liệu/UX | D-ACC-20 | MVP chỉ cho đổi **tên hiển thị**; không tự đổi email/số điện thoại đăng nhập. |
| Dữ liệu/UX | D-ACC-21 | Sau đăng nhập từ màn hình sản phẩm, quay lại trang sản phẩm; người dùng **chủ động bấm thêm giỏ**. |
| Dữ liệu/UX | D-ACC-22 | Dùng thông báo **trung tính** cho luồng đăng nhập, gửi lại và khôi phục để hạn chế lộ sự tồn tại tài khoản. |
| Dữ liệu/UX | D-ACC-23 | Mất SIM: **không tự phục hồi** trong MVP demo; khóa/chờ hỗ trợ hoặc xác minh thủ công an toàn. Trước phát hành công khai phải có quy trình xử lý thực tế. |
| Dữ liệu/UX | D-ACC-24 | Không có giao diện tự xóa tài khoản trong bản demo MVP; trước phát hành công khai phải xem xét và đáp ứng yêu cầu quyền riêng tư. |

## 6. Quy tắc nghiệp vụ (Business Rules)

**Quy ước:** Mỗi mã `BR-ACC-xx` là một yêu cầu phải tuân thủ; `BR-CART-001` là quy tắc thuộc Epic Giỏ hàng được tham chiếu ở đây. Các mã `BR-ACC-01` đến `BR-ACC-36` giữ theo bản nháp v0.4; phần bổ sung bắt đầu từ `BR-ACC-37`.

### 6.1. Danh tính, tài khoản độc lập và quyền

| Mã | Quy tắc |
|---|---|
| BR-ACC-01 | Mỗi email định danh chỉ thuộc tối đa một tài khoản theo cách chuẩn hóa được áp dụng nhất quán. |
| BR-ACC-02 | Mỗi số điện thoại định danh chỉ thuộc tối đa một tài khoản hiện hữu tại một thời điểm. |
| BR-ACC-03 | Đăng ký công khai chỉ cấp vai trò Customer, không tự cấp Admin. |
| BR-ACC-04 | Chưa hoàn thành xác minh thì không có quyền Customer cho các tính năng bảo vệ. |
| BR-ACC-05 | Chỉ chủ tài khoản được sử dụng giỏ hàng, địa chỉ, đơn hàng và hồ sơ riêng của tài khoản đó. |
| BR-ACC-06 | Không tự sửa quyền Admin hoặc vai trò qua cập nhật hồ sơ. |
| BR-ACC-07 | Thao tác thêm giỏ cần Customer đăng nhập hợp lệ (tham chiếu `BR-CART-001`). |
| BR-ACC-22 | Không tự liên kết/gộp tài khoản email và điện thoại. |
| BR-ACC-29 | Đăng ký bằng email và đăng ký bằng điện thoại có thể tạo hai tài khoản Customer độc lập. |
| BR-ACC-30 | MVP không có thao tác liên kết hai tài khoản. |
| BR-ACC-31 | Giỏ hàng, địa chỉ, đơn hàng của hai tài khoản không được tự chia sẻ/chuyển/gộp. |
| BR-ACC-32 | Đăng nhập phải chọn đúng tài khoản theo định danh đã được xác minh, không tự tìm tài khoản khác qua dữ liệu liên hệ. |
| BR-ACC-33 | Số điện thoại nhận hàng không tự trở thành định danh đăng nhập. |
| BR-ACC-34 | Email liên hệ/nhận thông báo không tự trở thành định danh đăng nhập. |
| BR-ACC-35 | MVP không chuyển, hợp nhất lịch sử đơn hàng giữa các tài khoản. |
| BR-ACC-40 | Email được so trùng không phân biệt chữ hoa/thường; không suy đoán rằng hai email khác nhau là cùng một địa chỉ khi không có chính sách rõ ràng. |
| BR-ACC-47 | Chỉ chấp nhận số điện thoại Việt Nam làm định danh; lưu/so khớp theo định dạng chuẩn +84. |
| BR-ACC-52 | Admin không được tạo qua đăng ký công khai; phải được cấp qua kênh quản trị tin cậy. |
| BR-ACC-53 | Tài khoản Admin và Customer dùng cho mua hàng là các tài khoản tách biệt trong MVP. |
| BR-ACC-58 | Customer chỉ sửa tên hiển thị trong MVP; không đổi định danh đăng nhập qua hồ sơ. |
| BR-ACC-59 | Sau đăng nhập từ trang sản phẩm, quay về đúng trang; không tự thêm vào giỏ. |
| BR-ACC-60 | Thông báo tại các luồng đăng nhập, gửi lại và khôi phục tránh tiết lộ trái phép tài khoản có tồn tại hay không. |

### 6.2. Đăng ký email, xác minh và đặt lại mật khẩu

| Mã | Quy tắc |
|---|---|
| BR-ACC-08 | Tài khoản đăng ký email phải xác minh email trước khi sử dụng. |
| BR-ACC-09 | Sau đăng ký email, tài khoản ở trạng thái **CHỜ_XÁC_MINH**; chưa có phiên Customer đầy đủ. |
| BR-ACC-10 | Chỉ liên kết xác minh đúng tài khoản, hợp lệ, còn hiệu lực mới được chấp nhận. |
| BR-ACC-11 | Liên kết xác minh chỉ có thể được sử dụng thành công một lần. |
| BR-ACC-12 | Có thể gửi lại liên kết xác minh nhưng phải áp dụng giới hạn chống lạm dụng. |
| BR-ACC-13 | Liên kết hết hạn không được kích hoạt tài khoản. |
| BR-ACC-36 | Email đã có tài khoản đang chờ xác minh được tiếp tục quy trình an toàn, không tạo tài khoản mới. |
| BR-ACC-37 | Liên kết email có hiệu lực 24 giờ; gửi lại sau tối thiểu 60 giây và không quá 5 lần gửi lại/giờ; liên kết mới vô hiệu hóa liên kết cũ. |
| BR-ACC-38 | Tài khoản email chưa xác minh sau 7 ngày đủ điều kiện hết hạn/dọn dữ liệu chờ; không tự động tuyên bố là đã xóa nếu việc dọn chưa xảy ra. |
| BR-ACC-39 | Khi tiếp tục đăng ký email chưa xác minh, chỉ người đã chứng minh kiểm soát hộp thư được đặt mật khẩu sử dụng thực tế; mật khẩu nhập trước bước đó không tự trở thành thông tin đăng nhập đang có hiệu lực. |
| BR-ACC-41 | Màn đăng ký email yêu cầu email và mật khẩu; thông tin hồ sơ khác không bắt buộc. Mật khẩu đăng nhập có hiệu lực phải được người kiểm soát email thiết lập sau xác minh. |
| BR-ACC-48 | Hoàn tất xác minh email và thiết lập mật khẩu thì chuyển đến đăng nhập, không tự tạo phiên Customer. |
| BR-ACC-51 | Tài khoản email có chức năng quên/đặt lại mật khẩu qua xác minh an toàn; chỉ người chứng minh kiểm soát email mới có thể thiết lập mật khẩu mới. |
| BR-ACC-57 | Mật khẩu đăng ký/thiết lập lại dài 12–128 ký tự; không ép kiểu phối hợp ký tự đặc biệt/chữ hoa/số. |
| BR-ACC-62 | Bản demo MVP chưa có giao diện tự xóa tài khoản; trước khi mở công khai phải rà soát và đáp ứng yêu cầu bảo vệ dữ liệu/tiếp nhận yêu cầu xóa. |

### 6.3. Đăng ký và đăng nhập bằng SMS OTP

| Mã | Quy tắc |
|---|---|
| BR-ACC-14 | MVP hỗ trợ đăng ký số điện thoại qua OTP. |
| BR-ACC-15 | Phải xác minh đúng số điện thoại bằng OTP trước khi có tài khoản Customer hoạt động. |
| BR-ACC-16 | OTP có thời gian hiệu lực; hết hạn thì không được chấp nhận. |
| BR-ACC-17 | OTP dùng thành công một lần cho đúng số, đúng mục đích, đúng yêu cầu. |
| BR-ACC-18 | OTP sai không được đăng ký/đăng nhập thành công. |
| BR-ACC-19 | Có giới hạn nhập sai và gửi lại OTP. |
| BR-ACC-20 | Số điện thoại đã có tài khoản có thể đăng nhập bằng OTP. |
| BR-ACC-21 | OTP đăng nhập và OTP đăng ký không hoán đổi cho nhau. |
| BR-ACC-23 | Mất SIM/đổi chủ số có rủi ro chiếm tài khoản; không cho tự khôi phục chỉ từ khai báo của người yêu cầu. |
| BR-ACC-42 | OTP gồm 6 chữ số, hiệu lực 5 phút, tối đa 5 lần nhập sai trên một đợt xác minh. |
| BR-ACC-43 | Gửi OTP cách nhau tối thiểu 60 giây; tối đa 5 lần/giờ và 10 lần/ngày cho mỗi số (tính cả gửi mới và gửi lại); phải có kiểm soát chống lạm dụng bổ sung. |
| BR-ACC-44 | Phát hành OTP mới làm OTP trước đó của cùng số và mục đích hết hiệu lực. |
| BR-ACC-45 | Khi đăng ký số điện thoại, chỉ tạo Customer **sau khi OTP đăng ký hợp lệ**; trước đó chỉ lưu yêu cầu/đợt xác minh đang chờ, không coi là tài khoản chưa xác minh. |
| BR-ACC-46 | Nhập số chưa đăng ký ở luồng đăng nhập không tự tạo tài khoản; giao diện đưa hướng dẫn đăng ký phù hợp và tránh lộ thông tin nhạy cảm. |
| BR-ACC-49 | Sau khi OTP **đăng ký** thành công và tài khoản được tạo, đăng nhập luôn vào chính tài khoản đó. |
| BR-ACC-61 | Mất SIM trong bản demo: không có tự phục hồi; tài khoản có thể khóa/chờ hỗ trợ theo quy trình. Trước phát hành công khai phải có quy trình xác minh danh tính/hỗ trợ thực tế. |

### 6.4. Phiên đăng nhập, bảo vệ tài khoản

| Mã | Quy tắc |
|---|---|
| BR-ACC-24 | Thông tin xác thực không hợp lệ không tạo phiên đăng nhập. |
| BR-ACC-25 | Phiên đã hết hạn hoặc bị thu hồi không được thực hiện chức năng bảo vệ. |
| BR-ACC-26 | Customer không thể thực hiện hành động Admin nếu không có quyền. |
| BR-ACC-27 | Đăng xuất kết thúc phiên hiện tại **ngay tại cả giao diện và API**. |
| BR-ACC-28 | Không tiết lộ mật khẩu, OTP, liên kết xác minh, mã phiên hoặc thông tin xác thực nhạy cảm qua UI/log. |
| BR-ACC-50 | Khi đăng xuất, vô hiệu hóa phiên hiện tại; các phiên hợp lệ trên thiết bị khác **vẫn còn**, trừ khi tài khoản bị vô hiệu hóa. |
| BR-ACC-54 | Cho phép nhiều phiên đăng nhập độc lập của một tài khoản trên các thiết bị. |
| BR-ACC-55 | Phiên hết hiệu lực khi **không hoạt động 7 ngày** hoặc khi đạt **30 ngày kể từ lúc tạo**, tùy mốc nào đến trước. |
| BR-ACC-56 | Khi Admin vô hiệu hóa tài khoản, mọi phiên hiện có đều không được tiếp tục sử dụng chức năng bảo vệ. |

## 7. Trạng thái và luồng nghiệp vụ

### 7.1. Trạng thái tài khoản

| Trạng thái | Giải thích | Được đăng nhập Customer? |
|---|---|---|
| **CHỜ_XÁC_MINH** | Chỉ dành cho tài khoản **email** đã được tạo nhưng chưa xác minh/hoàn tất mật khẩu | Không |
| **ĐANG_HOẠT_ĐỘNG** | Đã hoàn tất quy trình xác minh tương ứng | Có, nếu thông tin xác thực hợp lệ |
| **BỊ_VÔ_HIỆU_HÓA** | Bị vô hiệu hóa qua quy trình quản trị | Không; các phiên cũ cũng bị chặn |

**Phân biệt để tránh mâu thuẫn:** Đối với **đăng ký điện thoại**, trước OTP hợp lệ **chưa có tài khoản Customer**; chỉ có một *yêu cầu đăng ký chờ OTP*. Vì vậy D-ACC-12 áp dụng cho cả hai hình thức theo nghĩa “tiếp tục quy trình đang chờ”, **không** bắt buộc tồn tại account phone chưa xác minh.

### 7.2. Luồng A — Đăng ký email và xác minh

1. Khách nhập email, mật khẩu (12–128 ký tự); hệ thống xác minh dữ liệu và sự trùng lặp theo quy tắc chuẩn hóa.
2. Tạo tài khoản email **CHỜ_XÁC_MINH**; mật khẩu ban đầu **không được dùng để đăng nhập** trước khi người kiểm soát email thiết lập mật khẩu thực tế.
3. Gửi liên kết xác minh hiệu lực 24 giờ. UI thông báo trung tính, không công khai tình trạng tồn tại tài khoản trái phép.
4. Người nhận nhấn liên kết còn hiệu lực; hệ thống kiểm tra đúng yêu cầu và chỉ chấp nhận một lần.
5. Người kiểm soát email **thiết lập mật khẩu thực tế** trong quy trình xác minh an toàn (không mặc định chấp nhận mật khẩu do người khác có thể đã khai báo).
6. Tài khoản trở thành **ĐANG_HOẠT_ĐỘNG**; chuyển sang màn hình đăng nhập, **không tự đăng nhập**.
7. Nếu liên kết hết hạn/đã thay mới hoặc email không đến, người dùng yêu cầu gửi lại theo giới hạn 60 giây và 5 lần/giờ.

### 7.3. Luồng B — Tiếp tục đăng ký email đang chờ

1. Người dùng gửi lại yêu cầu đăng ký bằng email đã có tài khoản **CHỜ_XÁC_MINH**.
2. Không tạo tài khoản mới, không thay mật khẩu hay cấp quyền vì một yêu cầu công khai chưa được xác minh.
3. Cung cấp luồng tiếp tục/gửi lại xác minh với phản hồi trung tính, áp dụng giới hạn.
4. Chỉ khi kiểm soát email và thiết lập mật khẩu an toàn mới được kích hoạt.
5. Sau 7 ngày chờ, dữ liệu đăng ký đủ điều kiện dọn theo chính sách; nếu đã dọn thì yêu cầu hợp lệ mới có thể bắt đầu đăng ký mới.

### 7.4. Luồng C — Đăng ký số điện thoại

1. Người dùng nhập số Việt Nam; hệ thống chuẩn hóa về +84 và kiểm tra điều kiện đăng ký.
2. Khởi tạo **yêu cầu đăng ký chờ OTP**, **chưa tạo Customer**.
3. Gửi mã 6 số qua SMS, hiệu lực 5 phút; áp dụng giới hạn gửi.
4. Nhập mã đúng trong tối đa 5 lần thử sai; nếu gửi mã mới thì mã cũ hết hiệu lực.
5. Hệ thống hoàn tất tạo **một** tài khoản Customer, tránh trùng khi có yêu cầu đồng thời.
6. **Tự động đăng nhập ngay** vào tài khoản mới và chuyển tới điểm tiếp tục hợp lệ.
7. Nếu đăng ký lại khi yêu cầu cũ chưa hoàn tất, tiếp tục quy trình xác minh, không tạo tài khoản trùng.

### 7.5. Luồng D — Đăng nhập email hoặc điện thoại

- **Email:** nhập email + mật khẩu đã thiết lập sau xác minh → kiểm tra tài khoản hoạt động → tạo phiên của đúng tài khoản đó.
- **Điện thoại:** nhập số đã đăng ký → gửi OTP mục đích **ĐĂNG_NHẬP** → nhập mã hợp lệ → tạo phiên của đúng tài khoản điện thoại đó.
- Số chưa đăng ký trong luồng đăng nhập **không** được tự tạo Customer. Thông báo không được lộ thông tin tài khoản ngoài mức an toàn cho phép.
- Tài khoản bị vô hiệu hóa hoặc phiên bị thu hồi không được thực hiện hành động được bảo vệ.

### 7.6. Luồng E — Đăng xuất, đa thiết bị và hết hạn

- Một Customer có thể có các phiên độc lập trên nhiều thiết bị.
- Đăng xuất thiết bị A → phiên A ngừng hiệu lực ngay ở UI và API; phiên B vẫn có hiệu lực nếu chưa hết hạn/bị thu hồi.
- Một phiên tự hết hạn khi không hoạt động 7 ngày hoặc đạt tuổi đời 30 ngày, tùy điều kiện nào xảy ra trước.
- Admin vô hiệu hóa tài khoản → mọi phiên của tài khoản không thể tiếp tục dùng API bảo vệ.

### 7.7. Luồng F — Quên mật khẩu và mất SIM

- **Email:** người dùng yêu cầu đặt lại mật khẩu; hệ thống gửi hướng dẫn an toàn đến email đã xác minh mà không tiết lộ trái phép tài khoản có tồn tại; mật khẩu mới chỉ được đặt sau xác minh hợp lệ. Các phiên cũ sau đặt lại cần Architect đưa phương án bảo vệ và kiểm thử.
- **Mất SIM:** MVP demo không tự động cấp lại quyền truy cập chỉ dựa trên khai báo; có thể khóa chờ hỗ trợ. Trước phát hành công khai, phải có quy trình xác minh danh tính và hỗ trợ khả dụng thực tế.

### 7.8. Luồng G — Guest muốn thêm sản phẩm vào giỏ

1. Guest nhấn “Thêm vào giỏ” → không thêm sản phẩm, chuyển sang đăng nhập.
2. Chọn email/mật khẩu hoặc phone/OTP; nếu chưa có tài khoản thì đăng ký, hoàn tất xác minh tương ứng.
3. Sau khi xác thực, quay lại **đúng trang sản phẩm** (đường chuyển hướng phải an toàn).
4. Customer **tự nhấn** “Thêm vào giỏ” lần nữa; không tự thêm ngầm.

## 8. Tiêu chí nghiệm thu (Acceptance Criteria)

**Quy ước:** Mỗi dòng sử dụng logic **Cho trước — Khi — Thì** và phải có ít nhất một trường hợp kiểm thử. Mã `AC-ACC-01` đến `AC-ACC-39` kế thừa ý nghĩa chính từ v0.4; phần bổ sung bắt đầu ở `AC-ACC-40`.

### 8.1. Đăng ký, đăng nhập email, hồ sơ và quyền

| Mã | Tiêu chí nghiệm thu |
|---|---|
| AC-ACC-01 | Cho trước email hợp lệ chưa dùng; khi đăng ký; thì tạo đúng một tài khoản email **CHỜ_XÁC_MINH**, chưa tạo phiên Customer. |
| AC-ACC-02 | Cho trước email đã có tài khoản; khi đăng ký lại; thì không tạo tài khoản trùng. |
| AC-ACC-03 | Cho trước email hoặc mật khẩu không hợp lệ; khi gửi đăng ký; thì từ chối và không tạo tài khoản. |
| AC-ACC-04 | Cho trước đăng ký công khai; khi hoàn tất; thì không có quyền Admin. |
| AC-ACC-05 | Cho trước email đã xác minh và mật khẩu thực tế đúng; khi đăng nhập; thì có phiên Customer của đúng tài khoản. |
| AC-ACC-06 | Cho trước mật khẩu sai; khi đăng nhập; thì không tạo phiên. |
| AC-ACC-07 | Cho trước phiên hợp lệ; khi đăng xuất; thì phiên hiện tại bị từ chối ở API ngay sau đó. |
| AC-ACC-08 | Cho trước phiên hết hạn hoặc bị thu hồi; khi gọi API bảo vệ; thì thao tác bị từ chối. |
| AC-ACC-09 | Cho trước Customer A đăng nhập; khi xem hồ sơ; thì không nhận thông tin riêng của B. |
| AC-ACC-10 | Cho trước Customer đổi tên hiển thị hợp lệ; khi lưu; thì cập nhật đúng hồ sơ mình. |
| AC-ACC-11 | Cho trước Customer gửi yêu cầu sửa vai trò/quyền; khi xử lý; thì không được tăng quyền. |
| AC-ACC-12 | Cho trước Guest; khi nhấn thêm giỏ; thì không thêm và được yêu cầu đăng nhập. |
| AC-ACC-13 | Cho trước Customer không có quyền Admin; khi gọi trang/API quản trị; thì bị từ chối. |
| AC-ACC-14 | Cho trước Admin có quyền phù hợp; khi thao tác trong phạm vi đó; thì được cho phép. |
| AC-ACC-15 | Cho trước tài khoản A; khi cố xem/sửa dữ liệu riêng của B; thì bị từ chối, không lộ dữ liệu B. |

### 8.2. Xác minh email

| Mã | Tiêu chí nghiệm thu |
|---|---|
| AC-ACC-16 | Cho trước email chưa xác minh; khi đăng nhập/sử dụng chức năng Customer; thì không có quyền Customer đầy đủ. |
| AC-ACC-17 | Cho trước liên kết đúng và còn hiệu lực; khi chủ email hoàn tất xác minh và thiết lập mật khẩu; thì tài khoản ACTIVE đúng một lần. |
| AC-ACC-18 | Cho trước liên kết hết hạn; khi sử dụng; thì không kích hoạt tài khoản. |
| AC-ACC-19 | Cho trước liên kết sai hoặc không thuộc tài khoản; khi sử dụng; thì không kích hoạt. |
| AC-ACC-20 | Cho trước liên kết đã dùng; khi mở lại; thì không tạo lần kích hoạt hoặc quyền bổ sung. |
| AC-ACC-21 | Cho trước yêu cầu gửi lại hợp lệ; khi gửi; thì email được đưa vào quy trình gửi theo giới hạn. |
| AC-ACC-22 | Cho trước hai yêu cầu đăng ký cùng email đồng thời; khi xử lý; thì chỉ có tối đa một tài khoản. |
| AC-ACC-23 | Cho trước tài khoản chưa xác minh; khi gọi API mua hàng; thì bị từ chối. |
| AC-ACC-24 | Cho trước email gửi lỗi; khi đăng ký/xác minh; thì không thông báo sai tài khoản đã ACTIVE và có hướng gửi lại. |
| AC-ACC-25 | Cho trước tài khoản đã ACTIVE; khi có xác minh lặp; thì trạng thái và quyền không sai lệch. |

### 8.3. OTP điện thoại

| Mã | Tiêu chí nghiệm thu |
|---|---|
| AC-ACC-26 | Cho trước số Việt Nam hợp lệ chưa đăng ký; khi bắt đầu đăng ký; thì chỉ tạo yêu cầu chờ OTP, **chưa tạo Customer**. |
| AC-ACC-27 | Cho trước OTP đăng ký hợp lệ; khi xác minh; thì tạo đúng một Customer và đăng nhập vào tài khoản vừa tạo. |
| AC-ACC-28 | Cho trước OTP sai; khi nhập; thì không kích hoạt tài khoản/đăng nhập. |
| AC-ACC-29 | Cho trước OTP quá 5 phút; khi sử dụng; thì bị từ chối. |
| AC-ACC-30 | Cho trước số đã đăng ký; khi hoàn tất OTP mục đích đăng nhập; thì đăng nhập đúng tài khoản số đó. |
| AC-ACC-31 | Cho trước OTP đã dùng thành công; khi dùng lại; thì không tạo phiên mới từ cùng yêu cầu. |
| AC-ACC-32 | Cho trước vượt giới hạn gửi OTP; khi yêu cầu tiếp; thì bị giới hạn và không phát sinh lượt gửi vượt chính sách. |
| AC-ACC-33 | Cho trước hai yêu cầu đăng ký cùng số đồng thời; khi hoàn tất; thì không tạo tài khoản trùng. |

### 8.4. Độc lập tài khoản

| Mã | Tiêu chí nghiệm thu |
|---|---|
| AC-ACC-34 | Cho trước một người đăng ký email A và điện thoại B; khi hoàn tất; thì A và B là hai tài khoản, không tự liên kết. |
| AC-ACC-35 | Cho trước giỏ/đơn của A; khi đăng nhập B; thì không được xem, sửa hoặc chuyển dữ liệu của A. |
| AC-ACC-36 | Cho trước đăng nhập bằng email A; khi xem hồ sơ; thì nhận đúng A, không tự chuyển B. |
| AC-ACC-37 | Cho trước đăng nhập bằng OTP của B; khi xem lịch sử; thì chỉ nhận đơn của B. |
| AC-ACC-38 | Cho trước người dùng tìm chức năng gộp hai tài khoản; khi thao tác trong MVP; thì hệ thống không hỗ trợ hay tự thực hiện. |
| AC-ACC-39 | Cho trước số giao hàng trùng số đăng nhập của tài khoản khác; khi lưu địa chỉ; thì không tự tạo/liên kết danh tính đăng nhập. |

### 8.5. Tiêu chí nghiệm thu bổ sung theo 29 quyết định

| Mã | Tiêu chí nghiệm thu |
|---|---|
| AC-ACC-40 | Cho trước email đang có tài khoản CHỜ_XÁC_MINH; khi đăng ký lại; thì tiếp tục xác minh, không tạo tài khoản mới hoặc thay mật khẩu bằng khai báo chưa xác minh. |
| AC-ACC-41 | Cho trước hai email chỉ khác chữ hoa/thường; khi so trùng; thì nhận diện là cùng định danh theo chính sách. |
| AC-ACC-42 | Cho trước liên kết email được phát hành; khi quá 24 giờ; thì hết hiệu lực. |
| AC-ACC-43 | Cho trước yêu cầu gửi lại email trước 60 giây hoặc vượt 5 lần/giờ; khi gửi; thì bị giới hạn. Khi phát hành liên kết mới, liên kết cũ bị vô hiệu hóa. |
| AC-ACC-44 | Cho trước tài khoản email chờ xác minh đủ 7 ngày; khi đến kỳ xử lý dữ liệu chờ; thì được đưa vào quy trình hết hạn/dọn; không được xem là ACTIVE. |
| AC-ACC-45 | Cho trước đăng ký email; khi nhập email + mật khẩu hợp lệ; thì không bắt buộc họ tên/số điện thoại. Mật khẩu ban đầu không được dùng để chiếm tài khoản trước khi chủ email thiết lập mật khẩu sau xác minh. |
| AC-ACC-46 | Cho trước OTP 6 chữ số; khi sai đủ 5 lần trong một đợt; thì đợt đó không thể tiếp tục thử. OTP đúng chỉ dùng trong 5 phút. |
| AC-ACC-47 | Cho trước số đã nhận OTP; khi yêu cầu gửi tiếp trong 60 giây hoặc vượt 5 lần/giờ, 10 lần/ngày; thì không phát sinh lượt gửi vượt giới hạn. |
| AC-ACC-48 | Cho trước OTP cũ; khi phát hành OTP mới cho cùng số/mục đích; thì OTP cũ không còn được chấp nhận. |
| AC-ACC-49 | Cho trước OTP đăng ký phone đã xác minh thành công; khi hoàn tất; thì chỉ lúc đó Customer mới được tạo và tự đăng nhập đúng Customer mới. |
| AC-ACC-50 | Cho trước số chưa đăng ký; khi bắt đầu luồng đăng nhập; thì không tự tạo tài khoản và hiển thị hướng dẫn an toàn về đăng ký. |
| AC-ACC-51 | Cho trước hai cách nhập tương đương của số Việt Nam; khi chuẩn hóa; thì so trùng nhất quán theo +84. Số ngoài phạm vi Việt Nam bị từ chối hợp lệ. |
| AC-ACC-52 | Cho trước hoàn tất xác minh email và thiết lập mật khẩu; khi kết thúc; thì chuyển tới trang Login, không tự có phiên Customer. |
| AC-ACC-53 | Cho trước tài khoản có phiên A và B; khi đăng xuất A; thì A mất hiệu lực ngay tại API, B vẫn dùng được nếu chưa hết hạn. |
| AC-ACC-54 | Cho trước phiên không hoạt động 7 ngày hoặc tuổi đời 30 ngày; khi sử dụng; thì API từ chối phiên đó. |
| AC-ACC-55 | Cho trước Admin vô hiệu hóa tài khoản; khi bất kỳ phiên cũ nào gọi API bảo vệ; thì đều bị từ chối. |
| AC-ACC-56 | Cho trước tài khoản email được xác minh quyền kiểm soát; khi đặt lại mật khẩu; thì chỉ mật khẩu mới do người có quyền được chọn có hiệu lực; yêu cầu sai/hết hạn không đổi mật khẩu. |
| AC-ACC-57 | Cho trước mật khẩu 11 hoặc 129 ký tự; khi đăng ký/đặt lại; thì không hợp lệ. Mật khẩu 12–128 ký tự không bị từ chối **chỉ vì** thiếu chữ hoa/số/ký tự đặc biệt. |
| AC-ACC-58 | Cho trước khách đăng ký công khai; khi gửi yêu cầu tạo Admin; thì không thể có quyền Admin. Tài khoản Admin không dùng để đặt hàng như Customer. |
| AC-ACC-59 | Cho trước Customer sửa hồ sơ; khi chỉ đổi tên hiển thị thì được phép; khi cố đổi email/số định danh đăng nhập qua hồ sơ thì bị từ chối. |
| AC-ACC-60 | Cho trước Guest bắt đầu từ sản phẩm; khi đăng nhập xong; thì quay về sản phẩm, chưa thêm sản phẩm vào giỏ cho đến khi khách nhấn lại. |
| AC-ACC-61 | Cho trước các yêu cầu đăng nhập/gửi lại/khôi phục với định danh tồn tại hoặc không tồn tại; khi nhận kết quả; thì thông báo không tiết lộ trái phép sự tồn tại tài khoản. |
| AC-ACC-62 | Cho trước Customer mất SIM; khi yêu cầu tự phục hồi trong bản demo; thì không được tự cấp quyền; có thông tin khóa/chờ hỗ trợ phù hợp. |
| AC-ACC-63 | Cho trước bản demo MVP; khi tìm chức năng tự xóa tài khoản; thì chưa có chức năng đó. Trước mở công khai phải có bước đánh giá quyền riêng tư được ghi nhận. |
| AC-ACC-64 | Cho trước tài khoản email và phone riêng; khi có cùng địa chỉ giao hàng hoặc email liên hệ; thì không tự liên kết/gộp. |
| AC-ACC-65 | Cho trước yêu cầu xác minh phone đăng ký chưa hoàn tất; khi gửi lại/đăng ký lại; thì tiếp tục đợt xác minh theo chính sách, không tạo tài khoản trước OTP thành công. |
| AC-ACC-66 | Cho trước mã OTP được tạo cho đăng ký; khi dùng cho đăng nhập (hoặc ngược lại); thì bị từ chối. |

## 9. Ma trận tình huống ngoại lệ (Edge Cases)

| Mã | Tình huống / rủi ro | Kết quả bắt buộc | Liên kết AC |
|---|---|---|---|
| EC-ACC-01 | Đăng ký email khác chữ hoa/thường | Không tạo trùng | 02, 41 |
| EC-ACC-02 | Hai đăng ký email đồng thời | Một tài khoản tối đa | 22 |
| EC-ACC-03 | Người khác đăng ký email nạn nhân với mật khẩu tự chọn | Không chiếm được tài khoản nếu chủ email chỉ mở link; chủ email thiết lập mật khẩu | 40, 45 |
| EC-ACC-04 | Email xác minh không gửi được/vào Spam | Chưa ACTIVE, có hướng gửi lại | 24, 21 |
| EC-ACC-05 | Link hết 24 giờ | Từ chối, không kích hoạt | 18, 42 |
| EC-ACC-06 | Link cũ sau gửi lại | Link cũ vô hiệu | 43 |
| EC-ACC-07 | Link đã dùng hoặc mở song song | Không kích hoạt/cấp quyền trùng | 20, 25 |
| EC-ACC-08 | Gửi lại email trước 60 giây/vượt 5 lần giờ | Không gửi vượt ngưỡng | 43 |
| EC-ACC-09 | Email chờ qua 7 ngày | Đủ điều kiện hết hạn/dọn an toàn, không kích hoạt | 44 |
| EC-ACC-10 | Reset mật khẩu sai/hết hạn/dùng lại | Không đổi mật khẩu trái phép | 56 |
| EC-ACC-11 | Số điện thoại +84 và 0 tương đương | Không tạo trùng | 33, 51 |
| EC-ACC-12 | Số quốc tế ngoài Việt Nam | Từ chối hợp lệ | 51 |
| EC-ACC-13 | SMS đến chậm/mất | Có gửi lại theo hạn mức; không tạo Customer sớm | 26, 47 |
| EC-ACC-14 | OTP sai 5 lần | Khóa đợt xác minh đó | 46 |
| EC-ACC-15 | OTP đã hết 5 phút | Từ chối | 29, 46 |
| EC-ACC-16 | OTP cũ sau gửi mã mới | Từ chối | 48 |
| EC-ACC-17 | Dùng lại OTP thành công hoặc đồng thời | Không tạo phiên/tài khoản lặp từ cùng yêu cầu | 31, 33 |
| EC-ACC-18 | OTP đăng ký dùng để đăng nhập | Từ chối sai mục đích | 66 |
| EC-ACC-19 | OTP vượt 60 giây/5 giờ/10 ngày | Chặn lượt gửi mới | 47 |
| EC-ACC-20 | Đăng nhập số chưa đăng ký | Không tự đăng ký, thông báo trung tính | 50, 61 |
| EC-ACC-21 | Đăng ký phone bỏ dở rồi thử lại | Tiếp tục yêu cầu chờ, không có Customer chưa OTP | 65 |
| EC-ACC-22 | SIM bị đổi chủ hoặc mất SIM | Không tự khôi phục; khóa/chờ quy trình xác minh | 62 |
| EC-ACC-23 | Một người có hai tài khoản | Không tự liên kết/chuyển giỏ, địa chỉ, đơn | 34–38, 64 |
| EC-ACC-24 | Số người nhận hàng trùng số tài khoản khác | Không gán thành định danh đăng nhập | 39, 64 |
| EC-ACC-25 | Guest bấm thêm giỏ trước login | Quay lại sản phẩm, không tự thêm | 12, 60 |
| EC-ACC-26 | User A gọi API đọc giỏ/đơn B | Từ chối, không tiết lộ dữ liệu | 15, 35 |
| EC-ACC-27 | Customer cố gọi API Admin trực tiếp | Từ chối dù UI ẩn nút | 13, 58 |
| EC-ACC-28 | Đăng xuất A khi B còn đăng nhập | A bị vô hiệu ngay, B vẫn hợp lệ | 53 |
| EC-ACC-29 | Phiên idle 7 ngày/tuổi 30 ngày | Bị từ chối | 54 |
| EC-ACC-30 | Admin disable khi nhiều phiên còn sống | Tất cả bị chặn ngay | 55 |
| EC-ACC-31 | API/lệnh gửi OTP đồng thời nhiều node | Vẫn bảo toàn hạn mức và OTP dùng một lần | 32, 46–48 |
| EC-ACC-32 | User enumeration qua khác biệt thông báo/thời gian | Không tiết lộ trái phép tài khoản có tồn tại | 61 |
| EC-ACC-33 | Mật khẩu 12 chữ không có ký tự đặc biệt | Không bị từ chối chỉ vì thiếu loại ký tự | 57 |
| EC-ACC-34 | Mất SMS/email provider hoặc bị chặn gửi | Không đánh dấu xác minh thành công; thông báo khôi phục đúng | 24, 26, 32 |

## 10. Yêu cầu UI/UX bàn giao (không phải thiết kế giao diện chi tiết)

| Màn hình/luồng | Nội dung và trạng thái cần có |
|---|---|
| Đăng ký | Chọn Email hoặc Điện thoại; nhãn tiếng Việt; lỗi dữ liệu, đang gửi, chống nhấn trùng |
| Đăng ký email | Nhập email/mật khẩu, thông báo kiểm tra email (trung tính) |
| Xác minh email | Liên kết thành công/hết hạn/đã dùng; thiết lập mật khẩu sau xác minh; đi tới Login |
| Gửi lại email | Chờ 60 giây, ngưỡng 5 lần/giờ, lỗi dịch vụ gửi |
| Đăng ký điện thoại | Nhập số Việt Nam, nhận OTP, nhập mã, đếm thời hạn và số lượt thử; tự đăng nhập sau thành công |
| Đăng nhập điện thoại | OTP đúng mục đích; số chưa đăng ký không tự tạo tài khoản; hướng dẫn trung tính |
| Đăng nhập email | Email/mật khẩu, chưa xác minh, sai thông tin, tài khoản bị vô hiệu hóa |
| Quên mật khẩu | Gửi yêu cầu và thiết lập lại an toàn, không lộ sự tồn tại tài khoản |
| Hồ sơ | Xem dữ liệu tài khoản; chỉ sửa tên hiển thị; không có liên kết/gộp tài khoản |
| Phiên và phân quyền | Đăng xuất hiện tại, hết hạn phiên, thông báo không đủ quyền, duy trì ngữ cảnh sản phẩm an toàn |

Mỗi màn hình cần kiểm tra khi tải, rỗng, thành công, lỗi mạng, thao tác lặp, điều hướng bàn phím, trình đọc màn hình và màn hình nhỏ trong **UX Spec** riêng. Không suy ra việc mã SMS/email đã tới thật chỉ vì API gửi trả về thành công.

## 11. Rủi ro kỹ thuật cần Architect xác nhận trước khi giao lập trình

| Ưu tiên | Vấn đề / xung đột đã rà soát | Điều Architect phải chứng minh hoặc quyết định kỹ thuật |
|---|---|---|
| P0 | **Đăng ký phone sau OTP** vs **tiếp tục đăng ký đang chờ** | Phân biệt *đợt xác minh đăng ký* và *tài khoản Customer*. Bảo toàn tính duy nhất số khi hai OTP/yêu cầu đồng thời. |
| P0 | **Email bị đăng ký hộ** và mật khẩu ban đầu | Người thực sự kiểm soát email phải tự thiết lập mật khẩu sau xác minh; không được cho người đăng ký hộ dùng mật khẩu đặt trước để chiếm tài khoản. Cần chứng minh cả luồng link bị quét tự động. |
| P0 | **Đăng xuất và vô hiệu hóa ngay** trong mô hình JWT/microservices | Token access còn hạn không được qua Gateway hoặc truy cập API bảo vệ sau thu hồi; nhiều node, Redis lỗi, cache chậm, gọi trực tiếp backend đều phải được xem xét. Cơ chế bảo đảm hiệu lực đúng chính sách phải có test. |
| P0 | **OTP 6 số, dùng một lần** | Nguồn sinh mã, lưu kiểm chứng an toàn, phân biệt mục đích, chống đoán, hết hạn, ngăn dùng lại và yêu cầu đồng thời trên nhiều node. |
| P0 | **Giới hạn gửi OTP/email trên nhiều node** | Giới hạn theo số, tài khoản, IP/thiết bị/rủi ro; chống đồng thời vượt hạn, SMS bombing, chi phí và sự cố dịch vụ. Không giảm giới hạn đã chốt. |
| P0 | **SMS OTP và SIM đổi chủ** | Chứng minh phạm vi chấp nhận rủi ro của bản demo; không xem SMS là bảo đảm danh tính lâu dài; trước phát hành công khai cần phương án bảo vệ và phục hồi. |
| P0 | **Độc lập tài khoản** xuyên Cart/Address/Order | Các API lấy quyền sở hữu từ danh tính đã xác thực, không từ số giao hàng, email liên hệ hay header không tin cậy; test truy cập chéo. |
| P0 | **Đặt lại mật khẩu** | Liên kết một lần, giới hạn gửi, chống enumeration, không rò token; Architect đề xuất và kiểm thử chính sách thu hồi phiên cũ sau đổi mật khẩu trước khi phát hành. |
| P1 | **Chuẩn hóa email/điện thoại** | Quy tắc canonical hóa nhất quán giữa giao diện, API, DB, ràng buộc duy nhất, Unicode/chuỗi rỗng, chuyển 0 ↔ +84; tránh tự gộp email do dấu chấm/dấu cộng thiếu căn cứ. |
| P1 | **Email link 24 giờ và gửi lại** | Link mới vô hiệu link cũ, tránh token lộ qua log, trình duyệt, referrer, email scanner/prefetch; xác nhận một lần có ý thức. |
| P1 | **Phiên idle 7 ngày/absolute 30 ngày** | Định nghĩa hoạt động tính gia hạn, server-side expiry, cơ chế clock/timezone, nhiều thiết bị, lỗi đồng bộ. |
| P1 | **Hết hạn/dọn pending email 7 ngày** | Xác định lịch dọn, dữ liệu có thể xóa, idempotency, đăng ký lại sau cleanup, không xóa nhầm ACTIVE. |
| P1 | **Email/SMS provider lỗi** | Phân biệt “đã yêu cầu gửi”, “nhà cung cấp chấp nhận”, “người dùng nhận”; retry, trạng thái chờ, giám sát và giả lập kiểm thử. Không đánh dấu account active khi chưa xác minh. |
| P1 | **Quy trình Admin và mật khẩu** | Quy trình cấp Admin tin cậy, phân tách Customer/Admin, audit thay đổi quyền, kiểm tra disable/revoke. |
| P1 | **Bảo vệ dữ liệu, quyền xóa, hỗ trợ mất SIM** | Xác nhận ranh giới *demo* so với *phát hành công khai*; chuẩn bị quy trình thủ công và đánh giá yêu cầu pháp lý/quyền riêng tư trước go-live. |

**Điểm không phải mâu thuẫn sau khi làm rõ:**
1. `D-ACC-12` và `D-ACC-09D` cùng đúng: **email** có tài khoản chờ, còn **phone** chỉ có yêu cầu OTP chờ.
2. `D-ACC-13` và yêu cầu “email + mật khẩu khi đăng ký” cùng đúng: mật khẩu nhập ban đầu **không là mật khẩu được dùng để đăng nhập**, chủ email đặt mật khẩu thực tế trong bước xác minh.
3. `D-ACC-03`, `D-ACC-16`, `D-ACC-18` cùng đúng: logout thu hồi **một phiên**, vô hiệu hóa tài khoản thu hồi **mọi phiên**.
4. `D-ACC-09E` và `D-ACC-22` cùng đúng: không tự tạo tài khoản khi đăng nhập số lạ, nhưng cũng không công khai sự tồn tại tài khoản một cách trái phép.
5. `D-ACC-10` không cấm cùng một người có hai tài khoản; nó chỉ cấm **tự liên kết và chia sẻ dữ liệu** ở MVP.

## 12. Điều kiện bàn giao / nghiệm thu

**Cho Architect:** xem danh mục `BR-ACC-*`, `AC-ACC-*`, các quyết định và bảng rủi ro; tạo thiết kế kỹ thuật giải quyết P0 trước khi triển khai. Không thay chính sách nghiệp vụ nếu không xin lại phê duyệt.

**Cho Frontend/UI/UX:** dùng các luồng ở mục 7 và yêu cầu màn hình ở mục 10 để lập UX Spec tiếng Việt; mọi thông báo chưa duyệt câu chữ cụ thể phải tuân thủ không lộ tài khoản.

**Cho QA/Security:** lập ma trận kiểm thử từ từng AC và EC, có bằng chứng chạy thực tế; đặc biệt session revocation, đăng ký đồng thời, OTP dùng lại, đăng ký hộ email và dữ liệu chéo tài khoản.

**Hoàn thành BA:** các quyết định nghiệp vụ đã được chốt. **Chưa có nghĩa là đã implement, build pass, tuân thủ chuẩn bảo mật hoặc sẵn sàng phát hành công khai.** Không tự đóng Issue hay merge PR.

## 13. Bảng thuật ngữ

| Thuật ngữ | Tiếng Việt | Ý nghĩa |
|---|---|---|
| BA (Business Analyst) | Phân tích nghiệp vụ | Xác định nhu cầu, quy tắc và tiêu chí kiểm tra |
| MVP | Phiên bản khả dụng tối thiểu | Phiên bản nhỏ nhất hoàn thiện giá trị chính |
| Epic / Feature | Nhóm tính năng / tính năng | Mức phân nhóm backlog |
| User Story | Câu chuyện người dùng | Ai muốn làm gì, vì sao |
| Business Rule (BR) | Quy tắc nghiệp vụ | Điều hệ thống phải tuân thủ |
| Acceptance Criteria (AC) | Tiêu chí nghiệm thu | Cách xác minh một hành vi đạt yêu cầu |
| Edge Case (EC) | Tình huống ngoại lệ | Tình huống lỗi hoặc ít gặp |
| Guest / Customer / Admin | Khách chưa đăng nhập / Khách hàng / Quản trị viên | Ba vai trò MVP |
| OTP | Mã xác thực dùng một lần | Mã dùng một lần cho đúng mục đích, có thời hạn |
| SMS | Tin nhắn qua mạng di động | Kênh gửi OTP điện thoại |
| Verification Link | Liên kết xác minh | Link chứng minh quyền kiểm soát email |
| Authentication | Xác thực | Xác định đúng danh tính đăng nhập |
| Authorization | Phân quyền | Xác định thao tác được phép |
| Session | Phiên đăng nhập | Trạng thái cho phép dùng chức năng cần xác thực |
| Token / JWT | Thông tin xác thực / mã JWT | Công cụ kỹ thuật có thể dùng để kiểm tra phiên; chưa phải lựa chọn BA |
| Idle / Absolute Timeout | Hết hạn vì không hoạt động / giới hạn tuổi phiên | 7 ngày không hoạt động / 30 ngày kể từ lúc tạo |
| Account Linking / Merging | Liên kết / gộp tài khoản | Ngoài MVP |
| Account Recovery | Khôi phục tài khoản | Lấy lại quyền truy cập khi mất phương thức xác thực |
| Rate Limiting | Giới hạn tần suất | Chống gửi mã/thử sai quá nhiều |
| Account Enumeration | Dò sự tồn tại tài khoản | Suy ra email/số đã đăng ký từ kết quả hệ thống |
| CHỜ_XÁC_MINH / ĐANG_HOẠT_ĐỘNG / BỊ_VÔ_HIỆU_HÓA | Trạng thái tài khoản | Có áp dụng cho Customer email; phone chỉ có tài khoản sau xác minh |

**Tài liệu tham khảo cho phần kiểm soát kỹ thuật (không phải quyết định BA và không phải tuyên bố hệ thống đã tuân thủ):** NIST SP 800-63B-4 (2025); OWASP Email Validation and Verification Cheat Sheet; OWASP Session Management Cheat Sheet. Rà soát pháp lý trước khi triển khai thật tại thị trường mục tiêu.

---

## 14. Ghi nhận bàn giao sang Technical Design

- **Chủ dự án đã duyệt các quyết định nghiệp vụ và BA Spec v1.0.** Việc chấp thuận UX v1.0 được xác nhận trong cuộc trao đổi dự án ngày **05/11/2026** ("tôi thấy phần UX Spec ổn rồi, hãy thực hiện các bước tiếp theo cho tôi").
- Đầu vào UX: [`UX_SPEC.md`](UX_SPEC.md). Công việc Architect: [Issue #43](https://github.com/duvanan/shopee/issues/43).
- **Giới hạn phê duyệt:** Phê duyệt này **không** đồng nghĩa đã phê duyệt Technical Design, chưa cho phép tự thay đổi nghiệp vụ, và không khẳng định implementation đáp ứng. Các rủi ro P0 ở §11 vẫn phải được Architect xử lý hoặc escalated trước khi code.
