# 🎬 Hệ thống Quản lý Rạp chiếu phim Đa chi nhánh

Dự án phần mềm học phần (kéo dài 10 tuần) nhằm xây dựng một hệ thống quản lý chuỗi rạp chiếu phim toàn diện, từ khâu đặt vé, lên lịch chiếu đến vận hành hệ thống đa chi nhánh.

## 1. Nền tảng Công nghệ (Tech Stack)
* **Ngôn ngữ:** Java 21 (LTS)
* **Framework:** Spring Boot (Spring Web MVC, Spring Data JPA, Lombok)
* **Giao diện (View):** Thymeleaf, HTML/CSS/JS
* **Cơ sở dữ liệu:** SQL Server
* **Công cụ Build:** Maven
* **Môi trường phát triển:** IntelliJ IDEA, SQL Server Management Studio (SSMS)

## 2. Danh sách Thành viên
* **Trưởng nhóm:** Nguyễn Mạnh Cường 
* **Thành viên 2:** Cao Văn Hùng
* **Thành viên 3:** Trịnh Hoàng Dũng
* **Thành viên 4:** Vũ Đăng Thành
* **Thành viên 5:** Trịnh Hoàng Đức

## 3. Cài đặt Môi trường (Local Setup)
### Yêu cầu môi trường
* JDK 21 đã được cài đặt và cấu hình biến môi trường.
* SQL Server & SQL Server Management Studio (SSMS).
* IDE: IntelliJ IDEA.
* Git.

**Bước 1: Clone mã nguồn**
`git clone https://github.com/macune/SWP391_Project-Group.git

**Bước 2: Thiết lập Cơ sở dữ liệu**
1. Mở SQL Server Management Studio (SSMS).
2. Mở file `database/CinemaManagementDB.sql` và chạy toàn bộ script để khởi tạo cấu trúc dữ liệu.

**Bước 3: Cấu hình kết nối (IntelliJ)**
1. Mở file `src/main/resources/application.properties`.
2. Thay đổi thông số `spring.datasource.password` khớp với mật khẩu tài khoản `sa` (hoặc tài khoản tương đương) trên máy cá nhân.
3. Chạy `CinemaManagementApplication.java` và truy cập `http://localhost:8080`.

## 4. Kỷ luật Git & Quy trình Code (Bắt buộc)
* **Quy tắc Nhánh:** Tuyệt đối không code trực tiếp trên nhánh `main`. Mọi thay đổi phải nằm trên nhánh riêng xuất phát từ `main` mới nhất (Ví dụ: `feature/quan-ly-ve` hoặc `fix/loi-hien-thi`).
* **Quy trình Gộp code (Pull Request):** Mã nguồn chỉ được đưa vào dự án thông qua Pull Request. Cần ít nhất 1 thành viên khác phê duyệt (Approve). **Trưởng nhóm (Cường) là người duy nhất nắm quyền bấm nút Merge.**
* **Quản lý Database:** Thư mục `database/` được kiểm soát bởi `CODEOWNERS`. Mọi lệnh T-SQL thay đổi cấu trúc bảng phải được cập nhật vào `CinemaManagementDB.sql` và phải được Trưởng nhóm review trực tiếp.
* **Bảo toàn Lịch sử:** Tính năng Force Push đã bị khóa. Nghiêm cấm sử dụng `git reset --hard` trên các nhánh chung. Để hoàn tác một commit đã lọt lên `main`, bắt buộc phải dùng lệnh `git revert`.