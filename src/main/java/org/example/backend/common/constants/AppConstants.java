package org.example.backend.common.constants;

public class AppConstants {
    public static final String ROLE_USER = "USER";

    // Roles trong Scrum
    public static final String SCRUM_MASTER = "SCRUM_MASTER";
    //Người chịu trách nhiệm về giá trị của sản phẩm và định hướng dự án.
    //Quản lý Backlog, ưu tiên thứ tự Task, phê duyệt kết quả Sprint và có quyền cao nhất trong việc thay đổi yêu cầu.
    public static final String PRODUCT_OWNER = "PRODUCT_OWNER";
    //Người đảm bảo quy trình Scrum được vận hành đúng và tháo gỡ các vật cản cho team.
    //Tạo và quản lý Sprint, cấu hình bảng Kanban, theo dõi vận tốc (Velocity) của team và quản lý thành viên trong dự án.
    public static final String DEVELOPER = "DEVELOPER";
    //Những người trực tiếp thực hiện công việc kỹ thuật để tạo ra sản phẩm.
    //Nhận Task (Assign), cập nhật trạng thái công việc trên bảng Kanban, viết Comment và báo cáo tiến độ hàng ngày.
    public static final String PROJECT_PREFIX = "PROJECT_";

    // Trạng thái Task
    public static final String TASK_TODO = "TODO";
    public static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    public static final String TASK_REVIEW = "REVIEW";
    public static final String TASK_DONE = "DONE";


}
