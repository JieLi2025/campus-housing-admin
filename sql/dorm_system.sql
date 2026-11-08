/*
 Navicat Premium Dump SQL

 Source Server         : dorm_system
 Source Server Type    : MySQL
 Source Server Version : 50619 (5.6.19)
 Source Host           : localhost:3306
 Source Schema         : dorm_system

 Target Server Type    : MySQL
 Target Server Version : 50619 (5.6.19)
 File Encoding         : 65001

 Date: 27/05/2026 16:35:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
  `admin_type` enum('admin','superadmin') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '管理员类型',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职位',
  `office_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '办公地点',
  `work_hours` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作时间',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '个人简介',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE,
  INDEX `idx_admin_type`(`admin_type`) USING BTREE,
  CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '管理员信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (2, 1, 'superadmin', 0, NULL, NULL, NULL, '09:30-04:30', NULL);
INSERT INTO `admin` VALUES (5, 271, 'admin', 0, '宿舍管理处', '宿舍管理员', '4号楼1010', '09:30-04:30', '楼栋管理员');
INSERT INTO `admin` VALUES (7, 273, 'admin', 0, '宿舍管理处', '宿舍管理员', '3号楼1010', '09:30-04:30', '楼栋管理员');
INSERT INTO `admin` VALUES (8, 274, 'admin', 0, '宿舍管理处', '宿舍管理员', '2号楼1010', '09:30-04:30', '楼栋管理员');
INSERT INTO `admin` VALUES (9, 275, 'admin', 0, '宿舍管理处', '宿舍管理员', '1号楼1010', '08:30-05:00', 'A楼管理员');
INSERT INTO `admin` VALUES (10, 1064, 'admin', 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `admin` VALUES (11, 1068, 'admin', 1, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for announcement
-- ----------------------------
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容',
  `building_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标楼栋ID（JSON数组）',
  `publisher_id` bigint(20) NOT NULL COMMENT '发布者ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '草稿' COMMENT '状态：已发布、已归档',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（倒序排列公告）',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_publisher`(`publisher_id`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE,
  INDEX `idx_announcement_publisher_id`(`publisher_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of announcement
-- ----------------------------
INSERT INTO `announcement` VALUES (4, '宿舍分配', '学生在规定时间内寻找室友组建宿舍，先完成的宿舍可以优先选择宿舍，在规定时间内未完成的宿舍或是个人将由管理员统一分配', '3', 275, '已发布', '2025-12-31 16:00:00', '2025-12-01 15:00:04', 0);
INSERT INTO `announcement` VALUES (22, '宿舍分配', '请同学们尽快寻找室友组建宿舍组', '4', 273, '已归档', '2025-12-15 16:00:00', '2025-12-14 19:09:57', 0);
INSERT INTO `announcement` VALUES (24, '宿舍安全检查通知', '定于本周五下午进行宿舍安全检查，请各位同学提前整理好宿舍卫生。', '3,4', 275, '已发布', NULL, '2025-12-15 01:20:23', 0);

-- ----------------------------
-- Table structure for application
-- ----------------------------
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `building_id` bigint(20) NULL DEFAULT NULL COMMENT '楼栋ID（用于权限过滤）',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '申请类型',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请原因',
  `exchange_student_id` bigint(20) NULL DEFAULT NULL COMMENT '交换学生学号',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '申请状态',
  `admin_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '管理员审批意见',
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '拒绝原因',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（展示申请、申请列表排序）',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除',
  `target_dorm_info` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '目标宿舍信息',
  `assigned_room_id` bigint(20) NULL DEFAULT NULL COMMENT '分配的房间ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE,
  INDEX `idx_application_student_id`(`student_id`) USING BTREE,
  INDEX `idx_application_exchange_student_id`(`exchange_student_id`) USING BTREE,
  CONSTRAINT `fk_app_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '申请表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of application
-- ----------------------------
INSERT INTO `application` VALUES (19, 268, 3, '调宿申请', '与室友作息时间不一致，影响学习', NULL, '待审核', NULL, NULL, '13800138000', '2025-12-15 00:32:12', 0, NULL, NULL);
INSERT INTO `application` VALUES (20, 268, 3, '退宿申请', '因家庭原因需要走读', NULL, 'COMPENSATION_PENDING', '物资清点完成，需赔偿 ¥10.00', NULL, NULL, '2025-12-15 00:32:37', 0, NULL, NULL);
INSERT INTO `application` VALUES (21, 427, 4, '调宿申请', '其他原因: 希望调换宿舍', NULL, '待审核', NULL, NULL, '13131131311', '2025-12-25 10:18:51', 0, '101', NULL);
INSERT INTO `application` VALUES (22, 208, 3, '退宿申请', '【休学/退学】身体原因', NULL, '待审核', NULL, NULL, '13121312131', '2025-12-28 00:57:29', 0, NULL, NULL);
INSERT INTO `application` VALUES (23, 202, 3, '调宿申请', '其他原因: 希望调换宿舍', NULL, '待审核', NULL, NULL, '13121121111', '2025-12-28 12:15:25', 0, '103', NULL);

-- ----------------------------
-- Table structure for building
-- ----------------------------
DROP TABLE IF EXISTS `building`;
CREATE TABLE `building`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '楼栋ID',
  `building_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '楼栋名称',
  `building_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '楼栋编码',
  `building_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '楼栋类型',
  `floor_count` int(11) NOT NULL COMMENT '楼层数',
  `total_rooms` int(11) NOT NULL COMMENT '总房间数',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '详细地址',
  `manager_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '宿管姓名',
  `manager_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '宿管电话',
  `admin_id` bigint(20) NULL DEFAULT NULL COMMENT '管理员ID（关联admin表，一栋楼只能有一个管理员）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述信息',
  `current_phase` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前分配阶段：forming_teams(组建团队)/room_selection(选择房间)/admin_allocation(管理员剩余处理)/completed(已完成)',
  `room_selection_end` datetime NULL DEFAULT NULL COMMENT '选房阶段截止时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_building_code`(`building_code`) USING BTREE,
  INDEX `idx_building_type`(`building_type`) USING BTREE,
  INDEX `idx_building_admin_id`(`admin_id`) USING BTREE,
  INDEX `idx_current_phase`(`current_phase`) USING BTREE,
  CONSTRAINT `fk_building_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '楼栋表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of building
-- ----------------------------
INSERT INTO `building` VALUES (3, 'A栋', 'A', '女', 6, 100, '', 'liadmin', '13800000001', NULL, '', 'admin_allocation', '2025-12-31 00:29:52');
INSERT INTO `building` VALUES (4, 'C栋', 'C', '男', 6, 100, '东区雅园3号', 'wang', '13211111111', NULL, '多专业男生宿舍', 'admin_allocation', '2026-01-16 19:15:56');
INSERT INTO `building` VALUES (5, 'B栋', 'B', '女', 6, 100, NULL, 'zhou', '17267838376', NULL, '', 'admin_allocation', '2025-12-31 20:02:45');
INSERT INTO `building` VALUES (6, 'D栋', 'D', '男', 6, 100, NULL, 'wei', '13143532121', NULL, '男生楼', 'admin_allocation', '2025-12-31 00:29:52');

-- ----------------------------
-- Table structure for building_major_assignment
-- ----------------------------
DROP TABLE IF EXISTS `building_major_assignment`;
CREATE TABLE `building_major_assignment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id` bigint(20) NOT NULL COMMENT '楼栋ID',
  `area_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '区域ID（用于标识同一区域的多条记录）',
  `area_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '区域名称',
  `floors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '楼层列表',
  `major_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业名称',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级名称',
  `class_segment` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '班级分片标识：A/B/C等，未拆分为NULL',
  `grade` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '年级',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '性别要求',
  `allocated_rooms` int(11) NULL DEFAULT 0 COMMENT '分配房间数',
  `assignment_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'class-only' COMMENT '分配模式',
  `student_id_range` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当一个班级被拆分到多个区域时，记录每个分片包含的学号范围，帮助学生了解自己属于哪个分片',
  `room_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '房间类型',
  `allocated_students` int(11) NULL DEFAULT 0 COMMENT '已分配学生数',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '记录状态（软删除标记）：1=生效（正常使用），0=失效（已删除但保留记录）。删除专业分配时不会真正删除数据，而是将status改为0，这样可以保留历史记录并支持数据恢复',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_building_id`(`building_id`) USING BTREE,
  INDEX `idx_major_class`(`major_name`, `class_name`) USING BTREE,
  INDEX `idx_grade_gender`(`grade`, `gender`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_bma_area_id`(`area_id`) USING BTREE,
  INDEX `idx_major_class_segment`(`major_name`, `class_name`, `class_segment`) USING BTREE,
  CONSTRAINT `fk_bma_building` FOREIGN KEY (`building_id`) REFERENCES `building` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 139 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '楼栋-专业-班级分配表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of building_major_assignment
-- ----------------------------
INSERT INTO `building_major_assignment` VALUES (125, 3, 'area-1', '人工', '[1,2,3,4,5,6]', '软件工程', '软工2301', NULL, '2023', '女', 13, 'class-only', '', '二人间', 25, 1);
INSERT INTO `building_major_assignment` VALUES (126, 3, 'area-1', '人工', '[1,2,3,4,5,6]', '软件工程', '软工2401', NULL, '2024', '女', 12, 'class-only', '', '二人间', 24, 1);
INSERT INTO `building_major_assignment` VALUES (127, 3, 'area-1', '人工', '[1,2,3,4,5,6]', '软件工程', '软工2402', NULL, '2024', '女', 13, 'class-only', '', '二人间', 26, 1);
INSERT INTO `building_major_assignment` VALUES (128, 3, 'area-1', '人工', '[1,2,3,4,5,6]', '软件工程', '软工2302', NULL, '2023', '女', 10, 'class-only', '', '二人间', 20, 1);
INSERT INTO `building_major_assignment` VALUES (129, 3, 'area-1', '人工', '[1,2,3,4,5,6]', '人工智能', '人工智能2401', NULL, '2024', '女', 12, 'class-only', '', '二人间', 23, 1);
INSERT INTO `building_major_assignment` VALUES (130, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '人工智能', '人工智能2301', NULL, '2023', '男', 7, 'class-only', '', '四人间', 25, 1);
INSERT INTO `building_major_assignment` VALUES (131, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '人工智能', '人工智能2302', NULL, '2023', '男', 7, 'class-only', '', '四人间', 27, 1);
INSERT INTO `building_major_assignment` VALUES (132, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '人工智能', '人工智能2402', NULL, '2024', '男', 7, 'class-only', '', '四人间', 26, 1);
INSERT INTO `building_major_assignment` VALUES (133, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '数据科学与大数据技术', '数据2301', NULL, '2023', '男', 6, 'class-only', '', '四人间', 24, 1);
INSERT INTO `building_major_assignment` VALUES (134, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '数据科学与大数据技术', '数据2302', NULL, '2023', '男', 7, 'class-only', '', '四人间', 26, 1);
INSERT INTO `building_major_assignment` VALUES (135, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '数据科学与大数据技术', '数据2401', NULL, '2024', '男', 6, 'class-only', '', '四人间', 23, 1);
INSERT INTO `building_major_assignment` VALUES (136, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '数据科学与大数据技术', '数据2402', NULL, '2024', '男', 7, 'class-only', '', '四人间', 25, 1);
INSERT INTO `building_major_assignment` VALUES (137, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '计算机科学与技术', '计科2301', NULL, '2023', '男', 6, 'class-only', '', '四人间', 24, 1);
INSERT INTO `building_major_assignment` VALUES (138, 4, 'area-1', '计算机专业', '[1,2,3,4,5,6]', '计算机科学与技术', '计科2401', NULL, '2024', '男', 6, 'class-only', '', '四人间', 23, 1);

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint(20) NOT NULL COMMENT '会话ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint(20) NULL DEFAULT NULL COMMENT '接收者ID（群聊消息时为NULL）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件URL（图片/文件消息）',
  `quoted_message_id` bigint(20) NULL DEFAULT NULL COMMENT '引用的消息ID',
  `is_recalled` tinyint(1) NULL DEFAULT 0 COMMENT '是否已撤回',
  `is_anonymous` tinyint(1) NULL DEFAULT 0 COMMENT '是否匿名发送：0-否，1-是',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（消息列表排序）',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_time`(`conversation_id`, `created_at`) USING BTREE,
  INDEX `idx_sender`(`sender_id`) USING BTREE,
  INDEX `idx_conversation_sender_created`(`conversation_id`, `sender_id`, `created_at`) USING BTREE COMMENT '会话-发送者-创建时间组合索引，用于未读数计算',
  CONSTRAINT `fk_cm_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 108 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '消息表（聊天内容）' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` VALUES (87, 49, 443, 275, '老师你好', NULL, NULL, 0, 0, '2025-12-04 22:55:56', 0);
INSERT INTO `chat_message` VALUES (88, 51, 443, NULL, '大家好', NULL, NULL, 0, 1, '2025-12-06 00:52:17', 0);
INSERT INTO `chat_message` VALUES (89, 51, 443, NULL, '你们好', NULL, NULL, 0, 0, '2025-12-06 00:52:27', 0);
INSERT INTO `chat_message` VALUES (90, 50, 443, NULL, '大家好我叫苏婷', NULL, NULL, 0, 0, '2025-12-06 00:58:20', 0);
INSERT INTO `chat_message` VALUES (91, 52, 443, 379, '新舍友你好', NULL, NULL, 0, 0, '2025-12-06 00:59:20', 0);
INSERT INTO `chat_message` VALUES (92, 50, 275, NULL, '同学们好！', NULL, NULL, 0, 0, '2025-12-06 01:16:18', 0);
INSERT INTO `chat_message` VALUES (93, 50, 275, NULL, '有同学有问题可以通过联系人私聊我', NULL, NULL, 0, 0, '2025-12-06 01:16:29', 0);
INSERT INTO `chat_message` VALUES (94, 52, 379, 443, '你也好！', NULL, NULL, 0, 0, '2025-12-06 01:55:11', 0);
INSERT INTO `chat_message` VALUES (95, 50, 379, NULL, '好的好的', NULL, NULL, 0, 0, '2025-12-06 01:55:22', 0);
INSERT INTO `chat_message` VALUES (96, 51, 379, NULL, '好好好。', NULL, NULL, 0, 0, '2025-12-06 01:56:46', 0);
INSERT INTO `chat_message` VALUES (98, 52, 443, 379, '450923200309120321', NULL, NULL, 0, 0, '2025-12-06 22:02:14', 1);
INSERT INTO `chat_message` VALUES (99, 55, 576, NULL, '大家好', NULL, NULL, 0, 0, '2025-12-14 19:06:21', 0);
INSERT INTO `chat_message` VALUES (100, 55, 580, NULL, '你们好', NULL, NULL, 0, 0, '2025-12-14 19:06:56', 0);
INSERT INTO `chat_message` VALUES (101, 52, 443, 379, '111', NULL, NULL, 0, 0, '2025-12-14 19:46:15', 0);
INSERT INTO `chat_message` VALUES (102, 59, 602, NULL, '大家好！', NULL, NULL, 0, 0, '2025-12-27 20:22:14', 0);
INSERT INTO `chat_message` VALUES (103, 59, 602, NULL, '你们好！', NULL, NULL, 0, 1, '2025-12-27 20:22:27', 0);
INSERT INTO `chat_message` VALUES (104, 56, 602, NULL, '大家好！', NULL, NULL, 0, 0, '2025-12-27 20:23:08', 0);
INSERT INTO `chat_message` VALUES (105, 60, 602, 273, '老师好。', NULL, NULL, 0, 0, '2025-12-27 20:23:19', 1);
INSERT INTO `chat_message` VALUES (106, 60, 602, 273, '老师好', NULL, NULL, 1, 0, '2025-12-27 20:39:33', 0);
INSERT INTO `chat_message` VALUES (107, 60, 602, 273, '老师好', NULL, NULL, 0, 0, '2025-12-27 20:39:44', 0);

-- ----------------------------
-- Table structure for contact
-- ----------------------------
DROP TABLE IF EXISTS `contact`;
CREATE TABLE `contact`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `contact_id` bigint(20) NOT NULL COMMENT '联系人ID',
  `is_auto_added` tinyint(1) NULL DEFAULT 0 COMMENT '是否自动添加：0-否，1-是（学生自动添加的管理员）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（联系人列表排序）',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（标记消息已读时更新）',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_contact`(`user_id`, `contact_id`, `deleted`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_contact`(`contact_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '联系人表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of contact
-- ----------------------------
INSERT INTO `contact` VALUES (22, 1021, 1015, 0, '2025-12-03 19:24:18', '2025-12-03 23:54:50', 0);
INSERT INTO `contact` VALUES (23, 1015, 275, 1, '2025-12-04 01:40:12', '2025-12-04 01:40:12', 0);
INSERT INTO `contact` VALUES (24, 1021, 275, 1, '2025-12-04 01:41:39', '2025-12-04 01:41:39', 0);
INSERT INTO `contact` VALUES (27, 1011, 1015, 0, '2025-12-04 01:46:19', '2025-12-04 01:46:19', 0);
INSERT INTO `contact` VALUES (28, 443, 275, 1, '2025-12-04 22:55:41', '2025-12-04 22:55:41', 0);
INSERT INTO `contact` VALUES (29, 275, 443, 0, '2025-12-04 22:55:56', '2025-12-04 22:56:05', 0);
INSERT INTO `contact` VALUES (31, 379, 443, 0, '2025-12-06 00:59:06', '2025-12-07 13:56:22', 0);
INSERT INTO `contact` VALUES (32, 379, 275, 1, '2025-12-06 03:48:23', '2025-12-06 03:48:23', 0);
INSERT INTO `contact` VALUES (33, 576, 273, 1, '2025-12-07 18:08:10', '2025-12-07 18:08:10', 0);
INSERT INTO `contact` VALUES (34, 377, 275, 1, '2025-12-14 00:19:08', '2025-12-14 00:19:08', 0);
INSERT INTO `contact` VALUES (35, 443, 379, 0, '2025-12-14 19:46:08', '2025-12-14 19:46:08', 0);
INSERT INTO `contact` VALUES (36, 602, 273, 1, '2025-12-25 11:50:11', '2025-12-25 11:50:11', 0);
INSERT INTO `contact` VALUES (39, 273, 602, 0, '2025-12-27 20:23:19', '2025-12-27 20:40:26', 0);

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user1_id` bigint(20) NULL DEFAULT NULL COMMENT '会话发起者',
  `user2_id` bigint(20) NULL DEFAULT NULL COMMENT '会话接收者',
  `conversation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'private' COMMENT '会话类型：private-私聊，building_group-楼栋群聊，dorm_group-宿舍群聊',
  `building_id` bigint(20) NULL DEFAULT NULL COMMENT '楼栋ID（楼栋群聊时使用）',
  `dorm_group_id` bigint(20) NULL DEFAULT NULL COMMENT '宿舍队伍ID（宿舍群聊时使用）',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群聊名称',
  `last_message_id` bigint(20) NULL DEFAULT NULL COMMENT '最后一条消息ID（会话列表排序）',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后消息时间（显示最后消息）',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（学生解散宿舍组后，不直接删除聊天会话记录）',
  `mute_all` tinyint(1) NULL DEFAULT 0 COMMENT '全体禁言：0-未禁言，1-已禁言（群聊时使用）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_pair`(`user1_id`, `user2_id`, `deleted`) USING BTREE,
  INDEX `idx_user1`(`user1_id`) USING BTREE,
  INDEX `idx_user2`(`user2_id`) USING BTREE,
  INDEX `idx_last_message_time`(`last_message_time`) USING BTREE,
  INDEX `idx_conversation_type`(`conversation_type`) USING BTREE,
  INDEX `idx_building_id`(`building_id`) USING BTREE,
  INDEX `idx_dorm_group_id`(`dorm_group_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 61 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聊天会话表（私聊/宿舍群聊）' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of conversation
-- ----------------------------
INSERT INTO `conversation` VALUES (49, 443, 275, 'private', NULL, NULL, NULL, 87, '2025-12-04 22:55:56', 0, 0);
INSERT INTO `conversation` VALUES (50, NULL, NULL, 'building_group', 3, NULL, 'A群聊', 97, '2025-12-06 02:03:10', 0, 0);
INSERT INTO `conversation` VALUES (51, NULL, NULL, 'dorm_group', NULL, 1, '苏婷的团队群聊', 96, '2025-12-06 01:56:46', 0, 0);
INSERT INTO `conversation` VALUES (52, 443, 379, 'private', NULL, NULL, NULL, 101, '2025-12-14 19:46:15', 0, 0);
INSERT INTO `conversation` VALUES (53, 275, 2023002004, 'private', NULL, NULL, NULL, NULL, NULL, 0, 0);
INSERT INTO `conversation` VALUES (54, 275, 2023002068, 'private', NULL, NULL, NULL, NULL, NULL, 0, 0);
INSERT INTO `conversation` VALUES (55, NULL, NULL, 'dorm_group', NULL, 61, '宗龙的团队群聊', 100, '2025-12-14 19:06:56', 0, 0);
INSERT INTO `conversation` VALUES (56, NULL, NULL, 'building_group', 4, NULL, 'C栋群聊', 104, '2025-12-27 20:23:08', 0, 0);
INSERT INTO `conversation` VALUES (57, NULL, NULL, 'dorm_group', NULL, 65, '阳超的团队群聊', NULL, NULL, 0, 0);
INSERT INTO `conversation` VALUES (58, 273, 576, 'private', NULL, NULL, NULL, NULL, NULL, 0, 0);
INSERT INTO `conversation` VALUES (59, NULL, NULL, 'dorm_group', NULL, 75, '訾涛的团队群聊', 103, '2025-12-27 20:22:27', 0, 0);
INSERT INTO `conversation` VALUES (60, 602, 273, 'private', NULL, NULL, NULL, 107, '2025-12-27 20:39:44', 0, 0);

-- ----------------------------
-- Table structure for dorm_group
-- ----------------------------
DROP TABLE IF EXISTS `dorm_group`;
CREATE TABLE `dorm_group`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '队伍ID',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '队伍名称',
  `leader_id` bigint(20) NOT NULL COMMENT '队长ID',
  `target_room_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '房间类型',
  `max_members` int(11) NOT NULL COMMENT '最大人数',
  `current_members` int(11) NULL DEFAULT 1 COMMENT '当前人数',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '年级',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '性别',
  `selected_room_id` bigint(20) NULL DEFAULT NULL COMMENT '选择的房间ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '队伍状态：forming-组建中，full-已满员，room_selected-已选房，allocated-已分配，disbanded-已解散',
  `announcement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群公告',
  `members` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '成员列表（一次查询获得宿舍所有成员信息）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_leader`(`leader_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_room`(`selected_room_id`) USING BTREE,
  INDEX `idx_major_class`(`major`, `class_name`) USING BTREE,
  INDEX `idx_dorm_group_leader_id`(`leader_id`) USING BTREE,
  CONSTRAINT `fk_dg_leader` FOREIGN KEY (`leader_id`) REFERENCES `student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_dg_room` FOREIGN KEY (`selected_room_id`) REFERENCES `room` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 76 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选房队伍表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of dorm_group
-- ----------------------------
INSERT INTO `dorm_group` VALUES (1, '苏婷的团队', 268, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 219, 'room_selected', '明天大扫除', '[{\"className\":\"软工2302\",\"major\":\"软件工程\",\"avatar\":\"/src/assets/images/default-avatar.svg\",\"id\":268,\"studentId\":\"2023002068\",\"isLeader\":true,\"name\":\"苏婷\",\"bedNumber\":\"1\"},{\"isLeader\":false,\"name\":\"舒倩\",\"className\":\"软工2302\",\"major\":\"软件工程\",\"avatar\":\"\",\"id\":204,\"studentId\":\"2023002004\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (2, '奚芳的自动分组', 790, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 229, 'full', NULL, '[{\"studentId\":\"2024004006\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"奚芳\",\"className\":\"人工智能2401\",\"id\":790,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004008\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"彭倩\",\"className\":\"人工智能2401\",\"id\":792,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (3, '鲁萍的自动分组', 794, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 230, 'full', NULL, '[{\"studentId\":\"2024004010\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"鲁萍\",\"className\":\"人工智能2401\",\"id\":794,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004012\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"钟婷\",\"className\":\"人工智能2401\",\"id\":796,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (4, '陈二四的自动分组', 906, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 194, 'full', NULL, '[{\"studentId\":\"2025002006\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"陈二四\",\"className\":\"软工2401\",\"id\":906,\"avatar\":\"\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002006\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"伍婷\",\"className\":\"软工2401\",\"id\":598,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (5, '陈八的自动分组', 890, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 181, 'full', NULL, '[{\"studentId\":\"2025002002\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"陈八\",\"className\":\"软工2301\",\"id\":890,\"avatar\":\"\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002002\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"封婷\",\"className\":\"软工2301\",\"id\":202,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (6, '甫红的自动分组', 274, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 220, 'full', NULL, '[{\"studentId\":\"2023002074\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"甫红\",\"className\":\"软工2302\",\"id\":274,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002076\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"里洁\",\"className\":\"软工2302\",\"id\":276,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (7, '后欣的自动分组', 270, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 221, 'full', NULL, '[{\"studentId\":\"2023002070\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"后欣\",\"className\":\"软工2302\",\"id\":270,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002072\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"益静\",\"className\":\"软工2302\",\"id\":272,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (8, '汤欣的自动分组', 798, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 231, 'full', NULL, '[{\"studentId\":\"2024004014\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"汤欣\",\"className\":\"人工智能2401\",\"id\":798,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004016\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"安静\",\"className\":\"人工智能2401\",\"id\":800,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (9, '皮红的自动分组', 802, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 232, 'full', NULL, '[{\"studentId\":\"2024004018\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"皮红\",\"className\":\"人工智能2401\",\"id\":802,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004020\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"舒洁\",\"className\":\"人工智能2401\",\"id\":804,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (10, '凌艳的自动分组', 806, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 233, 'full', NULL, '[{\"studentId\":\"2024004022\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"凌艳\",\"className\":\"人工智能2401\",\"id\":806,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004024\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"柏娟\",\"className\":\"人工智能2401\",\"id\":808,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (11, '关丽的自动分组', 810, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 234, 'full', NULL, '[{\"studentId\":\"2024004026\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"关丽\",\"className\":\"人工智能2401\",\"id\":810,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004028\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"臧芳\",\"className\":\"人工智能2401\",\"id\":812,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (12, '宣倩的自动分组', 814, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 235, 'full', NULL, '[{\"studentId\":\"2024004030\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"宣倩\",\"className\":\"人工智能2401\",\"id\":814,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004032\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"赖萍\",\"className\":\"人工智能2401\",\"id\":816,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (13, '阮婷的自动分组', 818, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 236, 'full', NULL, '[{\"studentId\":\"2024004034\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"阮婷\",\"className\":\"人工智能2401\",\"id\":818,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004036\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"龚欣\",\"className\":\"人工智能2401\",\"id\":820,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (14, '植静的自动分组', 822, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 237, 'full', NULL, '[{\"studentId\":\"2024004038\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"植静\",\"className\":\"人工智能2401\",\"id\":822,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004040\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"郁红\",\"className\":\"人工智能2401\",\"id\":824,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (15, '蒙洁的自动分组', 826, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 238, 'full', NULL, '[{\"studentId\":\"2024004042\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"蒙洁\",\"className\":\"人工智能2401\",\"id\":826,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004044\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"别艳\",\"className\":\"人工智能2401\",\"id\":828,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (16, '席娟的自动分组', 830, '二人间', 2, 2, '人工智能', '人工智能2401', '2024', '女', 239, 'full', NULL, '[{\"studentId\":\"2024004046\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"席娟\",\"className\":\"人工智能2401\",\"id\":830,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024004048\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"晏丽\",\"className\":\"人工智能2401\",\"id\":832,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (17, '麴芳的自动分组', 834, '二人间', 2, 1, '人工智能', '人工智能2401', '2024', '女', 240, 'forming', NULL, '[{\"studentId\":\"2024004050\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"麴芳\",\"className\":\"人工智能2401\",\"id\":834,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (18, '苟欣的自动分组', 644, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 206, 'full', NULL, '[{\"studentId\":\"2024002052\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"苟欣\",\"className\":\"软工2402\",\"id\":644,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002054\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"麻静\",\"className\":\"软工2402\",\"id\":646,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (19, '国红的自动分组', 648, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 207, 'full', NULL, '[{\"studentId\":\"2024002056\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"国红\",\"className\":\"软工2402\",\"id\":648,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002058\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"丁洁\",\"className\":\"软工2402\",\"id\":650,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (20, '井艳的自动分组', 652, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 208, 'full', NULL, '[{\"studentId\":\"2024002060\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"井艳\",\"className\":\"软工2402\",\"id\":652,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002062\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"米娟\",\"className\":\"软工2402\",\"id\":654,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (21, '习丽的自动分组', 656, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 209, 'full', NULL, '[{\"studentId\":\"2024002064\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"习丽\",\"className\":\"软工2402\",\"id\":656,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002066\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"栾芳\",\"className\":\"软工2402\",\"id\":658,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (22, '能倩的自动分组', 660, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 210, 'full', NULL, '[{\"studentId\":\"2024002068\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"能倩\",\"className\":\"软工2402\",\"id\":660,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002070\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"冉萍\",\"className\":\"软工2402\",\"id\":662,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (23, '寇婷的自动分组', 664, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 211, 'full', NULL, '[{\"studentId\":\"2024002072\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"寇婷\",\"className\":\"软工2402\",\"id\":664,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002074\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"昌欣\",\"className\":\"软工2402\",\"id\":666,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (24, '别静的自动分组', 668, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 212, 'full', NULL, '[{\"studentId\":\"2024002076\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"别静\",\"className\":\"软工2402\",\"id\":668,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002078\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"蒯红\",\"className\":\"软工2402\",\"id\":670,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (25, '令洁的自动分组', 672, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 213, 'full', NULL, '[{\"studentId\":\"2024002080\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"令洁\",\"className\":\"软工2402\",\"id\":672,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002082\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"和艳\",\"className\":\"软工2402\",\"id\":674,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (26, '须娟的自动分组', 676, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 214, 'full', NULL, '[{\"studentId\":\"2024002084\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"须娟\",\"className\":\"软工2402\",\"id\":676,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002086\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"屈丽\",\"className\":\"软工2402\",\"id\":678,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (27, '宗芳的自动分组', 680, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 215, 'full', NULL, '[{\"studentId\":\"2024002088\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"宗芳\",\"className\":\"软工2402\",\"id\":680,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002090\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"班倩\",\"className\":\"软工2402\",\"id\":682,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (28, '应萍的自动分组', 684, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 216, 'full', NULL, '[{\"studentId\":\"2024002092\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"应萍\",\"className\":\"软工2402\",\"id\":684,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002094\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"幸婷\",\"className\":\"软工2402\",\"id\":686,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (29, '左欣的自动分组', 688, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 217, 'full', NULL, '[{\"studentId\":\"2024002096\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"左欣\",\"className\":\"软工2402\",\"id\":688,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002098\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"初静\",\"className\":\"软工2402\",\"id\":690,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (30, '钮红的自动分组', 692, '二人间', 2, 2, '软件工程', '软工2402', '2024', '女', 218, 'full', NULL, '[{\"studentId\":\"2024002100\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"钮红\",\"className\":\"软工2402\",\"id\":692,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2025002008\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"周二六\",\"className\":\"软工2402\",\"id\":908,\"avatar\":\"\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (31, '桂欣的自动分组', 600, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 195, 'full', NULL, '[{\"studentId\":\"2024002008\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"桂欣\",\"className\":\"软工2401\",\"id\":600,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002010\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"鲁静\",\"className\":\"软工2401\",\"id\":602,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (32, '阳红的自动分组', 604, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 196, 'full', NULL, '[{\"studentId\":\"2024002012\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"阳红\",\"className\":\"软工2401\",\"id\":604,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002014\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"艾洁\",\"className\":\"软工2401\",\"id\":606,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (33, '虞艳的自动分组', 608, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 197, 'full', NULL, '[{\"studentId\":\"2024002016\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"虞艳\",\"className\":\"软工2401\",\"id\":608,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002018\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"佟娟\",\"className\":\"软工2401\",\"id\":610,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (34, '容丽的自动分组', 612, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 198, 'full', NULL, '[{\"studentId\":\"2024002020\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"容丽\",\"className\":\"软工2401\",\"id\":612,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002022\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"井芳\",\"className\":\"软工2401\",\"id\":614,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (35, '项倩的自动分组', 616, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 199, 'full', NULL, '[{\"studentId\":\"2024002024\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"项倩\",\"className\":\"软工2401\",\"id\":616,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002026\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"慎萍\",\"className\":\"软工2401\",\"id\":618,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (36, '晁婷的自动分组', 620, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 200, 'full', NULL, '[{\"studentId\":\"2024002028\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"晁婷\",\"className\":\"软工2401\",\"id\":620,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002030\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"暨欣\",\"className\":\"软工2401\",\"id\":622,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (37, '松静的自动分组', 624, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 201, 'full', NULL, '[{\"studentId\":\"2024002032\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"松静\",\"className\":\"软工2401\",\"id\":624,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002034\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"印红\",\"className\":\"软工2401\",\"id\":626,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (38, '郜洁的自动分组', 628, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 202, 'full', NULL, '[{\"studentId\":\"2024002036\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"郜洁\",\"className\":\"软工2401\",\"id\":628,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002038\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"东艳\",\"className\":\"软工2401\",\"id\":630,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (39, '平娟的自动分组', 632, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 203, 'full', NULL, '[{\"studentId\":\"2024002040\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"平娟\",\"className\":\"软工2401\",\"id\":632,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002042\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"奚丽\",\"className\":\"软工2401\",\"id\":634,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (40, '商芳的自动分组', 636, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 204, 'full', NULL, '[{\"studentId\":\"2024002044\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"商芳\",\"className\":\"软工2401\",\"id\":636,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002046\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"滑倩\",\"className\":\"软工2401\",\"id\":638,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (41, '翟萍的自动分组', 640, '二人间', 2, 2, '软件工程', '软工2401', '2024', '女', 205, 'full', NULL, '[{\"studentId\":\"2024002048\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"翟萍\",\"className\":\"软工2401\",\"id\":640,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2024002050\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"强婷\",\"className\":\"软工2401\",\"id\":642,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (42, '靳红的自动分组', 252, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 222, 'full', NULL, '[{\"studentId\":\"2023002052\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"靳红\",\"className\":\"软工2302\",\"id\":252,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002054\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"蔚洁\",\"className\":\"软工2302\",\"id\":254,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (43, '闻艳的自动分组', 278, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 223, 'full', NULL, '[{\"studentId\":\"2023002078\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"闻艳\",\"className\":\"软工2302\",\"id\":278,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002080\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"连娟\",\"className\":\"软工2302\",\"id\":280,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (44, '赵丽的自动分组', 282, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 224, 'full', NULL, '[{\"studentId\":\"2023002082\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"赵丽\",\"className\":\"软工2302\",\"id\":282,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002084\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"孙芳\",\"className\":\"软工2302\",\"id\":284,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (45, '周倩的自动分组', 286, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 225, 'full', NULL, '[{\"studentId\":\"2023002086\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"周倩\",\"className\":\"软工2302\",\"id\":286,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002088\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"郑萍\",\"className\":\"软工2302\",\"id\":288,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (46, '冯婷的自动分组', 290, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 226, 'full', NULL, '[{\"studentId\":\"2023002090\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"冯婷\",\"className\":\"软工2302\",\"id\":290,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002092\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"褚欣\",\"className\":\"软工2302\",\"id\":292,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (47, '蒋静的自动分组', 294, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 227, 'full', NULL, '[{\"studentId\":\"2023002094\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"蒋静\",\"className\":\"软工2302\",\"id\":294,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002096\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"韩红\",\"className\":\"软工2302\",\"id\":296,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (48, '朱洁的自动分组', 298, '二人间', 2, 2, '软件工程', '软工2302', '2023', '女', 228, 'full', NULL, '[{\"studentId\":\"2023002098\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"朱洁\",\"className\":\"软工2302\",\"id\":298,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002100\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"尤艳\",\"className\":\"软工2302\",\"id\":300,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (49, '别静的自动分组', 206, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 182, 'full', NULL, '[{\"studentId\":\"2023002006\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"别静\",\"className\":\"软工2301\",\"id\":206,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002008\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"蒯红\",\"className\":\"软工2301\",\"id\":208,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (50, '令洁的自动分组', 210, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 183, 'full', NULL, '[{\"studentId\":\"2023002010\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"令洁\",\"className\":\"软工2301\",\"id\":210,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002012\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"和艳\",\"className\":\"软工2301\",\"id\":212,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (51, '须娟的自动分组', 214, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 184, 'full', NULL, '[{\"studentId\":\"2023002014\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"须娟\",\"className\":\"软工2301\",\"id\":214,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002016\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"屈丽\",\"className\":\"软工2301\",\"id\":216,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (52, '宗芳的自动分组', 218, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 185, 'full', NULL, '[{\"studentId\":\"2023002018\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"宗芳\",\"className\":\"软工2301\",\"id\":218,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002020\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"班倩\",\"className\":\"软工2301\",\"id\":220,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (53, '应萍的自动分组', 222, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 186, 'full', NULL, '[{\"studentId\":\"2023002022\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"应萍\",\"className\":\"软工2301\",\"id\":222,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002024\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"幸婷\",\"className\":\"软工2301\",\"id\":224,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (54, '左欣的自动分组', 226, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 187, 'full', NULL, '[{\"studentId\":\"2023002026\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"左欣\",\"className\":\"软工2301\",\"id\":226,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002028\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"初静\",\"className\":\"软工2301\",\"id\":228,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (55, '钮红的自动分组', 230, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 188, 'full', NULL, '[{\"studentId\":\"2023002030\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"钮红\",\"className\":\"软工2301\",\"id\":230,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002032\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"訾洁\",\"className\":\"软工2301\",\"id\":232,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (56, '利艳的自动分组', 234, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 189, 'full', NULL, '[{\"studentId\":\"2023002034\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"利艳\",\"className\":\"软工2301\",\"id\":234,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002036\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"哈娟\",\"className\":\"软工2301\",\"id\":236,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (57, '年丽的自动分组', 238, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 190, 'full', NULL, '[{\"studentId\":\"2023002038\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"年丽\",\"className\":\"软工2301\",\"id\":238,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002040\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"乔芳\",\"className\":\"软工2301\",\"id\":240,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (58, '皮倩的自动分组', 242, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 191, 'full', NULL, '[{\"studentId\":\"2023002042\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"皮倩\",\"className\":\"软工2301\",\"id\":242,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002044\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"卫萍\",\"className\":\"软工2301\",\"id\":244,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (59, '桑婷的自动分组', 246, '二人间', 2, 2, '软件工程', '软工2301', '2023', '女', 192, 'full', NULL, '[{\"studentId\":\"2023002046\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"桑婷\",\"className\":\"软工2301\",\"id\":246,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023002048\",\"major\":\"软件工程\",\"isLeader\":false,\"name\":\"通欣\",\"className\":\"软工2301\",\"id\":248,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (60, '干静的自动分组', 250, '二人间', 2, 1, '软件工程', '软工2301', '2023', '女', 193, 'forming', NULL, '[{\"studentId\":\"2023002050\",\"major\":\"软件工程\",\"isLeader\":true,\"name\":\"干静\",\"className\":\"软工2301\",\"id\":250,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (61, '宗龙的团队', 401, '四人间', 4, 4, '人工智能', '人工智能2301', '2023', '男', 293, 'room_selected', NULL, '[{\"avatar\":\"/src/assets/images/default-avatar.svg\",\"id\":401,\"studentId\":\"2023004001\",\"isLeader\":true,\"name\":\"宗龙\",\"className\":\"人工智能2301\",\"major\":\"人工智能\",\"bedNumber\":\"1\"},{\"id\":405,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"major\":\"人工智能\",\"className\":\"人工智能2301\",\"name\":\"慕涛\",\"isLeader\":false,\"studentId\":\"2023004005\",\"bedNumber\":\"1\"},{\"id\":407,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"major\":\"人工智能\",\"className\":\"人工智能2301\",\"name\":\"习杰\",\"isLeader\":false,\"studentId\":\"2023004007\",\"bedNumber\":\"1\"},{\"id\":409,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"major\":\"人工智能\",\"className\":\"人工智能2301\",\"name\":\"米峰\",\"isLeader\":false,\"studentId\":\"2023004009\",\"bedNumber\":\"2\"}]');
INSERT INTO `dorm_group` VALUES (66, '宁红的团队', 846, '四人间', 4, 1, '人工智能', '人工智能2402', '2024', '女', NULL, 'forming', NULL, '[{\"name\":\"宁红\",\"isLeader\":true,\"studentId\":\"2024004062\",\"id\":846,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"major\":\"人工智能\",\"className\":\"人工智能2402\"}]');
INSERT INTO `dorm_group` VALUES (73, '阳超的自动分组', 411, '四人间', 4, 4, '人工智能', '人工智能2301', '2023', '男', 294, 'full', NULL, '[{\"studentId\":\"2023004011\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"阳超\",\"className\":\"人工智能2301\",\"id\":411,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023004013\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"封龙\",\"className\":\"人工智能2301\",\"id\":413,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"},{\"studentId\":\"2023004015\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"舒斌\",\"className\":\"人工智能2301\",\"id\":415,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"3\"},{\"studentId\":\"2023004017\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"皮军\",\"className\":\"人工智能2301\",\"id\":417,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"4\"}]');
INSERT INTO `dorm_group` VALUES (74, '卫磊的自动分组', 419, '四人间', 4, 4, '人工智能', '人工智能2301', '2023', '男', 295, 'full', NULL, '[{\"studentId\":\"2023004019\",\"major\":\"人工智能\",\"isLeader\":true,\"name\":\"卫磊\",\"className\":\"人工智能2301\",\"id\":419,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"1\"},{\"studentId\":\"2023004021\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"滕伟\",\"className\":\"人工智能2301\",\"id\":421,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"2\"},{\"studentId\":\"2023004023\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"单鹏\",\"className\":\"人工智能2301\",\"id\":423,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"3\"},{\"studentId\":\"2023004025\",\"major\":\"人工智能\",\"isLeader\":false,\"name\":\"巩勇\",\"className\":\"人工智能2301\",\"id\":425,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"bedNumber\":\"4\"}]');
INSERT INTO `dorm_group` VALUES (75, '訾涛的团队', 427, '四人间', 4, 1, '人工智能', '人工智能2301', '2023', '男', NULL, 'forming', '星期六大扫除', '[{\"id\":427,\"avatar\":\"/src/assets/images/default-avatar.svg\",\"major\":\"人工智能\",\"className\":\"人工智能2301\",\"name\":\"訾涛\",\"isLeader\":true,\"studentId\":\"2023004027\"}]');

-- ----------------------------
-- Table structure for facility_template
-- ----------------------------
DROP TABLE IF EXISTS `facility_template`;
CREATE TABLE `facility_template`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '设施ID',
  `facility_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设施名称',
  `default_quantity` int(11) NULL DEFAULT 1 COMMENT '默认数量',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '个' COMMENT '单位',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序号',
  `default_room_types` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认房型列表（二人间,四人间,六人间,八人间）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_facility_name`(`facility_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '超级管理员端/配置管理页面的设施管理表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of facility_template
-- ----------------------------
INSERT INTO `facility_template` VALUES (1, '空调', 1, '个', 1, '二人间,四人间,六人间,八人间');
INSERT INTO `facility_template` VALUES (2, '洗衣机', 1, '个', 2, '八人间,六人间');
INSERT INTO `facility_template` VALUES (3, '饮水机', 1, '个', 3, '八人间');
INSERT INTO `facility_template` VALUES (4, '风扇', 1, '个', 4, '八人间');
INSERT INTO `facility_template` VALUES (5, '智能门锁', 1, '个', 5, NULL);
INSERT INTO `facility_template` VALUES (6, '阳台', 1, '个', 2, '二人间');
INSERT INTO `facility_template` VALUES (7, '独立卫浴', 1, '间', 3, '二人间,四人间');
INSERT INTO `facility_template` VALUES (8, 'WiFi', 1, '个', 4, '四人间');
INSERT INTO `facility_template` VALUES (10, '热水器', 1, '台', 6, NULL);
INSERT INTO `facility_template` VALUES (11, '书桌', 1, '张', 7, '二人间,八人间');
INSERT INTO `facility_template` VALUES (12, '椅子', 1, '把', 8, '二人间,八人间');
INSERT INTO `facility_template` VALUES (13, '衣柜', 1, '个', 9, '二人间,四人间,八人间');

-- ----------------------------
-- Table structure for group_chat_member
-- ----------------------------
DROP TABLE IF EXISTS `group_chat_member`;
CREATE TABLE `group_chat_member`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint(20) NOT NULL COMMENT '会话ID（群聊）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'member' COMMENT '角色：member-普通成员，admin-管理员',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '退群 false，进群true',
  `is_muted` tinyint(1) NULL DEFAULT 0 COMMENT '是否被禁言：0-未禁言，1-已禁言',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_conversation_user`(`conversation_id`, `user_id`, `is_active`) USING BTREE,
  INDEX `idx_conversation`(`conversation_id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '记录用户在群聊（楼栋群/宿舍组群）中的成员身份、角色和状态。明确谁在哪个群里' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of group_chat_member
-- ----------------------------
INSERT INTO `group_chat_member` VALUES (1, 4, 275, 'admin', 1, 0);
INSERT INTO `group_chat_member` VALUES (2, 4, 1015, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (3, 4, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (4, 4, 1042, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (5, 33, 271, 'admin', 1, 0);
INSERT INTO `group_chat_member` VALUES (6, 34, 1015, 'member', 0, 0);
INSERT INTO `group_chat_member` VALUES (7, 34, 1021, 'member', 0, 0);
INSERT INTO `group_chat_member` VALUES (8, 35, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (9, 35, 1015, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (10, 42, 275, 'admin', 1, 0);
INSERT INTO `group_chat_member` VALUES (11, 42, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (12, 42, 1015, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (13, 44, 1015, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (14, 44, 1021, 'member', 0, 0);
INSERT INTO `group_chat_member` VALUES (15, 47, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (16, 47, 1015, 'member', 0, 0);
INSERT INTO `group_chat_member` VALUES (17, 48, 1015, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (18, 48, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (19, 50, 275, 'admin', 1, 0);
INSERT INTO `group_chat_member` VALUES (20, 50, 443, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (21, 51, 443, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (22, 51, 379, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (23, 50, 379, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (24, 55, 576, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (25, 55, 580, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (26, 55, 582, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (27, 55, 584, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (28, 56, 576, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (29, 57, 586, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (30, 57, 592, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (31, 56, 273, 'admin', 1, 0);
INSERT INTO `group_chat_member` VALUES (32, 50, 377, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (33, 56, 580, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (34, 56, 1021, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (35, 56, 602, 'member', 1, 0);
INSERT INTO `group_chat_member` VALUES (36, 59, 602, 'member', 1, 0);

-- ----------------------------
-- Table structure for material_template
-- ----------------------------
DROP TABLE IF EXISTS `material_template`;
CREATE TABLE `material_template`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `building_id` bigint(20) NULL DEFAULT NULL COMMENT '楼栋ID',
  `template_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `building_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '楼栋类型',
  `room_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '房间类型',
  `materials` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '物资列表',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_building_room_type`(`building_id`, `room_type`) USING BTREE,
  INDEX `idx_building_type`(`building_type`) USING BTREE,
  INDEX `idx_room_type`(`room_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '超级管理员端/配置管理的物资模板表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of material_template
-- ----------------------------
INSERT INTO `material_template` VALUES (1, 3, 'A栋-二人间', '女', '二人间', '[{\"id\":\"固定设施_空调\",\"name\":\"空调\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_阳台\",\"name\":\"阳台\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_独立卫浴\",\"name\":\"独立卫浴\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_风扇\",\"name\":\"风扇\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_智能门锁\",\"name\":\"智能门锁\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_衣柜\",\"name\":\"衣柜\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"非固定设施_扫把\",\"name\":\"扫把\",\"category\":\"非固定设施\",\"quantity\":2,\"unit\":\"个\"}]');
INSERT INTO `material_template` VALUES (2, 5, 'B栋-四人间', '女', '四人间', '[{\"id\":\"固定设施_空调\",\"name\":\"空调\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_洗衣机\",\"name\":\"洗衣机\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_饮水机\",\"name\":\"饮水机\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_阳台\",\"name\":\"阳台\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"}]');
INSERT INTO `material_template` VALUES (3, 4, 'C栋-四人间', '男', '四人间', '[{\"id\":\"固定设施_空调\",\"name\":\"空调\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_洗衣机\",\"name\":\"洗衣机\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_饮水机\",\"name\":\"饮水机\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_阳台\",\"name\":\"阳台\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"}]');
INSERT INTO `material_template` VALUES (4, 3, 'A-四人间', '女', '四人间', '[{\"id\":\"固定设施_空调\",\"name\":\"空调\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_独立卫浴\",\"name\":\"独立卫浴\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"间\"},{\"id\":\"固定设施_WiFi\",\"name\":\"WiFi\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"},{\"id\":\"固定设施_衣柜\",\"name\":\"衣柜\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"}]');
INSERT INTO `material_template` VALUES (5, 4, 'C栋-六人间', '男', '六人间', '[{\"id\":\"固定设施_空调\",\"name\":\"空调\",\"category\":\"固定设施\",\"quantity\":1,\"unit\":\"个\"}]');

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '接收用户ID，NULL表示广播通知',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知内容',
  `type` enum('system','application','allocation','announcement','chat','visitor') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知类型：system-系统通知，application-申请通知，allocation-分配通知，announcement-公告通知，chat-聊天通知，visitor-访客通知',
  `related_id` bigint(20) NULL DEFAULT NULL COMMENT '关联ID（如申请ID、公告ID）',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id`, `is_read`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1186 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1179, 275, '新的访客预约申请', '访客刘涛预约访问学生封婷，请及时审批', 'visitor', 3, 1, '2025-12-12 14:31:09');
INSERT INTO `notification` VALUES (1180, 275, '新的调宿申请待审核', '学生苏婷提交了调宿申请，请及时处理', 'application', 19, 1, '2025-12-15 00:32:12');
INSERT INTO `notification` VALUES (1181, 275, '新的退宿申请待审核', '学生苏婷提交了退宿申请，请及时处理', 'application', 20, 1, '2025-12-15 00:32:37');
INSERT INTO `notification` VALUES (1182, 275, '新的访客预约申请', '访客五涛预约访问学生封婷，请及时审批', 'visitor', 4, 1, '2025-12-15 01:10:23');
INSERT INTO `notification` VALUES (1183, 273, '新的调宿申请待审核', '学生訾涛提交了调宿申请，请及时处理', 'application', 21, 0, '2025-12-25 10:18:51');
INSERT INTO `notification` VALUES (1184, 275, '新的退宿申请待审核', '学生蒯红提交了退宿申请，请及时处理', 'application', 22, 0, '2025-12-28 00:57:29');
INSERT INTO `notification` VALUES (1185, 275, '新的调宿申请待审核', '学生封婷提交了调宿申请，请及时处理', 'application', 23, 0, '2025-12-28 12:15:25');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint(20) NOT NULL COMMENT '操作用户ID',
  `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作用户名',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作内容',
  `operation_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作模块',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_operation_type`(`operation_type`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 292 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (208, 1, 'superadmin', '创建用品模板: A - 二人间', '用品模板配置', 1, NULL, '2025-12-04 20:27:26');
INSERT INTO `operation_log` VALUES (209, 1, 'superadmin', '创建用品模板: A - 四人间', '用品模板配置', 1, NULL, '2025-12-04 20:27:30');
INSERT INTO `operation_log` VALUES (210, 1, 'superadmin', '创建用品模板: A - 六人间', '用品模板配置', 1, NULL, '2025-12-04 20:27:32');
INSERT INTO `operation_log` VALUES (211, 1, 'superadmin', '创建用品模板: B栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-04 20:27:39');
INSERT INTO `operation_log` VALUES (212, 1, 'superadmin', '创建用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-04 20:27:43');
INSERT INTO `operation_log` VALUES (213, 1, 'superadmin', '保存房型设施配置: 双人间', '房间配置', 1, NULL, '2025-12-04 21:17:04');
INSERT INTO `operation_log` VALUES (214, 1, 'superadmin', '保存房型设施配置: 四人间', '房间配置', 1, NULL, '2025-12-04 21:17:09');
INSERT INTO `operation_log` VALUES (215, 1, 'superadmin', '保存房型设施配置: 六人间', '房间配置', 1, NULL, '2025-12-04 21:17:10');
INSERT INTO `operation_log` VALUES (216, 1, 'superadmin', '保存房型设施配置: 八人间', '房间配置', 1, NULL, '2025-12-04 21:17:12');
INSERT INTO `operation_log` VALUES (217, 1, 'superadmin', '创建用品模板: A - 双人间', '用品模板配置', 1, NULL, '2025-12-04 21:24:18');
INSERT INTO `operation_log` VALUES (218, 1, 'superadmin', '创建用品模板: B栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-04 21:24:23');
INSERT INTO `operation_log` VALUES (219, 1, 'superadmin', '创建用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-04 21:24:27');
INSERT INTO `operation_log` VALUES (220, 1, 'superadmin', '更新用品模板: A - 双人间', '用品模板配置', 1, NULL, '2025-12-04 21:32:14');
INSERT INTO `operation_log` VALUES (221, 1, 'superadmin', '批量添加宿舍：A 60个', '楼栋管理', 1, NULL, '2025-12-04 22:03:29');
INSERT INTO `operation_log` VALUES (222, 1, 'superadmin', '清空楼栋专业分配：A', '楼栋管理', 1, NULL, '2025-12-04 23:57:13');
INSERT INTO `operation_log` VALUES (223, 1, 'superadmin', '专业分配：为楼栋 A 保存 2 个专业的方案（占用60间）', '楼栋管理', 1, NULL, '2025-12-04 23:57:23');
INSERT INTO `operation_log` VALUES (224, 1, 'superadmin', '清空楼栋专业分配：A', '楼栋管理', 1, NULL, '2025-12-05 00:05:19');
INSERT INTO `operation_log` VALUES (225, 1, 'superadmin', '专业分配：为楼栋 A 保存 2 个专业的方案（占用60间）', '楼栋管理', 1, NULL, '2025-12-05 00:15:10');
INSERT INTO `operation_log` VALUES (226, 1, 'superadmin', '批量删除学生信息 8 条', '学生信息管理', 1, NULL, '2025-12-05 15:15:36');
INSERT INTO `operation_log` VALUES (227, 1, 'superadmin', '批量删除学生信息 8 条', '学生信息管理', 1, NULL, '2025-12-05 15:16:11');
INSERT INTO `operation_log` VALUES (228, 1, 'superadmin', '编辑管理员信息：管理员', '管理员管理', 1, NULL, '2025-12-06 01:50:48');
INSERT INTO `operation_log` VALUES (229, 1, 'superadmin', '编辑管理员信息：管理员', '管理员管理', 1, NULL, '2025-12-06 01:50:58');
INSERT INTO `operation_log` VALUES (230, 1, 'superadmin', '编辑管理员信息：管理员', '管理员管理', 1, NULL, '2025-12-06 01:51:06');
INSERT INTO `operation_log` VALUES (231, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 02:39:33');
INSERT INTO `operation_log` VALUES (232, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 02:58:43');
INSERT INTO `operation_log` VALUES (233, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 02:59:41');
INSERT INTO `operation_log` VALUES (234, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 03:18:19');
INSERT INTO `operation_log` VALUES (235, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 03:18:43');
INSERT INTO `operation_log` VALUES (236, 1, 'superadmin', '编辑管理员信息：李管理员', '管理员管理', 1, NULL, '2025-12-06 03:19:02');
INSERT INTO `operation_log` VALUES (237, 1, 'superadmin', '标记通知为已读：新访客预约', '通知管理', 1, NULL, '2025-12-07 11:29:37');
INSERT INTO `operation_log` VALUES (238, 1, 'superadmin', '标记通知为已读：新访客预约', '通知管理', 1, NULL, '2025-12-07 11:29:38');
INSERT INTO `operation_log` VALUES (239, 1, 'superadmin', '发布通知：21', '通知管理', 1, NULL, '2025-12-07 11:53:29');
INSERT INTO `operation_log` VALUES (240, 1, 'superadmin', '发布通知：12', '通知管理', 1, NULL, '2025-12-07 12:07:21');
INSERT INTO `operation_log` VALUES (241, 1, 'superadmin', '删除通知：12', '通知管理', 1, NULL, '2025-12-07 12:08:21');
INSERT INTO `operation_log` VALUES (242, 1, 'superadmin', '发布通知：111', '通知管理', 1, NULL, '2025-12-07 12:08:49');
INSERT INTO `operation_log` VALUES (243, 1, 'superadmin', '发布通知：12', '通知管理', 1, NULL, '2025-12-07 12:13:35');
INSERT INTO `operation_log` VALUES (244, 1, 'superadmin', '全部标记通知为已读', '通知管理', 1, NULL, '2025-12-07 12:18:00');
INSERT INTO `operation_log` VALUES (245, 1, 'superadmin', '发布通知：123', '通知管理', 1, NULL, '2025-12-07 12:18:25');
INSERT INTO `operation_log` VALUES (246, 1, 'superadmin', '全部标记通知为已读', '通知管理', 1, NULL, '2025-12-07 12:33:15');
INSERT INTO `operation_log` VALUES (247, 1, 'superadmin', '删除通知：12', '通知管理', 1, NULL, '2025-12-07 13:03:24');
INSERT INTO `operation_log` VALUES (248, 1, 'superadmin', '删除通知：12', '通知管理', 1, NULL, '2025-12-07 13:03:27');
INSERT INTO `operation_log` VALUES (249, 1, 'superadmin', '删除通知：12', '通知管理', 1, NULL, '2025-12-07 13:03:30');
INSERT INTO `operation_log` VALUES (250, 1, 'superadmin', '删除通知：12', '通知管理', 1, NULL, '2025-12-07 13:03:32');
INSERT INTO `operation_log` VALUES (251, 1, 'superadmin', '保存基本信息', '个人设置', 1, NULL, '2025-12-07 13:20:10');
INSERT INTO `operation_log` VALUES (252, 1, 'superadmin', '保存基本信息', '个人设置', 1, NULL, '2025-12-07 13:21:26');
INSERT INTO `operation_log` VALUES (253, 1, 'superadmin', '修改密码', '个人设置', 1, NULL, '2025-12-07 13:21:44');
INSERT INTO `operation_log` VALUES (254, 1, 'superadmin', '退出登录', '系统', 1, NULL, '2025-12-07 13:24:46');
INSERT INTO `operation_log` VALUES (255, 1, 'superadmin', '批量添加宿舍：C栋 60个', '楼栋管理', 1, NULL, '2025-12-07 15:23:48');
INSERT INTO `operation_log` VALUES (256, 1, 'superadmin', '专业分配：为楼栋 C栋 保存 3 个专业的方案（占用59间）', '楼栋管理', 1, NULL, '2025-12-07 15:24:20');
INSERT INTO `operation_log` VALUES (257, 1, 'superadmin', '编辑楼栋：C栋', '楼栋管理', 1, NULL, '2025-12-07 15:25:25');
INSERT INTO `operation_log` VALUES (258, 1, 'superadmin', '批量添加宿舍：B栋 60个', '楼栋管理', 1, NULL, '2025-12-07 18:09:34');
INSERT INTO `operation_log` VALUES (259, 1, 'superadmin', '更新用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-07 19:09:44');
INSERT INTO `operation_log` VALUES (260, 1, 'superadmin', '保存房型默认模板: 四人间', '设施管理', 1, NULL, '2025-12-07 23:02:29');
INSERT INTO `operation_log` VALUES (261, 1, 'superadmin', '创建用品模板: A - 四人间', '用品模板配置', 1, NULL, '2025-12-07 23:02:47');
INSERT INTO `operation_log` VALUES (262, 1, 'superadmin', '保存房型默认模板: 四人间', '设施管理', 1, NULL, '2025-12-07 23:03:06');
INSERT INTO `operation_log` VALUES (263, 1, 'superadmin', '更新用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-07 23:03:22');
INSERT INTO `operation_log` VALUES (264, 1, 'superadmin', '保存房型默认模板: 六人间', '设施管理', 1, NULL, '2025-12-07 23:03:43');
INSERT INTO `operation_log` VALUES (265, 1, 'superadmin', '创建用品模板: C栋 - 六人间', '用品模板配置', 1, NULL, '2025-12-07 23:03:58');
INSERT INTO `operation_log` VALUES (266, 1, 'superadmin', '删除通知：123', '通知管理', 1, NULL, '2025-12-09 18:15:46');
INSERT INTO `operation_log` VALUES (267, 1, 'superadmin', '禁用管理员：刘', '管理员管理', 1, NULL, '2025-12-13 23:46:09');
INSERT INTO `operation_log` VALUES (268, 1, 'superadmin', '启用管理员：刘', '管理员管理', 1, NULL, '2025-12-13 23:46:11');
INSERT INTO `operation_log` VALUES (269, 1, 'superadmin', '修改密码', '个人设置', 1, NULL, '2025-12-14 14:48:44');
INSERT INTO `operation_log` VALUES (270, 1, 'superadmin', '保存基本信息', '个人设置', 1, NULL, '2025-12-14 14:48:54');
INSERT INTO `operation_log` VALUES (271, 1, 'superadmin', '删除设施: 暖气', '房间配置', 1, NULL, '2025-12-14 14:49:57');
INSERT INTO `operation_log` VALUES (272, 1, 'superadmin', '禁用管理员：刘', '管理员管理', 1, NULL, '2025-12-17 14:42:53');
INSERT INTO `operation_log` VALUES (273, 1, 'superadmin', '启用管理员：刘', '管理员管理', 1, NULL, '2025-12-17 14:42:55');
INSERT INTO `operation_log` VALUES (274, 1, 'superadmin', '添加管理员：', '管理员管理', 1, NULL, '2025-12-17 14:43:54');
INSERT INTO `operation_log` VALUES (275, 1, 'superadmin', '编辑楼栋：A栋', '楼栋管理', 1, NULL, '2025-12-17 14:45:48');
INSERT INTO `operation_log` VALUES (276, 1, 'superadmin', '添加楼栋：D栋', '楼栋管理', 1, NULL, '2025-12-17 14:46:02');
INSERT INTO `operation_log` VALUES (277, 1, 'superadmin', '已将 D栋 分配给 李管理员', '楼栋管理', 1, NULL, '2025-12-17 14:46:21');
INSERT INTO `operation_log` VALUES (278, 1, 'superadmin', '已将 D栋 分配给 韦', '楼栋管理', 1, NULL, '2025-12-17 14:46:51');
INSERT INTO `operation_log` VALUES (279, 1, 'superadmin', '添加宿舍：D栋 101', '楼栋管理', 1, NULL, '2025-12-17 14:47:21');
INSERT INTO `operation_log` VALUES (280, 1, 'superadmin', '删除宿舍：101', '楼栋管理', 1, NULL, '2025-12-17 14:47:32');
INSERT INTO `operation_log` VALUES (281, 1, 'superadmin', '更新用品模板: A栋 - 二人间', '用品模板配置', 1, NULL, '2025-12-17 14:52:45');
INSERT INTO `operation_log` VALUES (282, 1, 'superadmin', '保存房型默认模板: 六人间', '设施管理', 1, NULL, '2025-12-17 14:52:56');
INSERT INTO `operation_log` VALUES (283, 1, 'superadmin', '编辑管理员信息：韦', '管理员管理', 1, NULL, '2025-12-17 15:34:59');
INSERT INTO `operation_log` VALUES (284, 1, 'superadmin', '编辑管理员信息：韦管理员', '管理员管理', 1, NULL, '2025-12-27 20:02:28');
INSERT INTO `operation_log` VALUES (285, 1, 'superadmin', '编辑管理员信息：刘管理员', '管理员管理', 1, NULL, '2025-12-27 20:02:32');
INSERT INTO `operation_log` VALUES (286, 1, 'superadmin', '编辑管理员信息：王管理员', '管理员管理', 1, NULL, '2025-12-27 20:02:35');
INSERT INTO `operation_log` VALUES (287, 1, 'superadmin', '编辑管理员信息：周管理员', '管理员管理', 1, NULL, '2025-12-27 20:02:37');
INSERT INTO `operation_log` VALUES (288, 1, 'superadmin', '更新用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-27 20:13:02');
INSERT INTO `operation_log` VALUES (289, 1, 'superadmin', '更新用品模板: C栋 - 四人间', '用品模板配置', 1, NULL, '2025-12-27 20:13:25');
INSERT INTO `operation_log` VALUES (290, 1, 'superadmin', '添加管理员：', '管理员管理', 1, NULL, '2025-12-28 01:04:54');
INSERT INTO `operation_log` VALUES (291, 1, 'superadmin', '删除管理员：马管理员', '管理员管理', 1, NULL, '2025-12-28 01:04:59');

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `building_id` bigint(20) NOT NULL COMMENT '所属宿舍楼ID',
  `room_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '房间号',
  `floor` int(11) NOT NULL COMMENT '楼层',
  `room_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '房间类型',
  `bed_count` int(11) NOT NULL COMMENT '床位数',
  `available_beds` int(11) NOT NULL COMMENT '可用床位数',
  `major_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '归属专业',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '归属班级',
  `class_segment` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '班级分片标识，匹配分配的学生分片',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_building_room`(`building_id`, `room_number`) USING BTREE,
  INDEX `idx_building`(`building_id`) USING BTREE,
  INDEX `idx_room_type`(`room_type`) USING BTREE,
  INDEX `idx_room_major_class`(`major_name`, `class_name`) USING BTREE,
  INDEX `idx_room_major_class_segment`(`major_name`, `class_name`, `class_segment`) USING BTREE,
  CONSTRAINT `fk_room_building` FOREIGN KEY (`building_id`) REFERENCES `building` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 361 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '房间表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of room
-- ----------------------------
INSERT INTO `room` VALUES (181, 3, '101', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (182, 3, '102', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (183, 3, '103', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (184, 3, '104', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (185, 3, '105', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (186, 3, '106', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (187, 3, '107', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (188, 3, '108', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (189, 3, '109', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (190, 3, '110', 1, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (191, 3, '201', 2, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (192, 3, '202', 2, '二人间', 2, 0, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (193, 3, '203', 2, '二人间', 2, 1, '软件工程', '软工2301', NULL);
INSERT INTO `room` VALUES (194, 3, '204', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (195, 3, '205', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (196, 3, '206', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (197, 3, '207', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (198, 3, '208', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (199, 3, '209', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (200, 3, '210', 2, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (201, 3, '301', 3, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (202, 3, '302', 3, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (203, 3, '303', 3, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (204, 3, '304', 3, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (205, 3, '305', 3, '二人间', 2, 0, '软件工程', '软工2401', NULL);
INSERT INTO `room` VALUES (206, 3, '306', 3, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (207, 3, '307', 3, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (208, 3, '308', 3, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (209, 3, '309', 3, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (210, 3, '310', 3, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (211, 3, '401', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (212, 3, '402', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (213, 3, '403', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (214, 3, '404', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (215, 3, '405', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (216, 3, '406', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (217, 3, '407', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (218, 3, '408', 4, '二人间', 2, 0, '软件工程', '软工2402', NULL);
INSERT INTO `room` VALUES (219, 3, '409', 4, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (220, 3, '410', 4, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (221, 3, '501', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (222, 3, '502', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (223, 3, '503', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (224, 3, '504', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (225, 3, '505', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (226, 3, '506', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (227, 3, '507', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (228, 3, '508', 5, '二人间', 2, 0, '软件工程', '软工2302', NULL);
INSERT INTO `room` VALUES (229, 3, '509', 5, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (230, 3, '510', 5, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (231, 3, '601', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (232, 3, '602', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (233, 3, '603', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (234, 3, '604', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (235, 3, '605', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (236, 3, '606', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (237, 3, '607', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (238, 3, '608', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (239, 3, '609', 6, '二人间', 2, 0, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (240, 3, '610', 6, '二人间', 2, 1, '人工智能', '人工智能2401', NULL);
INSERT INTO `room` VALUES (241, 4, '101', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (242, 4, '102', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (243, 4, '103', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (244, 4, '104', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (245, 4, '105', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (246, 4, '106', 1, '四人间', 4, 4, '计算机科学与技术', '计科2401', NULL);
INSERT INTO `room` VALUES (247, 4, '107', 1, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (248, 4, '108', 1, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (249, 4, '109', 1, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (250, 4, '110', 1, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (251, 4, '201', 2, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (252, 4, '202', 2, '四人间', 4, 4, '计算机科学与技术', '计科2301', NULL);
INSERT INTO `room` VALUES (253, 4, '203', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (254, 4, '204', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (255, 4, '205', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (256, 4, '206', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (257, 4, '207', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (258, 4, '208', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (259, 4, '209', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2402', NULL);
INSERT INTO `room` VALUES (260, 4, '210', 2, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (261, 4, '301', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (262, 4, '302', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (263, 4, '303', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (264, 4, '304', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (265, 4, '305', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2401', NULL);
INSERT INTO `room` VALUES (266, 4, '306', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (267, 4, '307', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (268, 4, '308', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (269, 4, '309', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (270, 4, '310', 3, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (271, 4, '401', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (272, 4, '402', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2302', NULL);
INSERT INTO `room` VALUES (273, 4, '403', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (274, 4, '404', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (275, 4, '405', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (276, 4, '406', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (277, 4, '407', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (278, 4, '408', 4, '四人间', 4, 4, '数据科学与大数据技术', '数据2301', NULL);
INSERT INTO `room` VALUES (279, 4, '409', 4, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (280, 4, '410', 4, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (281, 4, '501', 5, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (282, 4, '502', 5, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (283, 4, '503', 5, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (284, 4, '504', 5, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (285, 4, '505', 5, '四人间', 4, 4, '人工智能', '人工智能2402', NULL);
INSERT INTO `room` VALUES (286, 4, '506', 5, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (287, 4, '507', 5, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (288, 4, '508', 5, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (289, 4, '509', 5, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (290, 4, '510', 5, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (291, 4, '601', 6, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (292, 4, '602', 6, '四人间', 4, 4, '人工智能', '人工智能2302', NULL);
INSERT INTO `room` VALUES (293, 4, '603', 6, '四人间', 4, 0, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (294, 4, '604', 6, '四人间', 4, 0, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (295, 4, '605', 6, '四人间', 4, 0, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (296, 4, '606', 6, '四人间', 4, 4, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (297, 4, '607', 6, '四人间', 4, 4, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (298, 4, '608', 6, '四人间', 4, 4, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (299, 4, '609', 6, '四人间', 4, 4, '人工智能', '人工智能2301', NULL);
INSERT INTO `room` VALUES (300, 4, '610', 6, '四人间', 4, 4, NULL, NULL, NULL);
INSERT INTO `room` VALUES (301, 5, '101', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (302, 5, '102', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (303, 5, '103', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (304, 5, '104', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (305, 5, '105', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (306, 5, '106', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (307, 5, '107', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (308, 5, '108', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (309, 5, '109', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (310, 5, '110', 1, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (311, 5, '201', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (312, 5, '202', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (313, 5, '203', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (314, 5, '204', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (315, 5, '205', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (316, 5, '206', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (317, 5, '207', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (318, 5, '208', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (319, 5, '209', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (320, 5, '210', 2, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (321, 5, '301', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (322, 5, '302', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (323, 5, '303', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (324, 5, '304', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (325, 5, '305', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (326, 5, '306', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (327, 5, '307', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (328, 5, '308', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (329, 5, '309', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (330, 5, '310', 3, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (331, 5, '401', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (332, 5, '402', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (333, 5, '403', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (334, 5, '404', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (335, 5, '405', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (336, 5, '406', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (337, 5, '407', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (338, 5, '408', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (339, 5, '409', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (340, 5, '410', 4, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (341, 5, '501', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (342, 5, '502', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (343, 5, '503', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (344, 5, '504', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (345, 5, '505', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (346, 5, '506', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (347, 5, '507', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (348, 5, '508', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (349, 5, '509', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (350, 5, '510', 5, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (351, 5, '601', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (352, 5, '602', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (353, 5, '603', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (354, 5, '604', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (355, 5, '605', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (356, 5, '606', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (357, 5, '607', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (358, 5, '608', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (359, 5, '609', 6, '二人间', 2, 2, NULL, NULL, NULL);
INSERT INTO `room` VALUES (360, 5, '610', 6, '二人间', 2, 2, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for roommate_invite
-- ----------------------------
DROP TABLE IF EXISTS `roommate_invite`;
CREATE TABLE `roommate_invite`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `sender_id` bigint(20) NULL DEFAULT NULL COMMENT '发送邀请学生ID',
  `sender_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发送邀请的名字',
  `sender_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '发送邀请学生的学号',
  `receiver_id` bigint(20) NULL DEFAULT NULL COMMENT '受邀请学生ID',
  `receiver_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '受邀请学生姓名',
  `receiver_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '受邀请学生学号',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理，ACCEPTED-已接受，REJECTED-已拒绝',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sender`(`sender_id`) USING BTREE,
  INDEX `idx_receiver`(`receiver_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '室友邀请表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of roommate_invite
-- ----------------------------
INSERT INTO `roommate_invite` VALUES (1, 268, '苏婷', '2023002068', 204, '舒倩', '2023002004', 'ACCEPTED', '2025-12-05 21:16:45');
INSERT INTO `roommate_invite` VALUES (2, 401, '宗龙', '2023004001', 405, '慕涛', '2023004005', 'ACCEPTED', '2025-12-07 16:26:18');
INSERT INTO `roommate_invite` VALUES (3, 401, '宗龙', '2023004001', 407, '习杰', '2023004007', 'ACCEPTED', '2025-12-07 16:26:23');
INSERT INTO `roommate_invite` VALUES (4, 401, '宗龙', '2023004001', 409, '米峰', '2023004009', 'ACCEPTED', '2025-12-07 16:26:24');

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '关联用户ID（学生注册后关联）',
  `student_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '专业',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '年级',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级',
  `class_segment` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '班级分片标识：A/B/C等，对应building_major_assignment的分片',
  `dorm_group_id` bigint(20) NULL DEFAULT NULL COMMENT '宿舍组ID',
  `room_id` bigint(20) NULL DEFAULT NULL COMMENT '房间ID',
  `room_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '房间号',
  `invite_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邀请码',
  `invite_code_status` enum('unused','used','expired') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邀请码状态',
  `invite_code_expiry` datetime NULL DEFAULT NULL COMMENT '邀请码有效期',
  `is_registered` tinyint(1) NULL DEFAULT 0 COMMENT '是否已注册：0-未注册（仅导入），1-已注册（已激活账号）',
  `registration_date` datetime NULL DEFAULT NULL COMMENT '注册时间（学生使用邀请码激活账号的时间）',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '个人简介',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个人标签',
  `bed_time` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '23:00' COMMENT '就寝时间（格式：HH:mm）',
  `wake_time` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '07:00' COMMENT '起床时间（格式：HH:mm）',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_id`(`student_id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id`) USING BTREE,
  INDEX `idx_grade_major_class`(`grade`, `major`, `class_name`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE,
  INDEX `idx_student_user_id`(`user_id`) USING BTREE,
  INDEX `idx_major_class_segment`(`major`, `class_name`, `class_segment`) USING BTREE,
  INDEX `idx_dorm_group_id`(`dorm_group_id`) USING BTREE,
  INDEX `idx_room_id`(`room_id`) USING BTREE,
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 916 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生信息表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (101, 276, '2023001001', '文伟', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV0001019d3cd4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (102, 277, '2023001002', '毛芳', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV000102cf87ef', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (103, 278, '2023001003', '甘鹏', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00010321beed', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (104, 279, '2023001004', '牛敏', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00010421669d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (105, 280, '2023001005', '冀峰', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV0001055ef853', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (106, 281, '2023001006', '茹欣', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV000106580a44', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (107, 282, '2023001007', '衡超', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV000107b53638', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (108, 283, '2023001008', '纪静', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV0001080db725', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (109, 284, '2023001009', '谈龙', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV0001092f917b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (110, 285, '2023001010', '双红', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV00011001342e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (111, 286, '2023001011', '邴斌', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV000111fbc6e2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (112, 287, '2023001012', '水洁', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV0001129441c8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (113, 288, '2023001013', '卞军', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, 'INV000113672452', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:20', 0);
INSERT INTO `student` VALUES (114, 289, '2023001014', '翁艳', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20233TSQZM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (115, 290, '2023001015', '那磊', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023BNX6VR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (116, 291, '2023001016', '游娟', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20234BKXZB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (117, 292, '2023001017', '江伟', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023FQ9ULB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (118, 293, '2023001018', '芮丽', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023KUJSVV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (119, 294, '2023001019', '鲍鹏', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023AQMTPC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (120, 295, '2023001020', '阙芳', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023P5C2TT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (121, 296, '2023001021', '成勇', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023VY22PX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (122, 297, '2023001022', '武倩', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023Y7PL7E', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (123, 298, '2023001023', '常涛', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20238X7ZY5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (124, 299, '2023001024', '戚萍', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023NA7UN7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (125, 300, '2023001025', '乐杰', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20232T8UDS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (126, 301, '2023001026', '时婷', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023J69DLZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (127, 302, '2023001027', '吴峰', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023NLYQZV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (128, 303, '2023001028', '于欣', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023ZT7DHQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (129, 304, '2023001029', '元超', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023H3MDRE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (130, 305, '2023001030', '臧静', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20234AKFAD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (131, 306, '2023001031', '桂龙', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20237QGHMU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (132, 307, '2023001032', '滕红', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023TR9JUB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (133, 308, '2023001033', '殷斌', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20239KWAW5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (134, 309, '2023001034', '荣洁', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023VG7AZJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (135, 310, '2023001035', '涂军', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023LUYEXA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (136, 311, '2023001036', '甘艳', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023U44VJS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (137, 312, '2023001037', '宗磊', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20234ZY4BF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (138, 313, '2023001038', '闻娟', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023LQKH43', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (139, 314, '2023001039', '堵伟', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023Y4YNTU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (140, 315, '2023001040', '连丽', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023LDBTR6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (141, 316, '2023001041', '植鹏', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023GHK39L', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (142, 317, '2023001042', '乔芳', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023LKM3DM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (143, 318, '2023001043', '屈勇', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023MSMLL7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (144, 319, '2023001044', '慕倩', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023Y9GTLH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (145, 320, '2023001045', '封涛', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023H8NYFU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (146, 321, '2023001046', '习萍', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023BDD43Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (147, 322, '2023001047', '左杰', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023F6SXDZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (148, 323, '2023001048', '米婷', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '20236BDYVD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (149, 324, '2023001049', '舒峰', '男', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023ZF9N35', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (150, 325, '2023001050', '欧欣', '女', '计算机科学与技术', '2023', '计科2301', NULL, NULL, NULL, NULL, '2023GXTGNA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (151, 326, '2023001051', '郁超', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023LFUKSW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (152, 327, '2023001052', '葛静', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023M8WFHJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (153, 328, '2023001053', '卓龙', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023L249NG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (154, 329, '2023001054', '宗红', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023RKS82A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (155, 330, '2023001055', '井斌', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023NQBYZA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (156, 331, '2023001056', '秦洁', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023F2KA5M', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (157, 332, '2023001057', '栾军', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023R6M38C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (158, 333, '2023001058', '姬艳', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023QRA646', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (159, 334, '2023001059', '应磊', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023GLWAXR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (160, 335, '2023001060', '穆娟', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '202322SNEW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (161, 336, '2023001061', '居伟', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023KYJAC5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (162, 337, '2023001062', '华丽', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023AY5GFC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (163, 338, '2023001063', '幸鹏', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023SQEX5H', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (164, 339, '2023001064', '左芳', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '2023QTJCU2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (165, 340, '2023001065', '包勇', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, '20233KJD7K', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (166, 341, '2023001066', '伊倩', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00016639883c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (167, 342, '2023001067', '充涛', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000167114b31', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (168, 343, '2023001068', '乐萍', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000168acf1d9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (169, 344, '2023001069', '皮杰', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001694e4e86', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (170, 345, '2023001070', '卫婷', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001701ef835', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (171, 346, '2023001071', '逄峰', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00017163838d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (172, 347, '2023001072', '滕欣', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001725151cb', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (173, 348, '2023001073', '单超', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001736e71a5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (174, 349, '2023001074', '晏静', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000174a68152', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (175, 350, '2023001075', '巩龙', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000175d3469c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (176, 351, '2023001076', '隋红', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000176730aaf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (177, 352, '2023001077', '訾斌', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00017794a887', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (178, 353, '2023001078', '车洁', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001788689ac', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (179, 354, '2023001079', '羊军', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001792217f8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (180, 355, '2023001080', '慕艳', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00018057d022', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (181, 356, '2023001081', '宫磊', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000181286e2e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (182, 357, '2023001082', '国娟', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001820ef442', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (183, 358, '2023001083', '步伟', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000183153a53', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (184, 359, '2023001084', '丁丽', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000184b4f7e9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (185, 360, '2023001085', '元鹏', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001855e7c66', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (186, 361, '2023001086', '井芳', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000186bd8fff', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (187, 362, '2023001087', '怀勇', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001872e1de5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (188, 363, '2023001088', '米倩', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00018861c1fc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (189, 364, '2023001089', '党涛', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001892c88f4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (190, 365, '2023001090', '习萍', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000190fbd50b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (191, 366, '2023001091', '阮杰', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000191af6f58', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (192, 367, '2023001092', '栾婷', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000192f630de', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (193, 368, '2023001093', '胥峰', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001939ea5e9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (194, 369, '2023001094', '能欣', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001941f54b0', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (195, 370, '2023001095', '索超', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000195f0c4ce', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (196, 371, '2023001096', '冉静', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000196480073', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (197, 372, '2023001097', '咸龙', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV0001970e0155', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (198, 373, '2023001098', '寇红', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV00019899f1b2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (199, 374, '2023001099', '赖斌', '男', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000199676c1e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (200, 375, '2023001100', '昌洁', '女', '计算机科学与技术', '2023', '计科2302', NULL, NULL, NULL, NULL, 'INV000200f8385a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (201, 376, '2023002001', '祝杰', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000201fd8015', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (202, 377, '2023002002', '封婷', '女', '软件工程', '2023', '软工2301', NULL, 5, 181, '101', 'INV0002022677e3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', '爱打游戏乐于助人', '[\"爱打游戏\",\"夜猫子\"]', '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (203, 378, '2023002003', '米峰', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, 'INV000203a69eb0', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (204, 379, '2023002004', '舒倩', '女', '软件工程', '2023', '软工2302', NULL, 1, 219, '409', 'INV2004d5ad56da', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (205, 380, '2023002005', '郁超', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000205c8416e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (206, 381, '2023002006', '别静', '女', '软件工程', '2023', '软工2301', NULL, 49, 182, '102', 'INV00020687dcfe', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (207, 382, '2023002007', '厍龙', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000207a3da5a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (208, 383, '2023002008', '蒯红', '女', '软件工程', '2023', '软工2301', NULL, 49, 182, '102', 'INV00020813357d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (209, 384, '2023002009', '吕斌', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000209725b42', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (210, 385, '2023002010', '令洁', '女', '软件工程', '2023', '软工2301', NULL, 50, 183, '103', 'INV0002105ad2cb', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (211, 386, '2023002011', '欧军', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000211de52a6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (212, 387, '2023002012', '和艳', '女', '软件工程', '2023', '软工2301', NULL, 50, 183, '103', 'INV00021202a993', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (213, 388, '2023002013', '劳磊', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000213bad496', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (214, 389, '2023002014', '须娟', '女', '软件工程', '2023', '软工2301', NULL, 51, 184, '104', 'INV000214b90966', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (215, 390, '2023002015', '冷伟', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000215016348', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (216, 391, '2023002016', '屈丽', '女', '软件工程', '2023', '软工2301', NULL, 51, 184, '104', 'INV00021613ae5b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (217, 392, '2023002017', '丛鹏', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002179c78cc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (218, 393, '2023002018', '宗芳', '女', '软件工程', '2023', '软工2301', NULL, 52, 185, '105', 'INV000218d2501d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (219, 394, '2023002019', '曾勇', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002192d4fdc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (220, 395, '2023002020', '班倩', '女', '软件工程', '2023', '软工2301', NULL, 52, 185, '105', 'INV000220beb81b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (221, 396, '2023002021', '原涛', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV00022193943e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (222, 397, '2023002022', '应萍', '女', '软件工程', '2023', '软工2301', NULL, 53, 186, '106', 'INV0002227a03f6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (223, 398, '2023002023', '盛杰', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000223a27f36', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (224, 399, '2023002024', '幸婷', '女', '软件工程', '2023', '软工2301', NULL, 53, 186, '106', 'INV00022460c490', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (225, 400, '2023002025', '危峰', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002256a4302', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (226, 401, '2023002026', '左欣', '女', '软件工程', '2023', '软工2301', NULL, 54, 187, '107', 'INV000226f71dcc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (227, 402, '2023002027', '富超', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000227b21a09', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (228, 403, '2023002028', '初静', '女', '软件工程', '2023', '软工2301', NULL, 54, 187, '107', 'INV00022824026a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (229, 404, '2023002029', '栗龙', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002292101b6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (230, 405, '2023002030', '钮红', '女', '软件工程', '2023', '软工2301', NULL, 55, 188, '108', 'INV0002306a705c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (231, 406, '2023002031', '饶斌', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000231ef80b0', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (232, 407, '2023002032', '訾洁', '女', '软件工程', '2023', '软工2301', NULL, 55, 188, '108', 'INV000232de4d61', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (233, 408, '2023002033', '厉军', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000233dfe4e0', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (234, 409, '2023002034', '利艳', '女', '软件工程', '2023', '软工2301', NULL, 56, 189, '109', 'INV00023494febf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (235, 410, '2023002035', '於磊', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002353261b7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (236, 411, '2023002036', '哈娟', '女', '软件工程', '2023', '软工2301', NULL, 56, 189, '109', 'INV0002369eea48', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (237, 412, '2023002037', '房伟', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000237764f9d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (238, 413, '2023002038', '年丽', '女', '软件工程', '2023', '软工2301', NULL, 57, 190, '110', 'INV0002381c8869', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (239, 414, '2023002039', '钦鹏', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000239deea10', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (240, 415, '2023002040', '乔芳', '女', '软件工程', '2023', '软工2301', NULL, 57, 190, '110', 'INV000240bc97c7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (241, 416, '2023002041', '贝勇', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002413efc27', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (242, 417, '2023002042', '皮倩', '女', '软件工程', '2023', '软工2301', NULL, 58, 191, '201', 'INV00024296a81c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (243, 418, '2023002043', '东涛', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV0002432f670d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (244, 419, '2023002044', '卫萍', '女', '软件工程', '2023', '软工2301', NULL, 58, 191, '201', 'INV000244caf8b4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (245, 420, '2023002045', '司杰', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000245de9069', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (246, 421, '2023002046', '桑婷', '女', '软件工程', '2023', '软工2301', NULL, 59, 192, '202', 'INV000246cb762a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (247, 422, '2023002047', '寿峰', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV00024779ab86', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (248, 423, '2023002048', '通欣', '女', '软件工程', '2023', '软工2301', NULL, 59, 192, '202', 'INV0002482ded96', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (249, 424, '2023002049', '归超', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, 'INV000249c12c30', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (250, 425, '2023002050', '干静', '女', '软件工程', '2023', '软工2301', NULL, 60, 193, '203', 'INV000250a1cde2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (251, 426, '2023002051', '况龙', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, 'INV0002512cb9bd', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (252, 427, '2023002052', '靳红', '女', '软件工程', '2023', '软工2302', NULL, 42, 222, '502', 'INV000252b06a95', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (253, 428, '2023002053', '广斌', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, 'INV00025356deed', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (254, 429, '2023002054', '蔚洁', '女', '软件工程', '2023', '软工2302', NULL, 42, 222, '502', 'INV0002541efc26', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (255, 430, '2023002055', '蓬军', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, 'INV000255d60348', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (257, 432, '2023002057', '郦磊', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023774H2P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (259, 434, '2023002059', '养伟', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023QHRD9G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (261, 436, '2023002061', '刁鹏', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023UYRKFH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (263, 438, '2023002063', '修勇', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023YDBRN9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (265, 440, '2023002065', '充涛', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023JW45GM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (267, 442, '2023002067', '爱杰', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20237TD5CD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (268, 443, '2023002068', '苏婷', '女', '软件工程', '2023', '软工2302', NULL, 1, 219, '409', '2023BUZVTG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', '爱吃爱玩', '[\"爱打游戏\",\"夜猫子\"]', '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (269, 444, '2023002069', '向峰', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20234HW8Z3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (270, 445, '2023002070', '后欣', '女', '软件工程', '2023', '软工2302', NULL, 7, 221, '501', '2023GGSKQR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (271, 446, '2023002071', '红超', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '202394YZ7B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (272, 447, '2023002072', '益静', '女', '软件工程', '2023', '软工2302', NULL, 7, 221, '501', '20238ZBT6A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (273, 448, '2023002073', '皇龙', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023QD67YH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (274, 449, '2023002074', '甫红', '女', '软件工程', '2023', '软工2302', NULL, 6, 220, '410', '20236RT4SW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (275, 450, '2023002075', '百斌', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023XLHX7B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (276, 451, '2023002076', '里洁', '女', '软件工程', '2023', '软工2302', NULL, 6, 220, '410', '2023UMT2XY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (277, 452, '2023002077', '衣军', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023MQFAVV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (278, 453, '2023002078', '闻艳', '女', '软件工程', '2023', '软工2302', NULL, 43, 223, '503', '2023E532TT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (279, 454, '2023002079', '堵磊', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20236ZU7BK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (280, 455, '2023002080', '连娟', '女', '软件工程', '2023', '软工2302', NULL, 43, 223, '503', '2023QVPSY8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (281, 456, '2023002081', '刘伟', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20237MPA53', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (282, 457, '2023002082', '赵丽', '女', '软件工程', '2023', '软工2302', NULL, 44, 224, '504', '2023ABER6T', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (283, 458, '2023002083', '钱鹏', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023J2CSFH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (284, 459, '2023002084', '孙芳', '女', '软件工程', '2023', '软工2302', NULL, 44, 224, '504', '20234HVJ6N', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (285, 460, '2023002085', '李勇', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023N9EWXT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (286, 461, '2023002086', '周倩', '女', '软件工程', '2023', '软工2302', NULL, 45, 225, '505', '2023NECTMA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (287, 462, '2023002087', '吴涛', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023CEH3RR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (288, 463, '2023002088', '郑萍', '女', '软件工程', '2023', '软工2302', NULL, 45, 225, '505', '20235RUBSR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (289, 464, '2023002089', '王杰', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023VLPZBU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (290, 465, '2023002090', '冯婷', '女', '软件工程', '2023', '软工2302', NULL, 46, 226, '506', '2023KHXNAP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (291, 466, '2023002091', '陈峰', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023XNTCBX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (292, 467, '2023002092', '褚欣', '女', '软件工程', '2023', '软工2302', NULL, 46, 226, '506', '2023SVWJGP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (293, 468, '2023002093', '卫超', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20233HWX77', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (294, 469, '2023002094', '蒋静', '女', '软件工程', '2023', '软工2302', NULL, 47, 227, '507', '2023D8V8AF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (295, 470, '2023002095', '沈龙', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '20232Y3PW8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (296, 471, '2023002096', '韩红', '女', '软件工程', '2023', '软工2302', NULL, 47, 227, '507', '202357UCNN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (297, 472, '2023002097', '杨斌', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023SACCUF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (298, 473, '2023002098', '朱洁', '女', '软件工程', '2023', '软工2302', NULL, 48, 228, '508', '2023GCHUMN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (299, 474, '2023002099', '秦军', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2023WKZPVN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (300, 475, '2023002100', '尤艳', '女', '软件工程', '2023', '软工2302', NULL, 48, 228, '508', '2023LP7NJL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (301, 476, '2023003001', '欧勇', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023LMCF3A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (302, 477, '2023003002', '郁欣', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023B77H46', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (303, 478, '2023003003', '葛超', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023KB99CE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (304, 479, '2023003004', '卓静', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023YYMBHL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (305, 480, '2023003005', '许磊', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023STBJHF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (306, 481, '2023003006', '何娟', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20239FWRV9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (307, 482, '2023003007', '吕伟', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20234V95GX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (308, 483, '2023003008', '施丽', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023XKLUUN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (309, 484, '2023003009', '张鹏', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023VF23ZE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (310, 485, '2023003010', '孔芳', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023SP8ND7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (311, 486, '2023003011', '曹勇', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023KJGB3P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (312, 487, '2023003012', '严倩', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023H9V32S', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (313, 488, '2023003013', '华涛', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20234RAVMG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (314, 489, '2023003014', '金萍', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023H2H78S', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (315, 490, '2023003015', '魏杰', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20233H5SBG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (316, 491, '2023003016', '陶婷', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20232D7E92', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (317, 492, '2023003017', '姜峰', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023L288BN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (318, 493, '2023003018', '戚欣', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20237D2NZ4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (319, 494, '2023003019', '谢超', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023Q2457W', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (320, 495, '2023003020', '邹静', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023EYDWV3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (321, 496, '2023003021', '喻龙', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023J8WBST', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (322, 497, '2023003022', '柏红', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20239PFMMW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (323, 498, '2023003023', '水斌', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023F5S8HU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (324, 499, '2023003024', '窦洁', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20236VMPWP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (325, 500, '2023003025', '章军', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023MT5MTP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (326, 501, '2023003026', '云艳', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023R5M5Y6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (327, 502, '2023003027', '苏磊', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023SVZ7FT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (328, 503, '2023003028', '潘娟', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20238PHP85', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (329, 504, '2023003029', '葛伟', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023L5NYRA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (330, 505, '2023003030', '奚丽', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023KXCD8E', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (331, 506, '2023003031', '范鹏', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023F4Q62C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (332, 507, '2023003032', '彭芳', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023F9CADX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (333, 508, '2023003033', '郎勇', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023X58R8G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (334, 509, '2023003034', '鲁倩', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023JMFARS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (335, 510, '2023003035', '钱涛', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20233EUUQH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (336, 511, '2023003036', '钟萍', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023XFL3BC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (337, 512, '2023003037', '盛杰', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20233YQ8GV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (338, 513, '2023003038', '汤婷', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023UW2PAV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (339, 514, '2023003039', '陆峰', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023J4MXDN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (340, 515, '2023003040', '安欣', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023F8FP9B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (341, 516, '2023003041', '齐超', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023P5FMS8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (342, 517, '2023003042', '皮静', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '202354LBQ9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (343, 518, '2023003043', '卞龙', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023EQR88T', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (344, 519, '2023003044', '舒红', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023V7NJZ7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (345, 520, '2023003045', '苗斌', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '202395NUJW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (346, 521, '2023003046', '凌洁', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023WQY38P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (347, 522, '2023003047', '包军', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023JY8WW2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (348, 523, '2023003048', '柏艳', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023E8TSWB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (349, 524, '2023003049', '耿磊', '男', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '2023EA7T58', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (350, 525, '2023003050', '关娟', '女', '数据科学与大数据技术', '2023', '数据2301', NULL, NULL, NULL, NULL, '20237CNVVV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (351, 526, '2023003051', '明伟', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '202398SC2B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (352, 527, '2023003052', '臧丽', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023YGKKE3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (353, 528, '2023003053', '伏鹏', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023PHFH3Q', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (354, 529, '2023003054', '宣芳', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023ZG8BGS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (355, 530, '2023003055', '童勇', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20238XQ2QX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (356, 531, '2023003056', '赖倩', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20235VXCSW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (357, 532, '2023003057', '祝涛', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20233PUPFY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (358, 533, '2023003058', '阮萍', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023K74Y5P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (359, 534, '2023003059', '毕杰', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023RQGJJL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (360, 535, '2023003060', '龚婷', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023ZRTHRY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (361, 536, '2023003061', '汪峰', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023QQCG42', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (362, 537, '2023003062', '植欣', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023UKQEMW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (363, 538, '2023003063', '融超', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023RVE6AG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (364, 539, '2023003064', '郁静', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '202326DPZC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (365, 540, '2023003065', '权龙', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023YKXAYE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (366, 541, '2023003066', '蒙红', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20239CVBLL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (367, 542, '2023003067', '穆斌', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023XV2P7H', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (368, 543, '2023003068', '别洁', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023UDF4B3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (369, 544, '2023003069', '游军', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20237L6XM9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (370, 545, '2023003070', '席艳', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023GM5R5G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (371, 546, '2023003071', '隆磊', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20239XBSR3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (372, 547, '2023003072', '晏娟', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023NRWW77', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (373, 548, '2023003073', '边伟', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023KLYN7F', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (374, 549, '2023003074', '麴丽', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20236TPQCV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (375, 550, '2023003075', '殷鹏', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20234KRG4Z', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (376, 551, '2023003076', '国芳', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023M9XNVZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (377, 552, '2023003077', '欧勇', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023N93B6B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (378, 553, '2023003078', '文倩', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023UTQ5HT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (379, 554, '2023003079', '冉涛', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023SHQZNP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (380, 555, '2023003080', '寿萍', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023U2XDT9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (381, 556, '2023003081', '解杰', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023KQMX6L', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (382, 557, '2023003082', '简婷', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023GCAV6G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (383, 558, '2023003083', '应峰', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023DVGDX4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (384, 559, '2023003084', '荆欣', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023ECFEMG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (385, 560, '2023003085', '饶超', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '202342L6DR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (386, 561, '2023003086', '宁静', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023TEWES7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (387, 562, '2023003087', '储龙', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023NJVKZ6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (388, 563, '2023003088', '桓红', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023VUCREM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (389, 564, '2023003089', '井斌', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023AUNHRW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (390, 565, '2023003090', '麻洁', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023FVQP7J', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (391, 566, '2023003091', '卜军', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023297F5C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (392, 567, '2023003092', '扈艳', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023GQX6ZZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (393, 568, '2023003093', '单磊', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023AFEVMR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (394, 569, '2023003094', '管娟', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023AX5XLJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (395, 570, '2023003095', '戎伟', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023LZRAL8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (396, 571, '2023003096', '郜丽', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '20238KA28Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (397, 572, '2023003097', '屈鹏', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023VNGBD2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (398, 573, '2023003098', '幸芳', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023L7LPY9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (399, 574, '2023003099', '姬勇', '男', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023CJMGM7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (400, 575, '2023003100', '能倩', '女', '数据科学与大数据技术', '2023', '数据2302', NULL, NULL, NULL, NULL, '2023GLMN65', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (401, 576, '2023004001', '宗龙', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023ZQY7Z4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (402, 577, '2023004002', '井红', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '202358Z49M', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (403, 578, '2023004003', '秦斌', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, '2023Y2KKKQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (404, 579, '2023004004', '栾洁', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, '2023UUG676', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (405, 580, '2023004005', '慕涛', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023VZHDVX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (406, 581, '2023004006', '居萍', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023A2NH4H', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (407, 582, '2023004007', '习杰', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023HFGMSD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (408, 583, '2023004008', '党婷', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023DFZSL2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (409, 584, '2023004009', '米峰', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023B9MW4M', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (410, 585, '2023004010', '栾欣', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023GMR3PT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (411, 586, '2023004011', '阳超', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023KNWMPU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (412, 587, '2023004012', '充静', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023S3E5M8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (413, 588, '2023004013', '封龙', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023FNARRE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (414, 589, '2023004014', '左红', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '202325C2UG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (415, 590, '2023004015', '舒斌', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '20238F5SYF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (416, 591, '2023004016', '虞洁', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023QDDMGL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (417, 592, '2023004017', '皮军', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023PA4GLV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (418, 593, '2023004018', '乐艳', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023ECE5TC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (419, 594, '2023004019', '卫磊', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023CLJ5Q8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (420, 595, '2023004020', '逄娟', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023FPB4EA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (421, 596, '2023004021', '滕伟', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2023XD34QS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (422, 597, '2023004022', '伊丽', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004220c8436', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (423, 598, '2023004023', '单鹏', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004234ec9d7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (424, 599, '2023004024', '晏芳', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004246f436b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (425, 600, '2023004025', '巩勇', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV00042528f25e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (426, 601, '2023004026', '隋倩', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000426fadee7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (427, 602, '2023004027', '訾涛', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004275a20a1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', '', '[\"爱打游戏\"]', '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (428, 603, '2023004028', '车萍', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004289e514e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (429, 604, '2023004029', '羊杰', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000429c74ca5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (430, 605, '2023004030', '慕婷', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000430c657ef', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (431, 606, '2023004031', '宫峰', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000431dbf88c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (432, 607, '2023004032', '国欣', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV00043285f517', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (433, 608, '2023004033', '步超', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000433cb79c6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (434, 609, '2023004034', '丁静', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000434cd9205', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (435, 610, '2023004035', '元龙', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000435e0ceea', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (436, 611, '2023004036', '井红', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000436f67b21', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (437, 612, '2023004037', '怀斌', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000437c76cd7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (438, 613, '2023004038', '米洁', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000438570dff', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (439, 614, '2023004039', '党军', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004399d4625', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (440, 615, '2023004040', '习艳', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000440c5f3c8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (441, 616, '2023004041', '阮磊', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000441d9a4e4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (442, 617, '2023004042', '栾娟', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004425c4a77', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (443, 618, '2023004043', '胥伟', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000443d86a41', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (444, 619, '2023004044', '能丽', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000444bb966f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (445, 620, '2023004045', '索鹏', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000445679da1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (446, 621, '2023004046', '冉芳', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV00044661358a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (447, 622, '2023004047', '咸勇', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004473afbe3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (448, 623, '2023004048', '寇倩', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000448bd70dd', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (449, 624, '2023004049', '赖涛', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV000449dafb7d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (450, 625, '2023004050', '昌萍', '女', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, 'INV0004502d03f7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (451, 626, '2023004051', '郁杰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000451d18eeb', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (452, 627, '2023004052', '别婷', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000452fca3a5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (453, 628, '2023004053', '厍峰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000453cb54b7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (454, 629, '2023004054', '蒯欣', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV00045452e124', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (455, 630, '2023004055', '吕超', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000455508c5d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (456, 631, '2023004056', '令静', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004564f8040', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (457, 632, '2023004057', '欧龙', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004575cf3b3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (458, 633, '2023004058', '和红', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004583a9310', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (459, 634, '2023004059', '劳斌', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004597a3938', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (460, 635, '2023004060', '须洁', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000460f36441', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (461, 636, '2023004061', '冷军', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000461668806', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (462, 637, '2023004062', '屈艳', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000462c5b3d2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (463, 638, '2023004063', '丛磊', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000463a5670b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (464, 639, '2023004064', '宗娟', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004647f4018', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (465, 640, '2023004065', '曾伟', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004650a47d7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (466, 641, '2023004066', '班丽', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000466afaf08', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (467, 642, '2023004067', '原鹏', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004679629fa', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (468, 643, '2023004068', '应芳', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000468c5c38d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (469, 644, '2023004069', '盛勇', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004698f7500', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (470, 645, '2023004070', '幸倩', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000470380631', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (471, 646, '2023004071', '危涛', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004717c00e1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (472, 647, '2023004072', '左萍', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004728de412', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (473, 648, '2023004073', '富杰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000473613d9b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (474, 649, '2023004074', '初婷', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000474562958', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (475, 650, '2023004075', '栗峰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000475007ceb', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (476, 651, '2023004076', '钮欣', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004761b5795', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (477, 652, '2023004077', '饶超', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000477f44839', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (478, 653, '2023004078', '訾静', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000478536794', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (479, 654, '2023004079', '厉龙', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000479b7aa5c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (480, 655, '2023004080', '利红', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000480c6e62d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (481, 656, '2023004081', '於斌', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000481aef98f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (482, 657, '2023004082', '哈洁', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV00048294ad92', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (483, 658, '2023004083', '房军', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000483528d8f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (484, 659, '2023004084', '年艳', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004844b0739', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (485, 660, '2023004085', '钦磊', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000485d0a510', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (486, 661, '2023004086', '乔娟', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000486850330', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (487, 662, '2023004087', '贝伟', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV00048791b37c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (488, 663, '2023004088', '皮丽', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000488f98087', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (489, 664, '2023004089', '东鹏', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000489e3d808', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (490, 665, '2023004090', '卫芳', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV00049058fea4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (491, 666, '2023004091', '司勇', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000491ad0abd', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (492, 667, '2023004092', '桑倩', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000492a7addf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (493, 668, '2023004093', '寿涛', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000493df1cfc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (494, 669, '2023004094', '通萍', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000494a2d609', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (495, 670, '2023004095', '归杰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV0004952ce185', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (496, 671, '2023004096', '干婷', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000496103f03', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (497, 672, '2023004097', '况峰', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000497b09ec5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (498, 673, '2023004098', '靳欣', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000498ed93d1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (499, 674, '2023004099', '广超', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000499a58a5c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (500, 675, '2023004100', '蔚静', '女', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, 'INV000500b795dd', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (501, 676, '2024001005', '林海', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV0005016e3640', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (502, 677, '2024001006', '董雪', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000502200f91', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (503, 678, '2024001007', '郝强', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV00050367c76c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (504, 679, '2024001008', '汤丽', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000504a01b23', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (505, 680, '2024001009', '邹鹏', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV0005050475ce', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (506, 681, '2024001010', '熊敏', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000506c36484', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (507, 682, '2024001011', '武涛', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000507151cab', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (508, 683, '2024001012', '康娜', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000508f48766', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (509, 684, '2024001013', '于杰', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV00050991a45f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (510, 685, '2024001014', '苏婷', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV0005103c239b', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (511, 686, '2024001015', '范峰', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, 'INV000511050a50', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (512, 687, '2024001016', '白欣', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024MQK5ER', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (513, 688, '2024001017', '孟超', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024GCL9M8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (514, 689, '2024001018', '常静', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024BSNBDC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (515, 690, '2024001019', '江龙', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20245NN2YA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (516, 691, '2024001020', '施红', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024HV94CK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (517, 692, '2024001021', '柳斌', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024TWV8DG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (518, 693, '2024001022', '查洁', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024JM9AWL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (519, 694, '2024001023', '俞军', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024N3TMPC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (520, 695, '2024001024', '姜艳', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20248JEFRY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (521, 696, '2024001025', '凌磊', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024P9WF94', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (522, 697, '2024001026', '齐娟', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024JQJ4MJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (523, 698, '2024001027', '薛伟', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024GERHVK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (524, 699, '2024001028', '费丽', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024CRW4SW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (525, 700, '2024001029', '庄鹏', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024XD3F9K', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (526, 701, '2024001030', '成芳', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024V3HJXS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (527, 702, '2024001031', '顾勇', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024NSUTEY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (528, 703, '2024001032', '卜倩', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024D4Z68C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (529, 704, '2024001033', '倪涛', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024NKAL4P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (530, 705, '2024001034', '荣萍', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024RKK99C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (531, 706, '2024001035', '季杰', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024WJTR5L', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (532, 707, '2024001036', '简婷', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024R5LXCJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (533, 708, '2024001037', '阎峰', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024JBQDV5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (534, 709, '2024001038', '蓝欣', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024Y4PQ9T', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (535, 710, '2024001039', '易超', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '202454K78C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (536, 711, '2024001040', '温静', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20246JHX9E', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (537, 712, '2024001041', '游龙', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024FCDBYD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (538, 713, '2024001042', '农红', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '202449LLQZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (539, 714, '2024001043', '卞斌', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024359DMY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (540, 715, '2024001044', '尚洁', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20249D77NR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (541, 716, '2024001045', '席军', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024VLRK5W', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (542, 717, '2024001046', '练艳', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20245833HG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (543, 718, '2024001047', '晋磊', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024633F8V', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (544, 719, '2024001048', '冼娟', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '2024EJD64H', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (545, 720, '2024001049', '管伟', '男', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '202433ZAT5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (546, 721, '2024001050', '裴丽', '女', '计算机科学与技术', '2024', '计科2401', NULL, NULL, NULL, NULL, '20249S4AND', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (547, 722, '2024001051', '安鹏', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024PQ6XDG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (548, 723, '2024001052', '柯芳', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024XGR5XV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (549, 724, '2024001053', '芦勇', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024YM5RN2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (550, 725, '2024001054', '花倩', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024V6AN3L', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (551, 726, '2024001055', '朱涛', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024TWUTLX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (552, 727, '2024001056', '甄萍', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024E473XE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (553, 728, '2024001057', '娄杰', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024XPTDBS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (554, 729, '2024001058', '曲婷', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024HXYEXR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (555, 730, '2024001059', '苗峰', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20244W42HK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (556, 731, '2024001060', '符欣', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024H9FSQD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (557, 732, '2024001061', '解超', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024RKLTHS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (558, 733, '2024001062', '储静', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20247P7YDD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (559, 734, '2024001063', '路龙', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20248UT8DX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (560, 735, '2024001064', '权红', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024XBRZMS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (561, 736, '2024001065', '糜斌', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024ERLAZ9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (562, 737, '2024001066', '仇洁', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024HB558M', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (563, 738, '2024001067', '景军', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024R339WW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (564, 739, '2024001068', '党艳', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024JD5AX7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (565, 740, '2024001069', '裘磊', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20246HL4QR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (566, 741, '2024001070', '巫娟', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024SUN5GP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (567, 742, '2024001071', '扈伟', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024SR3B2N', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (568, 743, '2024001072', '岑丽', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024R4FRWY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (569, 744, '2024001073', '楼鹏', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024AEK4BT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (570, 745, '2024001074', '闵芳', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20246W7TUL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (571, 746, '2024001075', '殷勇', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024RMVA4D', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (572, 747, '2024001076', '戚倩', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024MUR8PS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (573, 748, '2024001077', '聂涛', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024ZTZWFJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (574, 749, '2024001078', '喻萍', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20244XX7YW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (575, 750, '2024001079', '师杰', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024R94JGS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (576, 751, '2024001080', '翁婷', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024VQ47AB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (577, 752, '2024001081', '洪峰', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024AJC3YV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (578, 753, '2024001082', '颜欣', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024WGMUKY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (579, 754, '2024001083', '宁超', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '202449WEF7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (580, 755, '2024001084', '祁静', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024BNNBTH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (581, 756, '2024001085', '柴龙', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024HP8SZG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (582, 757, '2024001086', '燕红', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024XGC7GG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (583, 758, '2024001087', '古斌', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024MWMRZH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (584, 759, '2024001088', '籍洁', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024KP852V', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (585, 760, '2024001089', '禹军', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024TNYV9D', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (586, 761, '2024001090', '边艳', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024LEDSS5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (587, 762, '2024001091', '戴磊', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024RTHUZ3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (588, 763, '2024001092', '申娟', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20242NVXVT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (589, 764, '2024001093', '漆伟', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20245Z5BBX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (590, 765, '2024001094', '莫丽', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024FM63HK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (591, 766, '2024001095', '蒲鹏', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024WMJPDP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (592, 767, '2024001096', '殳芳', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20243MU5LU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (593, 768, '2024001097', '郭勇', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024FGQ66A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (594, 769, '2024001098', '贝倩', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '2024WXRT62', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (595, 770, '2024001099', '尤涛', '男', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20245EQBER', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (596, 771, '2024001100', '仲萍', '女', '计算机科学与技术', '2024', '计科2402', NULL, NULL, NULL, NULL, '20247CS53N', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (597, 772, '2024002005', '柏杰', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024J7HCQJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (598, 773, '2024002006', '伍婷', '女', '软件工程', '2024', '软工2401', NULL, 4, 194, '204', '2024RJ9LR6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (599, 774, '2024002007', '焦峰', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024W5KDUF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (600, 775, '2024002008', '桂欣', '女', '软件工程', '2024', '软工2401', NULL, 31, 195, '205', '2024AM3KMZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (601, 776, '2024002009', '衡超', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024DXM7WR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (602, 777, '2024002010', '鲁静', '女', '软件工程', '2024', '软工2401', NULL, 31, 195, '205', '2024WQPJT3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (603, 778, '2024002011', '全龙', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20249G8G26', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (604, 779, '2024002012', '阳红', '女', '软件工程', '2024', '软工2401', NULL, 32, 196, '206', '2024XUTY5Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (605, 780, '2024002013', '鞠斌', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20245NHXKH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (606, 781, '2024002014', '艾洁', '女', '软件工程', '2024', '软工2401', NULL, 32, 196, '206', '2024LWCNVY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (607, 782, '2024002015', '荀军', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024KMKNZX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (608, 783, '2024002016', '虞艳', '女', '软件工程', '2024', '软工2401', NULL, 33, 197, '207', '2024L98QD4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (609, 784, '2024002017', '郎磊', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024B7RN2Z', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (610, 785, '2024002018', '佟娟', '女', '软件工程', '2024', '软工2401', NULL, 33, 197, '207', '2024W7Y763', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (611, 786, '2024002019', '束伟', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024YZC79A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (612, 787, '2024002020', '容丽', '女', '软件工程', '2024', '软工2401', NULL, 34, 198, '208', '2024DV998Q', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (613, 788, '2024002021', '涂鹏', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024Y6FSJ6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (614, 789, '2024002022', '井芳', '女', '软件工程', '2024', '软工2401', NULL, 34, 198, '208', '2024VSDZBG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (615, 790, '2024002023', '时勇', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024X2YYD7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (616, 791, '2024002024', '项倩', '女', '软件工程', '2024', '软工2401', NULL, 35, 199, '209', '2024AKKMNA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (617, 792, '2024002025', '庞涛', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024MLNKQC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (618, 793, '2024002026', '慎萍', '女', '软件工程', '2024', '软工2401', NULL, 35, 199, '209', '2024YS6SP4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (619, 794, '2024002027', '雍杰', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20248Q534Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (620, 795, '2024002028', '晁婷', '女', '软件工程', '2024', '软工2401', NULL, 36, 200, '210', '2024Q7SAQC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (621, 796, '2024002029', '顾峰', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024WCDRRH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (622, 797, '2024002030', '暨欣', '女', '软件工程', '2024', '软工2401', NULL, 36, 200, '210', '20249JLWJQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (623, 798, '2024002031', '辛超', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024TMWP2X', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (624, 799, '2024002032', '松静', '女', '软件工程', '2024', '软工2401', NULL, 37, 201, '301', '2024DQ2C7N', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (625, 800, '2024002033', '厉龙', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20247AZCZM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (626, 801, '2024002034', '印红', '女', '软件工程', '2024', '软工2401', NULL, 37, 201, '301', '2024PDMY7N', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (627, 802, '2024002035', '宿斌', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024FS5CLV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (628, 803, '2024002036', '郜洁', '女', '软件工程', '2024', '软工2401', NULL, 38, 202, '302', '2024DD7PHC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (629, 804, '2024002037', '隆军', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024BVGREC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (630, 805, '2024002038', '东艳', '女', '软件工程', '2024', '软工2401', NULL, 38, 202, '302', '2024MCME2E', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (631, 806, '2024002039', '车磊', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20245AQ48Z', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (632, 807, '2024002040', '平娟', '女', '软件工程', '2024', '软工2401', NULL, 39, 203, '303', '2024BPVWB5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (633, 808, '2024002041', '巩伟', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024FBJ9QY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (634, 809, '2024002042', '奚丽', '女', '软件工程', '2024', '软工2401', NULL, 39, 203, '303', '20247KAZ46', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (635, 810, '2024002043', '邢鹏', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '20247E66GT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (636, 811, '2024002044', '商芳', '女', '软件工程', '2024', '软工2401', NULL, 40, 204, '304', '2024QAMZQZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (637, 812, '2024002045', '戈勇', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024LQG7HT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (638, 813, '2024002046', '滑倩', '女', '软件工程', '2024', '软工2401', NULL, 40, 204, '304', '2024VA6X47', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (639, 814, '2024002047', '毕涛', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024LD9JWQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (640, 815, '2024002048', '翟萍', '女', '软件工程', '2024', '软工2401', NULL, 41, 205, '305', '2024MEUY5K', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (641, 816, '2024002049', '连杰', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2024WNSN4C', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (642, 817, '2024002050', '强婷', '女', '软件工程', '2024', '软工2401', NULL, 41, 205, '305', '2024JT789R', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (643, 818, '2024002051', '匡峰', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024S5KUY3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (644, 819, '2024002052', '苟欣', '女', '软件工程', '2024', '软工2402', NULL, 18, 206, '306', '2024PE57AQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (645, 820, '2024002053', '相超', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024EA7S3Q', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (646, 821, '2024002054', '麻静', '女', '软件工程', '2024', '软工2402', NULL, 18, 206, '306', '2024W9FPPK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (647, 822, '2024002055', '宫龙', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024RSXJAJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (648, 823, '2024002056', '国红', '女', '软件工程', '2024', '软工2402', NULL, 19, 207, '307', '2024JEARXC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (649, 824, '2024002057', '步斌', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024UK9JFH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (650, 825, '2024002058', '丁洁', '女', '软件工程', '2024', '软工2402', NULL, 19, 207, '307', '2024X7TW5V', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (651, 826, '2024002059', '元军', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024U6C95A', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (652, 827, '2024002060', '井艳', '女', '软件工程', '2024', '软工2402', NULL, 20, 208, '308', '20244CCFXM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (653, 828, '2024002061', '怀磊', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024NRJ8RK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (654, 829, '2024002062', '米娟', '女', '软件工程', '2024', '软工2402', NULL, 20, 208, '308', '2024EYE7MR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (655, 830, '2024002063', '党伟', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024DH6PNZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (656, 831, '2024002064', '习丽', '女', '软件工程', '2024', '软工2402', NULL, 21, 209, '309', '2024AWGKFN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (657, 832, '2024002065', '阮鹏', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024BH43LX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (658, 833, '2024002066', '栾芳', '女', '软件工程', '2024', '软工2402', NULL, 21, 209, '309', '20246UUZER', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (659, 834, '2024002067', '胥勇', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024Q3MPWB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (660, 835, '2024002068', '能倩', '女', '软件工程', '2024', '软工2402', NULL, 22, 210, '310', '2024J28TRS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (661, 836, '2024002069', '索涛', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024F9A5A9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (662, 837, '2024002070', '冉萍', '女', '软件工程', '2024', '软工2402', NULL, 22, 210, '310', '2024YT4PQ9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (663, 838, '2024002071', '咸杰', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024L69MA3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (664, 839, '2024002072', '寇婷', '女', '软件工程', '2024', '软工2402', NULL, 23, 211, '401', '20242SN9ZZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (665, 840, '2024002073', '赖峰', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024QAUYHW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (666, 841, '2024002074', '昌欣', '女', '软件工程', '2024', '软工2402', NULL, 23, 211, '401', '20242B5VZB', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (667, 842, '2024002075', '郁超', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024JFAQLJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (668, 843, '2024002076', '别静', '女', '软件工程', '2024', '软工2402', NULL, 24, 212, '402', '2024GBSXNC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (669, 844, '2024002077', '厍龙', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024TKG6B9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (670, 845, '2024002078', '蒯红', '女', '软件工程', '2024', '软工2402', NULL, 24, 212, '402', '2024KEWSWM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (671, 846, '2024002079', '吕斌', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024KH9YS2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (672, 847, '2024002080', '令洁', '女', '软件工程', '2024', '软工2402', NULL, 25, 213, '403', '2024CACTVP', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (673, 848, '2024002081', '欧军', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024ZBUZDG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (674, 849, '2024002082', '和艳', '女', '软件工程', '2024', '软工2402', NULL, 25, 213, '403', '2024JBYDDM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (675, 850, '2024002083', '劳磊', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '2024S37DMN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (676, 851, '2024002084', '须娟', '女', '软件工程', '2024', '软工2402', NULL, 26, 214, '404', '2024KYZ955', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (677, 852, '2024002085', '冷伟', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV0006772c52b6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (678, 853, '2024002086', '屈丽', '女', '软件工程', '2024', '软工2402', NULL, 26, 214, '404', 'INV00067893d8ee', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (679, 854, '2024002087', '丛鹏', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV0006794189bf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (680, 855, '2024002088', '宗芳', '女', '软件工程', '2024', '软工2402', NULL, 27, 215, '405', 'INV000680b9b057', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (681, 856, '2024002089', '曾勇', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV000681d64e01', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (682, 857, '2024002090', '班倩', '女', '软件工程', '2024', '软工2402', NULL, 27, 215, '405', 'INV00068252e27c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (683, 858, '2024002091', '原涛', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV000683eca600', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (684, 859, '2024002092', '应萍', '女', '软件工程', '2024', '软工2402', NULL, 28, 216, '406', 'INV0006849b5ef3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (685, 860, '2024002093', '盛杰', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV0006858adb12', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (686, 861, '2024002094', '幸婷', '女', '软件工程', '2024', '软工2402', NULL, 28, 216, '406', 'INV000686a574b1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (687, 862, '2024002095', '危峰', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV000687fdf2f2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (688, 863, '2024002096', '左欣', '女', '软件工程', '2024', '软工2402', NULL, 29, 217, '407', 'INV00068836ec13', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (689, 864, '2024002097', '富超', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV0006898054bf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (690, 865, '2024002098', '初静', '女', '软件工程', '2024', '软工2402', NULL, 29, 217, '407', 'INV000690742631', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (691, 866, '2024002099', '栗龙', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, 'INV000691270dd6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (692, 867, '2024002100', '钮红', '女', '软件工程', '2024', '软工2402', NULL, 30, 218, '408', 'INV00069248815f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (693, 868, '2024003005', '饶斌', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00069390b10d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (694, 869, '2024003006', '訾洁', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000694e96b13', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (695, 870, '2024003007', '厉军', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000695d6e111', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (696, 871, '2024003008', '利艳', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000696af5f89', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (697, 872, '2024003009', '於磊', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000697927156', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (698, 873, '2024003010', '哈娟', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000698d91d97', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (699, 874, '2024003011', '房伟', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00069941fd11', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (700, 875, '2024003012', '年丽', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000700c10cdb', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (701, 876, '2024003013', '钦鹏', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007010a7f57', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (702, 877, '2024003014', '乔芳', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000702e706c6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (703, 878, '2024003015', '贝勇', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000703ca8766', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (704, 879, '2024003016', '皮倩', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007041f0608', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (705, 880, '2024003017', '东涛', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00070597ccd5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (706, 881, '2024003018', '卫萍', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000706a4eee2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (707, 882, '2024003019', '司杰', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007079e1940', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (708, 883, '2024003020', '桑婷', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000708712abf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (709, 884, '2024003021', '寿峰', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000709f0dcae', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (710, 885, '2024003022', '通欣', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000710f701cf', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (711, 886, '2024003023', '归超', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000711ffde70', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (712, 887, '2024003024', '干静', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007126e6bc7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (713, 888, '2024003025', '况龙', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00071354ac9e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (714, 889, '2024003026', '靳红', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000714efb630', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (715, 890, '2024003027', '广斌', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000715198d2d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (716, 891, '2024003028', '蔚洁', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000716c8a4d5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (717, 892, '2024003029', '蓬军', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00071747f152', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (718, 893, '2024003030', '隗艳', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000718aabd48', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (719, 894, '2024003031', '郦磊', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000719bd364c', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (720, 895, '2024003032', '怡娟', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000720560c31', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (721, 896, '2024003033', '养伟', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00072123b6d5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (722, 897, '2024003034', '红丽', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00072298d83e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (723, 898, '2024003035', '刁鹏', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000723892862', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (724, 899, '2024003036', '鱼芳', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007241e520e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (725, 900, '2024003037', '修勇', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000725db6bb5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (726, 901, '2024003038', '揭倩', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007260669f3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (727, 902, '2024003039', '充涛', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000727641350', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (728, 903, '2024003040', '茅萍', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00072878d272', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (729, 904, '2024003041', '爱杰', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000729a5bf15', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (730, 905, '2024003042', '苏婷', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000730a921d4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (731, 906, '2024003043', '向峰', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000731e43101', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (732, 907, '2024003044', '后欣', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV00073247344e', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (733, 908, '2024003045', '红超', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000733420d83', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (734, 909, '2024003046', '益静', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007341cad28', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (735, 910, '2024003047', '皇龙', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000735b0b086', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (736, 911, '2024003048', '甫红', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV0007367655b0', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (737, 912, '2024003049', '百斌', '男', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000737a504d6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (738, 913, '2024003050', '里洁', '女', '数据科学与大数据技术', '2024', '数据2401', NULL, NULL, NULL, NULL, 'INV000738126bdc', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (739, 914, '2024003051', '衣军', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00073937a1e4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (740, 915, '2024003052', '闻艳', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000740fb3088', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (741, 916, '2024003053', '堵磊', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000741b0cd98', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (742, 917, '2024003054', '连娟', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000742789b91', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (743, 918, '2024003055', '澹台伟', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00074360c505', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (744, 919, '2024003056', '第五丽', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000744759bf4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (745, 920, '2024003057', '刘鹏', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007455411e3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (746, 921, '2024003058', '赵芳', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007467c5927', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (747, 922, '2024003059', '钱勇', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000747dfc42f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (748, 923, '2024003060', '孙倩', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000748a00035', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (749, 924, '2024003061', '李涛', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000749a74bb8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (750, 925, '2024003062', '周萍', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007500aa35a', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (751, 926, '2024003063', '吴杰', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007518e8174', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (752, 927, '2024003064', '郑婷', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007523f8127', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (753, 928, '2024003065', '王峰', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00075351f581', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (754, 929, '2024003066', '冯欣', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000754ede715', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (755, 930, '2024003067', '陈超', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000755072a51', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (756, 931, '2024003068', '褚静', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007563e9aaa', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (757, 932, '2024003069', '卫龙', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000757094e5d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (758, 933, '2024003070', '蒋红', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000758b6a12f', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (759, 934, '2024003071', '沈斌', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00075945d9b5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (760, 935, '2024003072', '韩洁', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000760a83007', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (761, 936, '2024003073', '杨军', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00076153a76d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (762, 937, '2024003074', '朱艳', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000762e0b4e1', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (763, 938, '2024003075', '秦磊', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00076388fa03', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (764, 939, '2024003076', '尤娟', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007647afead', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (765, 940, '2024003077', '许伟', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV0007650abb54', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (766, 941, '2024003078', '何丽', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV00076656a445', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (767, 942, '2024003079', '吕鹏', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, 'INV000767f7d63d', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (768, 943, '2024003080', '施芳', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '20242KN22J', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (769, 944, '2024003081', '张勇', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024YKXAHL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (770, 945, '2024003082', '孔倩', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024KARAZ7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (771, 946, '2024003083', '曹涛', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024MQTKLX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (772, 947, '2024003084', '严萍', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024VRS8CJ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (773, 948, '2024003085', '华杰', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024PYGBE7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (774, 949, '2024003086', '金婷', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024CE7Y2H', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (775, 950, '2024003087', '魏峰', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '202423ZFQ5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (776, 951, '2024003088', '陶欣', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024NQTWF9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (777, 952, '2024003089', '姜超', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024EMKMD9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (778, 953, '2024003090', '戚静', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024MTWWVT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (779, 954, '2024003091', '谢龙', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024PES2JA', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (780, 955, '2024003092', '邹红', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024CJT5DY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (781, 956, '2024003093', '喻斌', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '202474FYFN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (782, 957, '2024003094', '柏洁', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024HQAHMS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (783, 958, '2024003095', '水军', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024AQ6B8L', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (784, 959, '2024003096', '窦艳', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024G3RJNF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (785, 960, '2024003097', '章磊', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024DTEVLY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (786, 961, '2024003098', '云娟', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024MPHX43', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (787, 962, '2024003099', '苏伟', '男', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '2024NQU9SS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (788, 963, '2024003100', '潘丽', '女', '数据科学与大数据技术', '2024', '数据2402', NULL, NULL, NULL, NULL, '20248QRXB4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (789, 964, '2024004005', '葛鹏', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024VFV9KD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (790, 965, '2024004006', '奚芳', '女', '人工智能', '2024', '人工智能2401', NULL, 2, 229, '509', '2024QECTJ2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (791, 966, '2024004007', '范勇', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '20243XL8JU', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (792, 967, '2024004008', '彭倩', '女', '人工智能', '2024', '人工智能2401', NULL, 2, 229, '509', '2024XLYDX8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (793, 968, '2024004009', '郎涛', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024KWKQ4B', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (794, 969, '2024004010', '鲁萍', '女', '人工智能', '2024', '人工智能2401', NULL, 3, 230, '510', '2024CAYBPE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (795, 970, '2024004011', '钱杰', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024PSDRV2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (796, 971, '2024004012', '钟婷', '女', '人工智能', '2024', '人工智能2401', NULL, 3, 230, '510', '20246C9W4G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (797, 972, '2024004013', '盛峰', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '20245BQ8KD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (798, 973, '2024004014', '汤欣', '女', '人工智能', '2024', '人工智能2401', NULL, 8, 231, '601', '2024Q5QK5K', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (799, 974, '2024004015', '陆超', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024D6GLFQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (800, 975, '2024004016', '安静', '女', '人工智能', '2024', '人工智能2401', NULL, 8, 231, '601', '2024BNSXFV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (801, 976, '2024004017', '齐龙', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024QG8MA4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (802, 977, '2024004018', '皮红', '女', '人工智能', '2024', '人工智能2401', NULL, 9, 232, '602', '2024SX4SRN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (803, 978, '2024004019', '卞斌', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024G2HCQF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (804, 979, '2024004020', '舒洁', '女', '人工智能', '2024', '人工智能2401', NULL, 9, 232, '602', '2024TZWAJS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (805, 980, '2024004021', '苗军', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024X3MAPK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (806, 981, '2024004022', '凌艳', '女', '人工智能', '2024', '人工智能2401', NULL, 10, 233, '603', '2024YEQ9AH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (807, 982, '2024004023', '包磊', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024MMEAKF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (808, 983, '2024004024', '柏娟', '女', '人工智能', '2024', '人工智能2401', NULL, 10, 233, '603', '20242ZRG6Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (809, 984, '2024004025', '耿伟', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '20246K9JP2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (810, 985, '2024004026', '关丽', '女', '人工智能', '2024', '人工智能2401', NULL, 11, 234, '604', '20246UWK8U', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (811, 986, '2024004027', '明鹏', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024FJUQQZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (812, 987, '2024004028', '臧芳', '女', '人工智能', '2024', '人工智能2401', NULL, 11, 234, '604', '20244Z8LF5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (813, 988, '2024004029', '伏勇', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024UEPS6X', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (814, 989, '2024004030', '宣倩', '女', '人工智能', '2024', '人工智能2401', NULL, 12, 235, '605', '2024G4JZ5P', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (815, 990, '2024004031', '童涛', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024KCPJFG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (816, 991, '2024004032', '赖萍', '女', '人工智能', '2024', '人工智能2401', NULL, 12, 235, '605', '2024DPCL72', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (817, 992, '2024004033', '祝杰', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024F5MW34', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (818, 993, '2024004034', '阮婷', '女', '人工智能', '2024', '人工智能2401', NULL, 13, 236, '606', '20249XRTRX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (819, 994, '2024004035', '毕峰', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '20244QWSHQ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (820, 995, '2024004036', '龚欣', '女', '人工智能', '2024', '人工智能2401', NULL, 13, 236, '606', '2024474B8M', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (821, 996, '2024004037', '汪超', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024Y7HAM7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (822, 997, '2024004038', '植静', '女', '人工智能', '2024', '人工智能2401', NULL, 14, 237, '607', '20245785LZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (823, 998, '2024004039', '融龙', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024KPNPWD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (824, 999, '2024004040', '郁红', '女', '人工智能', '2024', '人工智能2401', NULL, 14, 237, '607', '2024AYTUX9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (825, 1000, '2024004041', '权斌', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024NKKV8G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (826, 1001, '2024004042', '蒙洁', '女', '人工智能', '2024', '人工智能2401', NULL, 15, 238, '608', '2024B9JZZX', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (827, 1002, '2024004043', '穆军', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024EKY98G', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (828, 1003, '2024004044', '别艳', '女', '人工智能', '2024', '人工智能2401', NULL, 15, 238, '608', '20245G2LT4', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (829, 1004, '2024004045', '游磊', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024YFUK4X', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (830, 1005, '2024004046', '席娟', '女', '人工智能', '2024', '人工智能2401', NULL, 16, 239, '609', '2024EMAEA2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (831, 1006, '2024004047', '隆伟', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024DUKFCY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (832, 1007, '2024004048', '晏丽', '女', '人工智能', '2024', '人工智能2401', NULL, 16, 239, '609', '2024PU2WY9', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (833, 1008, '2024004049', '边鹏', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2024YW5GWY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (834, 1009, '2024004050', '麴芳', '女', '人工智能', '2024', '人工智能2401', NULL, 17, 240, '610', '2024X97JJR', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (835, 1010, '2024004051', '殷勇', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024GPM2UW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (836, 1011, '2024004052', '国倩', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20244N8LR6', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (837, 1012, '2024004053', '欧涛', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024A6FQ2W', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (838, 1013, '2024004054', '文萍', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024RRBQDN', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:21', 0);
INSERT INTO `student` VALUES (839, 1014, '2024004055', '冉杰', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024W77VND', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (840, 1015, '2024004056', '寿婷', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024JJUT2V', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (841, 1016, '2024004057', '解峰', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024RLA4M8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (842, 1017, '2024004058', '简欣', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024NNU5PE', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (843, 1018, '2024004059', '应超', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024AEPEZZ', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (844, 1019, '2024004060', '荆静', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024WDJUMF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (845, 1020, '2024004061', '饶龙', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024ZYH7CW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (846, 1021, '2024004062', '宁红', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024JYBURM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', '爱学习还是爱游戏？？', '[\"爱学习\",\"爱玩游戏\"]', '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (847, 1022, '2024004063', '储斌', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20242DETTL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (848, 1023, '2024004064', '桓洁', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024PHM54R', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (849, 1024, '2024004065', '井军', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024B4HVQ8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (850, 1025, '2024004066', '麻艳', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024LS6WHC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (851, 1026, '2024004067', '卜磊', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '202478M78J', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (852, 1027, '2024004068', '扈娟', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024MFJHC8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (853, 1028, '2024004069', '单伟', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024DG9KXD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (854, 1029, '2024004070', '管丽', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024NY39U3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (855, 1030, '2024004071', '戎鹏', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20244DLCT5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (856, 1031, '2024004072', '郜芳', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024K9W5A5', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (857, 1032, '2024004073', '屈勇', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024CANSJF', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (858, 1033, '2024004074', '幸倩', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024PUK9LH', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (859, 1034, '2024004075', '姬涛', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024ZVM6GS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (860, 1035, '2024004076', '能萍', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024EQKJMC', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (861, 1036, '2024004077', '慕杰', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024UJRVQM', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (862, 1037, '2024004078', '居婷', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024AWR84U', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (863, 1038, '2024004079', '习峰', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024TJYBTT', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (864, 1039, '2024004080', '党欣', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024429LA7', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (865, 1040, '2024004081', '米超', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024Y9UYQK', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (866, 1041, '2024004082', '栾静', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20247KR8JW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (867, 1042, '2024004083', '阳龙', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024NH7YL3', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (868, 1043, '2024004084', '充红', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024VNZ599', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (869, 1044, '2024004085', '封斌', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024PKP99K', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (870, 1045, '2024004086', '左洁', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024S52RD2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (871, 1046, '2024004087', '舒军', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024QP3HHV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (872, 1047, '2024004088', '虞艳', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '202462FW2Y', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (873, 1048, '2024004089', '皮磊', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20248JSJB2', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (874, 1049, '2024004090', '乐娟', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024N7QLMD', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (875, 1050, '2024004091', '卫伟', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '202485DQRV', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (876, 1051, '2024004092', '逄丽', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20242LZNL8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (877, 1052, '2024004093', '滕鹏', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '20248SQZLL', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (878, 1053, '2024004094', '伊芳', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024CB6MXS', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (879, 1054, '2024004095', '单勇', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024TFCR42', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (880, 1055, '2024004096', '晏倩', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024XK3AVW', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (881, 1056, '2024004097', '巩涛', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024ZYT9J8', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (882, 1057, '2024004098', '隋萍', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024JTS58V', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (883, 1058, '2024004099', '訾杰', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024V7WJAG', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (884, 1059, '2024004100', '车婷', '女', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '2024CFBREY', 'used', '2026-12-05 00:41:22', 1, '2025-11-05 00:42:16', NULL, NULL, '23:00', '07:00', '2025-11-05 00:24:22', 0);
INSERT INTO `student` VALUES (889, NULL, '2025002001', '刘七', '男', '软件工程', '2023', '软工2301', NULL, NULL, NULL, NULL, '2025UBSR3V', 'unused', '2026-12-05 00:41:22', 0, NULL, NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (890, 1060, '2025002002', '陈八', '女', '软件工程', '2023', '软工2301', NULL, 5, 181, '101', '2025VFBYQE', 'used', '2026-12-05 00:41:22', 1, '2025-12-07 14:22:43', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (891, 1062, '2025002003', '杨九', '男', '软件工程', '2023', '软工2302', NULL, NULL, NULL, NULL, '2025JAF7AN', 'used', '2026-12-05 00:41:22', 1, '2025-12-15 00:17:29', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (897, 1063, '2025004001', '许十五', '男', '人工智能', '2023', '人工智能2301', NULL, NULL, NULL, NULL, '2025TECA22', 'used', '2026-12-05 00:41:22', 1, '2025-12-15 00:24:29', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (899, NULL, '2025004003', '吕十七', '男', '人工智能', '2023', '人工智能2302', NULL, NULL, NULL, NULL, '20255S6AN7', 'unused', '2026-12-05 00:41:22', 0, NULL, NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (905, NULL, '2025002005', '刘二三', '男', '软件工程', '2024', '软工2401', NULL, NULL, NULL, NULL, '2025DNXDVV', 'unused', '2026-12-05 00:41:22', 0, NULL, NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (906, 1061, '2025002006', '陈二四', '女', '软件工程', '2024', '软工2401', NULL, 4, 194, '204', '2025KWJPLM', 'used', '2026-12-05 00:41:22', 1, '2025-12-14 18:24:50', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (907, NULL, '2025002007', '杨二五', '男', '软件工程', '2024', '软工2402', NULL, NULL, NULL, NULL, '20254AWF7B', 'unused', '2026-12-05 00:41:22', 0, NULL, NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (908, 1067, '2025002008', '周二六', '女', '软件工程', '2024', '软工2402', NULL, 30, 218, '408', '2025TS8KSZ', 'used', '2026-12-05 00:41:22', 1, '2025-12-28 01:03:33', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (913, 1066, '2025004005', '许三一', '男', '人工智能', '2024', '人工智能2401', NULL, NULL, NULL, NULL, '2025UVHPNY', 'used', '2026-12-05 00:41:22', 1, '2025-12-17 15:16:53', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);
INSERT INTO `student` VALUES (915, 1065, '2025004007', '吕三三', '男', '人工智能', '2024', '人工智能2402', NULL, NULL, NULL, NULL, '202534ZDNX', 'used', '2026-12-05 00:41:22', 1, '2025-12-17 15:11:47', NULL, NULL, '23:00', '07:00', '2025-11-07 19:11:38', 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（MD5加密）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `user_type` enum('student','admin','superadmin') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户类型',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用，超级管理员管理页面',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像URL',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_user_type`(`user_type`) USING BTREE,
  INDEX `idx_email`(`email`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1069 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'superadmin', '25d55ad283aa400af464c76d713c07ad', '超级管理员', 'superadmin@campus.edu.cn', '13800138112', 'superadmin', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (271, 'zhou', '25d55ad283aa400af464c76d713c07ad', '周管理员', 'zhou@admin.cn', '17267838376', 'admin', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (273, 'wang', '25d55ad283aa400af464c76d713c07ad', '王管理员', 'wang@admin.cn', '13211111111', 'admin', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (274, 'liu', '25d55ad283aa400af464c76d713c07ad', '刘管理员', 'liu@admin.cn', '13212121212', 'admin', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (275, 'liadmin', '25d55ad283aa400af464c76d713c07ad', '李管理员', 'li@dormitory.edu.cn', '13800000001', 'admin', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (276, '2023001001', '25d55ad283aa400af464c76d713c07ad', '文伟', '2023001001@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (277, '2023001002', '25d55ad283aa400af464c76d713c07ad', '毛芳', '2023001002@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (278, '2023001003', '25d55ad283aa400af464c76d713c07ad', '甘鹏', '2023001003@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (279, '2023001004', '25d55ad283aa400af464c76d713c07ad', '牛敏', '2023001004@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (280, '2023001005', '25d55ad283aa400af464c76d713c07ad', '冀峰', '2023001005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (281, '2023001006', '25d55ad283aa400af464c76d713c07ad', '茹欣', '2023001006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (282, '2023001007', '25d55ad283aa400af464c76d713c07ad', '衡超', '2023001007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (283, '2023001008', '25d55ad283aa400af464c76d713c07ad', '纪静', '2023001008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (284, '2023001009', '25d55ad283aa400af464c76d713c07ad', '谈龙', '2023001009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (285, '2023001010', '25d55ad283aa400af464c76d713c07ad', '双红', '2023001010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (286, '2023001011', '25d55ad283aa400af464c76d713c07ad', '邴斌', '2023001011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (287, '2023001012', '25d55ad283aa400af464c76d713c07ad', '水洁', '2023001012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (288, '2023001013', '25d55ad283aa400af464c76d713c07ad', '卞军', '2023001013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (289, '2023001014', '25d55ad283aa400af464c76d713c07ad', '翁艳', '2023001014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (290, '2023001015', '25d55ad283aa400af464c76d713c07ad', '那磊', '2023001015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (291, '2023001016', '25d55ad283aa400af464c76d713c07ad', '游娟', '2023001016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (292, '2023001017', '25d55ad283aa400af464c76d713c07ad', '江伟', '2023001017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (293, '2023001018', '25d55ad283aa400af464c76d713c07ad', '芮丽', '2023001018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (294, '2023001019', '25d55ad283aa400af464c76d713c07ad', '鲍鹏', '2023001019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (295, '2023001020', '25d55ad283aa400af464c76d713c07ad', '阙芳', '2023001020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (296, '2023001021', '25d55ad283aa400af464c76d713c07ad', '成勇', '2023001021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (297, '2023001022', '25d55ad283aa400af464c76d713c07ad', '武倩', '2023001022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (298, '2023001023', '25d55ad283aa400af464c76d713c07ad', '常涛', '2023001023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (299, '2023001024', '25d55ad283aa400af464c76d713c07ad', '戚萍', '2023001024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (300, '2023001025', '25d55ad283aa400af464c76d713c07ad', '乐杰', '2023001025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (301, '2023001026', '25d55ad283aa400af464c76d713c07ad', '时婷', '2023001026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (302, '2023001027', '25d55ad283aa400af464c76d713c07ad', '吴峰', '2023001027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (303, '2023001028', '25d55ad283aa400af464c76d713c07ad', '于欣', '2023001028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (304, '2023001029', '25d55ad283aa400af464c76d713c07ad', '元超', '2023001029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (305, '2023001030', '25d55ad283aa400af464c76d713c07ad', '臧静', '2023001030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (306, '2023001031', '25d55ad283aa400af464c76d713c07ad', '桂龙', '2023001031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (307, '2023001032', '25d55ad283aa400af464c76d713c07ad', '滕红', '2023001032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (308, '2023001033', '25d55ad283aa400af464c76d713c07ad', '殷斌', '2023001033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (309, '2023001034', '25d55ad283aa400af464c76d713c07ad', '荣洁', '2023001034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (310, '2023001035', '25d55ad283aa400af464c76d713c07ad', '涂军', '2023001035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (311, '2023001036', '25d55ad283aa400af464c76d713c07ad', '甘艳', '2023001036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (312, '2023001037', '25d55ad283aa400af464c76d713c07ad', '宗磊', '2023001037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (313, '2023001038', '25d55ad283aa400af464c76d713c07ad', '闻娟', '2023001038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (314, '2023001039', '25d55ad283aa400af464c76d713c07ad', '堵伟', '2023001039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (315, '2023001040', '25d55ad283aa400af464c76d713c07ad', '连丽', '2023001040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (316, '2023001041', '25d55ad283aa400af464c76d713c07ad', '植鹏', '2023001041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (317, '2023001042', '25d55ad283aa400af464c76d713c07ad', '乔芳', '2023001042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (318, '2023001043', '25d55ad283aa400af464c76d713c07ad', '屈勇', '2023001043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (319, '2023001044', '25d55ad283aa400af464c76d713c07ad', '慕倩', '2023001044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (320, '2023001045', '25d55ad283aa400af464c76d713c07ad', '封涛', '2023001045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (321, '2023001046', '25d55ad283aa400af464c76d713c07ad', '习萍', '2023001046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (322, '2023001047', '25d55ad283aa400af464c76d713c07ad', '左杰', '2023001047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (323, '2023001048', '25d55ad283aa400af464c76d713c07ad', '米婷', '2023001048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (324, '2023001049', '25d55ad283aa400af464c76d713c07ad', '舒峰', '2023001049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (325, '2023001050', '25d55ad283aa400af464c76d713c07ad', '欧欣', '2023001050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (326, '2023001051', '25d55ad283aa400af464c76d713c07ad', '郁超', '2023001051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (327, '2023001052', '25d55ad283aa400af464c76d713c07ad', '葛静', '2023001052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (328, '2023001053', '25d55ad283aa400af464c76d713c07ad', '卓龙', '2023001053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (329, '2023001054', '25d55ad283aa400af464c76d713c07ad', '宗红', '2023001054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (330, '2023001055', '25d55ad283aa400af464c76d713c07ad', '井斌', '2023001055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (331, '2023001056', '25d55ad283aa400af464c76d713c07ad', '秦洁', '2023001056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (332, '2023001057', '25d55ad283aa400af464c76d713c07ad', '栾军', '2023001057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (333, '2023001058', '25d55ad283aa400af464c76d713c07ad', '姬艳', '2023001058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (334, '2023001059', '25d55ad283aa400af464c76d713c07ad', '应磊', '2023001059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (335, '2023001060', '25d55ad283aa400af464c76d713c07ad', '穆娟', '2023001060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (336, '2023001061', '25d55ad283aa400af464c76d713c07ad', '居伟', '2023001061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (337, '2023001062', '25d55ad283aa400af464c76d713c07ad', '华丽', '2023001062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (338, '2023001063', '25d55ad283aa400af464c76d713c07ad', '幸鹏', '2023001063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (339, '2023001064', '25d55ad283aa400af464c76d713c07ad', '左芳', '2023001064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (340, '2023001065', '25d55ad283aa400af464c76d713c07ad', '包勇', '2023001065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (341, '2023001066', '25d55ad283aa400af464c76d713c07ad', '伊倩', '2023001066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (342, '2023001067', '25d55ad283aa400af464c76d713c07ad', '充涛', '2023001067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (343, '2023001068', '25d55ad283aa400af464c76d713c07ad', '乐萍', '2023001068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (344, '2023001069', '25d55ad283aa400af464c76d713c07ad', '皮杰', '2023001069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (345, '2023001070', '25d55ad283aa400af464c76d713c07ad', '卫婷', '2023001070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (346, '2023001071', '25d55ad283aa400af464c76d713c07ad', '逄峰', '2023001071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (347, '2023001072', '25d55ad283aa400af464c76d713c07ad', '滕欣', '2023001072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (348, '2023001073', '25d55ad283aa400af464c76d713c07ad', '单超', '2023001073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (349, '2023001074', '25d55ad283aa400af464c76d713c07ad', '晏静', '2023001074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (350, '2023001075', '25d55ad283aa400af464c76d713c07ad', '巩龙', '2023001075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (351, '2023001076', '25d55ad283aa400af464c76d713c07ad', '隋红', '2023001076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (352, '2023001077', '25d55ad283aa400af464c76d713c07ad', '訾斌', '2023001077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (353, '2023001078', '25d55ad283aa400af464c76d713c07ad', '车洁', '2023001078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (354, '2023001079', '25d55ad283aa400af464c76d713c07ad', '羊军', '2023001079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (355, '2023001080', '25d55ad283aa400af464c76d713c07ad', '慕艳', '2023001080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (356, '2023001081', '25d55ad283aa400af464c76d713c07ad', '宫磊', '2023001081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (357, '2023001082', '25d55ad283aa400af464c76d713c07ad', '国娟', '2023001082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (358, '2023001083', '25d55ad283aa400af464c76d713c07ad', '步伟', '2023001083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (359, '2023001084', '25d55ad283aa400af464c76d713c07ad', '丁丽', '2023001084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (360, '2023001085', '25d55ad283aa400af464c76d713c07ad', '元鹏', '2023001085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (361, '2023001086', '25d55ad283aa400af464c76d713c07ad', '井芳', '2023001086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (362, '2023001087', '25d55ad283aa400af464c76d713c07ad', '怀勇', '2023001087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (363, '2023001088', '25d55ad283aa400af464c76d713c07ad', '米倩', '2023001088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (364, '2023001089', '25d55ad283aa400af464c76d713c07ad', '党涛', '2023001089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (365, '2023001090', '25d55ad283aa400af464c76d713c07ad', '习萍', '2023001090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (366, '2023001091', '25d55ad283aa400af464c76d713c07ad', '阮杰', '2023001091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (367, '2023001092', '25d55ad283aa400af464c76d713c07ad', '栾婷', '2023001092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (368, '2023001093', '25d55ad283aa400af464c76d713c07ad', '胥峰', '2023001093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (369, '2023001094', '25d55ad283aa400af464c76d713c07ad', '能欣', '2023001094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (370, '2023001095', '25d55ad283aa400af464c76d713c07ad', '索超', '2023001095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (371, '2023001096', '25d55ad283aa400af464c76d713c07ad', '冉静', '2023001096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (372, '2023001097', '25d55ad283aa400af464c76d713c07ad', '咸龙', '2023001097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (373, '2023001098', '25d55ad283aa400af464c76d713c07ad', '寇红', '2023001098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (374, '2023001099', '25d55ad283aa400af464c76d713c07ad', '赖斌', '2023001099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (375, '2023001100', '25d55ad283aa400af464c76d713c07ad', '昌洁', '2023001100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (376, '2023002001', '25d55ad283aa400af464c76d713c07ad', '祝杰', '2023002001@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (377, '2023002002', '25d55ad283aa400af464c76d713c07ad', '封婷', '2023002002@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (378, '2023002003', '25d55ad283aa400af464c76d713c07ad', '米峰', '2023002003@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (379, '2023002004', '25d55ad283aa400af464c76d713c07ad', '舒倩', '2023002004@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (380, '2023002005', '25d55ad283aa400af464c76d713c07ad', '郁超', '2023002005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (381, '2023002006', '25d55ad283aa400af464c76d713c07ad', '别静', '2023002006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (382, '2023002007', '25d55ad283aa400af464c76d713c07ad', '厍龙', '2023002007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (383, '2023002008', '25d55ad283aa400af464c76d713c07ad', '蒯红', '2023002008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (384, '2023002009', '25d55ad283aa400af464c76d713c07ad', '吕斌', '2023002009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (385, '2023002010', '25d55ad283aa400af464c76d713c07ad', '令洁', '2023002010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (386, '2023002011', '25d55ad283aa400af464c76d713c07ad', '欧军', '2023002011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (387, '2023002012', '25d55ad283aa400af464c76d713c07ad', '和艳', '2023002012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (388, '2023002013', '25d55ad283aa400af464c76d713c07ad', '劳磊', '2023002013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (389, '2023002014', '25d55ad283aa400af464c76d713c07ad', '须娟', '2023002014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (390, '2023002015', '25d55ad283aa400af464c76d713c07ad', '冷伟', '2023002015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (391, '2023002016', '25d55ad283aa400af464c76d713c07ad', '屈丽', '2023002016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (392, '2023002017', '25d55ad283aa400af464c76d713c07ad', '丛鹏', '2023002017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (393, '2023002018', '25d55ad283aa400af464c76d713c07ad', '宗芳', '2023002018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (394, '2023002019', '25d55ad283aa400af464c76d713c07ad', '曾勇', '2023002019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (395, '2023002020', '25d55ad283aa400af464c76d713c07ad', '班倩', '2023002020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (396, '2023002021', '25d55ad283aa400af464c76d713c07ad', '原涛', '2023002021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (397, '2023002022', '25d55ad283aa400af464c76d713c07ad', '应萍', '2023002022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (398, '2023002023', '25d55ad283aa400af464c76d713c07ad', '盛杰', '2023002023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (399, '2023002024', '25d55ad283aa400af464c76d713c07ad', '幸婷', '2023002024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (400, '2023002025', '25d55ad283aa400af464c76d713c07ad', '危峰', '2023002025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (401, '2023002026', '25d55ad283aa400af464c76d713c07ad', '左欣', '2023002026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (402, '2023002027', '25d55ad283aa400af464c76d713c07ad', '富超', '2023002027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (403, '2023002028', '25d55ad283aa400af464c76d713c07ad', '初静', '2023002028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (404, '2023002029', '25d55ad283aa400af464c76d713c07ad', '栗龙', '2023002029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (405, '2023002030', '25d55ad283aa400af464c76d713c07ad', '钮红', '2023002030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (406, '2023002031', '25d55ad283aa400af464c76d713c07ad', '饶斌', '2023002031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (407, '2023002032', '25d55ad283aa400af464c76d713c07ad', '訾洁', '2023002032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (408, '2023002033', '25d55ad283aa400af464c76d713c07ad', '厉军', '2023002033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (409, '2023002034', '25d55ad283aa400af464c76d713c07ad', '利艳', '2023002034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (410, '2023002035', '25d55ad283aa400af464c76d713c07ad', '於磊', '2023002035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (411, '2023002036', '25d55ad283aa400af464c76d713c07ad', '哈娟', '2023002036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (412, '2023002037', '25d55ad283aa400af464c76d713c07ad', '房伟', '2023002037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (413, '2023002038', '25d55ad283aa400af464c76d713c07ad', '年丽', '2023002038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (414, '2023002039', '25d55ad283aa400af464c76d713c07ad', '钦鹏', '2023002039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (415, '2023002040', '25d55ad283aa400af464c76d713c07ad', '乔芳', '2023002040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (416, '2023002041', '25d55ad283aa400af464c76d713c07ad', '贝勇', '2023002041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (417, '2023002042', '25d55ad283aa400af464c76d713c07ad', '皮倩', '2023002042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (418, '2023002043', '25d55ad283aa400af464c76d713c07ad', '东涛', '2023002043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (419, '2023002044', '25d55ad283aa400af464c76d713c07ad', '卫萍', '2023002044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (420, '2023002045', '25d55ad283aa400af464c76d713c07ad', '司杰', '2023002045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (421, '2023002046', '25d55ad283aa400af464c76d713c07ad', '桑婷', '2023002046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (422, '2023002047', '25d55ad283aa400af464c76d713c07ad', '寿峰', '2023002047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (423, '2023002048', '25d55ad283aa400af464c76d713c07ad', '通欣', '2023002048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (424, '2023002049', '25d55ad283aa400af464c76d713c07ad', '归超', '2023002049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (425, '2023002050', '25d55ad283aa400af464c76d713c07ad', '干静', '2023002050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (426, '2023002051', '25d55ad283aa400af464c76d713c07ad', '况龙', '2023002051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (427, '2023002052', '25d55ad283aa400af464c76d713c07ad', '靳红', '2023002052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (428, '2023002053', '25d55ad283aa400af464c76d713c07ad', '广斌', '2023002053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (429, '2023002054', '25d55ad283aa400af464c76d713c07ad', '蔚洁', '2023002054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (430, '2023002055', '25d55ad283aa400af464c76d713c07ad', '蓬军', '2023002055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (432, '2023002057', '25d55ad283aa400af464c76d713c07ad', '郦磊', '2023002057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (434, '2023002059', '25d55ad283aa400af464c76d713c07ad', '养伟', '2023002059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (436, '2023002061', '25d55ad283aa400af464c76d713c07ad', '刁鹏', '2023002061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (438, '2023002063', '25d55ad283aa400af464c76d713c07ad', '修勇', '2023002063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (440, '2023002065', '25d55ad283aa400af464c76d713c07ad', '充涛', '2023002065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (442, '2023002067', '25d55ad283aa400af464c76d713c07ad', '爱杰', '2023002067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (443, '2023002068', '25d55ad283aa400af464c76d713c07ad', '苏婷', '2023002068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (444, '2023002069', '25d55ad283aa400af464c76d713c07ad', '向峰', '2023002069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (445, '2023002070', '25d55ad283aa400af464c76d713c07ad', '后欣', '2023002070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (446, '2023002071', '25d55ad283aa400af464c76d713c07ad', '红超', '2023002071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (447, '2023002072', '25d55ad283aa400af464c76d713c07ad', '益静', '2023002072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (448, '2023002073', '25d55ad283aa400af464c76d713c07ad', '皇龙', '2023002073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (449, '2023002074', '25d55ad283aa400af464c76d713c07ad', '甫红', '2023002074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (450, '2023002075', '25d55ad283aa400af464c76d713c07ad', '百斌', '2023002075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (451, '2023002076', '25d55ad283aa400af464c76d713c07ad', '里洁', '2023002076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (452, '2023002077', '25d55ad283aa400af464c76d713c07ad', '衣军', '2023002077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (453, '2023002078', '25d55ad283aa400af464c76d713c07ad', '闻艳', '2023002078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (454, '2023002079', '25d55ad283aa400af464c76d713c07ad', '堵磊', '2023002079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (455, '2023002080', '25d55ad283aa400af464c76d713c07ad', '连娟', '2023002080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (456, '2023002081', '25d55ad283aa400af464c76d713c07ad', '刘伟', '2023002081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (457, '2023002082', '25d55ad283aa400af464c76d713c07ad', '赵丽', '2023002082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (458, '2023002083', '25d55ad283aa400af464c76d713c07ad', '钱鹏', '2023002083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (459, '2023002084', '25d55ad283aa400af464c76d713c07ad', '孙芳', '2023002084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (460, '2023002085', '25d55ad283aa400af464c76d713c07ad', '李勇', '2023002085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (461, '2023002086', '25d55ad283aa400af464c76d713c07ad', '周倩', '2023002086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (462, '2023002087', '25d55ad283aa400af464c76d713c07ad', '吴涛', '2023002087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (463, '2023002088', '25d55ad283aa400af464c76d713c07ad', '郑萍', '2023002088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (464, '2023002089', '25d55ad283aa400af464c76d713c07ad', '王杰', '2023002089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (465, '2023002090', '25d55ad283aa400af464c76d713c07ad', '冯婷', '2023002090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (466, '2023002091', '25d55ad283aa400af464c76d713c07ad', '陈峰', '2023002091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (467, '2023002092', '25d55ad283aa400af464c76d713c07ad', '褚欣', '2023002092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (468, '2023002093', '25d55ad283aa400af464c76d713c07ad', '卫超', '2023002093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (469, '2023002094', '25d55ad283aa400af464c76d713c07ad', '蒋静', '2023002094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (470, '2023002095', '25d55ad283aa400af464c76d713c07ad', '沈龙', '2023002095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (471, '2023002096', '25d55ad283aa400af464c76d713c07ad', '韩红', '2023002096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (472, '2023002097', '25d55ad283aa400af464c76d713c07ad', '杨斌', '2023002097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (473, '2023002098', '25d55ad283aa400af464c76d713c07ad', '朱洁', '2023002098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (474, '2023002099', '25d55ad283aa400af464c76d713c07ad', '秦军', '2023002099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (475, '2023002100', '25d55ad283aa400af464c76d713c07ad', '尤艳', '2023002100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (476, '2023003001', '25d55ad283aa400af464c76d713c07ad', '欧勇', '2023003001@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (477, '2023003002', '25d55ad283aa400af464c76d713c07ad', '郁欣', '2023003002@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (478, '2023003003', '25d55ad283aa400af464c76d713c07ad', '葛超', '2023003003@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (479, '2023003004', '25d55ad283aa400af464c76d713c07ad', '卓静', '2023003004@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (480, '2023003005', '25d55ad283aa400af464c76d713c07ad', '许磊', '2023003005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (481, '2023003006', '25d55ad283aa400af464c76d713c07ad', '何娟', '2023003006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (482, '2023003007', '25d55ad283aa400af464c76d713c07ad', '吕伟', '2023003007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (483, '2023003008', '25d55ad283aa400af464c76d713c07ad', '施丽', '2023003008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (484, '2023003009', '25d55ad283aa400af464c76d713c07ad', '张鹏', '2023003009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (485, '2023003010', '25d55ad283aa400af464c76d713c07ad', '孔芳', '2023003010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (486, '2023003011', '25d55ad283aa400af464c76d713c07ad', '曹勇', '2023003011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (487, '2023003012', '25d55ad283aa400af464c76d713c07ad', '严倩', '2023003012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (488, '2023003013', '25d55ad283aa400af464c76d713c07ad', '华涛', '2023003013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (489, '2023003014', '25d55ad283aa400af464c76d713c07ad', '金萍', '2023003014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (490, '2023003015', '25d55ad283aa400af464c76d713c07ad', '魏杰', '2023003015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (491, '2023003016', '25d55ad283aa400af464c76d713c07ad', '陶婷', '2023003016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (492, '2023003017', '25d55ad283aa400af464c76d713c07ad', '姜峰', '2023003017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (493, '2023003018', '25d55ad283aa400af464c76d713c07ad', '戚欣', '2023003018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (494, '2023003019', '25d55ad283aa400af464c76d713c07ad', '谢超', '2023003019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (495, '2023003020', '25d55ad283aa400af464c76d713c07ad', '邹静', '2023003020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (496, '2023003021', '25d55ad283aa400af464c76d713c07ad', '喻龙', '2023003021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (497, '2023003022', '25d55ad283aa400af464c76d713c07ad', '柏红', '2023003022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (498, '2023003023', '25d55ad283aa400af464c76d713c07ad', '水斌', '2023003023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (499, '2023003024', '25d55ad283aa400af464c76d713c07ad', '窦洁', '2023003024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (500, '2023003025', '25d55ad283aa400af464c76d713c07ad', '章军', '2023003025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (501, '2023003026', '25d55ad283aa400af464c76d713c07ad', '云艳', '2023003026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (502, '2023003027', '25d55ad283aa400af464c76d713c07ad', '苏磊', '2023003027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (503, '2023003028', '25d55ad283aa400af464c76d713c07ad', '潘娟', '2023003028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (504, '2023003029', '25d55ad283aa400af464c76d713c07ad', '葛伟', '2023003029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (505, '2023003030', '25d55ad283aa400af464c76d713c07ad', '奚丽', '2023003030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (506, '2023003031', '25d55ad283aa400af464c76d713c07ad', '范鹏', '2023003031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (507, '2023003032', '25d55ad283aa400af464c76d713c07ad', '彭芳', '2023003032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (508, '2023003033', '25d55ad283aa400af464c76d713c07ad', '郎勇', '2023003033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (509, '2023003034', '25d55ad283aa400af464c76d713c07ad', '鲁倩', '2023003034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (510, '2023003035', '25d55ad283aa400af464c76d713c07ad', '钱涛', '2023003035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (511, '2023003036', '25d55ad283aa400af464c76d713c07ad', '钟萍', '2023003036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (512, '2023003037', '25d55ad283aa400af464c76d713c07ad', '盛杰', '2023003037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (513, '2023003038', '25d55ad283aa400af464c76d713c07ad', '汤婷', '2023003038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (514, '2023003039', '25d55ad283aa400af464c76d713c07ad', '陆峰', '2023003039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (515, '2023003040', '25d55ad283aa400af464c76d713c07ad', '安欣', '2023003040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (516, '2023003041', '25d55ad283aa400af464c76d713c07ad', '齐超', '2023003041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (517, '2023003042', '25d55ad283aa400af464c76d713c07ad', '皮静', '2023003042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (518, '2023003043', '25d55ad283aa400af464c76d713c07ad', '卞龙', '2023003043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (519, '2023003044', '25d55ad283aa400af464c76d713c07ad', '舒红', '2023003044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (520, '2023003045', '25d55ad283aa400af464c76d713c07ad', '苗斌', '2023003045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (521, '2023003046', '25d55ad283aa400af464c76d713c07ad', '凌洁', '2023003046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (522, '2023003047', '25d55ad283aa400af464c76d713c07ad', '包军', '2023003047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (523, '2023003048', '25d55ad283aa400af464c76d713c07ad', '柏艳', '2023003048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (524, '2023003049', '25d55ad283aa400af464c76d713c07ad', '耿磊', '2023003049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (525, '2023003050', '25d55ad283aa400af464c76d713c07ad', '关娟', '2023003050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (526, '2023003051', '25d55ad283aa400af464c76d713c07ad', '明伟', '2023003051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (527, '2023003052', '25d55ad283aa400af464c76d713c07ad', '臧丽', '2023003052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (528, '2023003053', '25d55ad283aa400af464c76d713c07ad', '伏鹏', '2023003053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (529, '2023003054', '25d55ad283aa400af464c76d713c07ad', '宣芳', '2023003054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (530, '2023003055', '25d55ad283aa400af464c76d713c07ad', '童勇', '2023003055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (531, '2023003056', '25d55ad283aa400af464c76d713c07ad', '赖倩', '2023003056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (532, '2023003057', '25d55ad283aa400af464c76d713c07ad', '祝涛', '2023003057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (533, '2023003058', '25d55ad283aa400af464c76d713c07ad', '阮萍', '2023003058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (534, '2023003059', '25d55ad283aa400af464c76d713c07ad', '毕杰', '2023003059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (535, '2023003060', '25d55ad283aa400af464c76d713c07ad', '龚婷', '2023003060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (536, '2023003061', '25d55ad283aa400af464c76d713c07ad', '汪峰', '2023003061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (537, '2023003062', '25d55ad283aa400af464c76d713c07ad', '植欣', '2023003062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (538, '2023003063', '25d55ad283aa400af464c76d713c07ad', '融超', '2023003063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (539, '2023003064', '25d55ad283aa400af464c76d713c07ad', '郁静', '2023003064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (540, '2023003065', '25d55ad283aa400af464c76d713c07ad', '权龙', '2023003065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (541, '2023003066', '25d55ad283aa400af464c76d713c07ad', '蒙红', '2023003066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (542, '2023003067', '25d55ad283aa400af464c76d713c07ad', '穆斌', '2023003067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (543, '2023003068', '25d55ad283aa400af464c76d713c07ad', '别洁', '2023003068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (544, '2023003069', '25d55ad283aa400af464c76d713c07ad', '游军', '2023003069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (545, '2023003070', '25d55ad283aa400af464c76d713c07ad', '席艳', '2023003070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (546, '2023003071', '25d55ad283aa400af464c76d713c07ad', '隆磊', '2023003071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (547, '2023003072', '25d55ad283aa400af464c76d713c07ad', '晏娟', '2023003072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (548, '2023003073', '25d55ad283aa400af464c76d713c07ad', '边伟', '2023003073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (549, '2023003074', '25d55ad283aa400af464c76d713c07ad', '麴丽', '2023003074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (550, '2023003075', '25d55ad283aa400af464c76d713c07ad', '殷鹏', '2023003075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (551, '2023003076', '25d55ad283aa400af464c76d713c07ad', '国芳', '2023003076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (552, '2023003077', '25d55ad283aa400af464c76d713c07ad', '欧勇', '2023003077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (553, '2023003078', '25d55ad283aa400af464c76d713c07ad', '文倩', '2023003078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (554, '2023003079', '25d55ad283aa400af464c76d713c07ad', '冉涛', '2023003079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (555, '2023003080', '25d55ad283aa400af464c76d713c07ad', '寿萍', '2023003080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (556, '2023003081', '25d55ad283aa400af464c76d713c07ad', '解杰', '2023003081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (557, '2023003082', '25d55ad283aa400af464c76d713c07ad', '简婷', '2023003082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (558, '2023003083', '25d55ad283aa400af464c76d713c07ad', '应峰', '2023003083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (559, '2023003084', '25d55ad283aa400af464c76d713c07ad', '荆欣', '2023003084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (560, '2023003085', '25d55ad283aa400af464c76d713c07ad', '饶超', '2023003085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (561, '2023003086', '25d55ad283aa400af464c76d713c07ad', '宁静', '2023003086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (562, '2023003087', '25d55ad283aa400af464c76d713c07ad', '储龙', '2023003087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (563, '2023003088', '25d55ad283aa400af464c76d713c07ad', '桓红', '2023003088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (564, '2023003089', '25d55ad283aa400af464c76d713c07ad', '井斌', '2023003089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (565, '2023003090', '25d55ad283aa400af464c76d713c07ad', '麻洁', '2023003090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (566, '2023003091', '25d55ad283aa400af464c76d713c07ad', '卜军', '2023003091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (567, '2023003092', '25d55ad283aa400af464c76d713c07ad', '扈艳', '2023003092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (568, '2023003093', '25d55ad283aa400af464c76d713c07ad', '单磊', '2023003093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (569, '2023003094', '25d55ad283aa400af464c76d713c07ad', '管娟', '2023003094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (570, '2023003095', '25d55ad283aa400af464c76d713c07ad', '戎伟', '2023003095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (571, '2023003096', '25d55ad283aa400af464c76d713c07ad', '郜丽', '2023003096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (572, '2023003097', '25d55ad283aa400af464c76d713c07ad', '屈鹏', '2023003097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (573, '2023003098', '25d55ad283aa400af464c76d713c07ad', '幸芳', '2023003098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (574, '2023003099', '25d55ad283aa400af464c76d713c07ad', '姬勇', '2023003099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (575, '2023003100', '25d55ad283aa400af464c76d713c07ad', '能倩', '2023003100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (576, '2023004001', '25d55ad283aa400af464c76d713c07ad', '宗龙', '2023004001@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (577, '2023004002', '25d55ad283aa400af464c76d713c07ad', '井红', '2023004002@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (578, '2023004003', '25d55ad283aa400af464c76d713c07ad', '秦斌', '2023004003@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (579, '2023004004', '25d55ad283aa400af464c76d713c07ad', '栾洁', '2023004004@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (580, '2023004005', '25d55ad283aa400af464c76d713c07ad', '慕涛', '2023004005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (581, '2023004006', '25d55ad283aa400af464c76d713c07ad', '居萍', '2023004006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (582, '2023004007', '25d55ad283aa400af464c76d713c07ad', '习杰', '2023004007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (583, '2023004008', '25d55ad283aa400af464c76d713c07ad', '党婷', '2023004008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (584, '2023004009', '25d55ad283aa400af464c76d713c07ad', '米峰', '2023004009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (585, '2023004010', '25d55ad283aa400af464c76d713c07ad', '栾欣', '2023004010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (586, '2023004011', '25d55ad283aa400af464c76d713c07ad', '阳超', '2023004011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (587, '2023004012', '25d55ad283aa400af464c76d713c07ad', '充静', '2023004012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (588, '2023004013', '25d55ad283aa400af464c76d713c07ad', '封龙', '2023004013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (589, '2023004014', '25d55ad283aa400af464c76d713c07ad', '左红', '2023004014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (590, '2023004015', '25d55ad283aa400af464c76d713c07ad', '舒斌', '2023004015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (591, '2023004016', '25d55ad283aa400af464c76d713c07ad', '虞洁', '2023004016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (592, '2023004017', '25d55ad283aa400af464c76d713c07ad', '皮军', '2023004017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (593, '2023004018', '25d55ad283aa400af464c76d713c07ad', '乐艳', '2023004018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (594, '2023004019', '25d55ad283aa400af464c76d713c07ad', '卫磊', '2023004019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (595, '2023004020', '25d55ad283aa400af464c76d713c07ad', '逄娟', '2023004020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (596, '2023004021', '25d55ad283aa400af464c76d713c07ad', '滕伟', '2023004021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (597, '2023004022', '25d55ad283aa400af464c76d713c07ad', '伊丽', '2023004022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (598, '2023004023', '25d55ad283aa400af464c76d713c07ad', '单鹏', '2023004023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (599, '2023004024', '25d55ad283aa400af464c76d713c07ad', '晏芳', '2023004024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (600, '2023004025', '25d55ad283aa400af464c76d713c07ad', '巩勇', '2023004025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (601, '2023004026', '25d55ad283aa400af464c76d713c07ad', '隋倩', '2023004026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (602, '2023004027', '25d55ad283aa400af464c76d713c07ad', '訾涛', '2023004027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (603, '2023004028', '25d55ad283aa400af464c76d713c07ad', '车萍', '2023004028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (604, '2023004029', '25d55ad283aa400af464c76d713c07ad', '羊杰', '2023004029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (605, '2023004030', '25d55ad283aa400af464c76d713c07ad', '慕婷', '2023004030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (606, '2023004031', '25d55ad283aa400af464c76d713c07ad', '宫峰', '2023004031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (607, '2023004032', '25d55ad283aa400af464c76d713c07ad', '国欣', '2023004032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (608, '2023004033', '25d55ad283aa400af464c76d713c07ad', '步超', '2023004033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (609, '2023004034', '25d55ad283aa400af464c76d713c07ad', '丁静', '2023004034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (610, '2023004035', '25d55ad283aa400af464c76d713c07ad', '元龙', '2023004035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (611, '2023004036', '25d55ad283aa400af464c76d713c07ad', '井红', '2023004036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (612, '2023004037', '25d55ad283aa400af464c76d713c07ad', '怀斌', '2023004037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (613, '2023004038', '25d55ad283aa400af464c76d713c07ad', '米洁', '2023004038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (614, '2023004039', '25d55ad283aa400af464c76d713c07ad', '党军', '2023004039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (615, '2023004040', '25d55ad283aa400af464c76d713c07ad', '习艳', '2023004040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (616, '2023004041', '25d55ad283aa400af464c76d713c07ad', '阮磊', '2023004041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (617, '2023004042', '25d55ad283aa400af464c76d713c07ad', '栾娟', '2023004042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (618, '2023004043', '25d55ad283aa400af464c76d713c07ad', '胥伟', '2023004043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (619, '2023004044', '25d55ad283aa400af464c76d713c07ad', '能丽', '2023004044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (620, '2023004045', '25d55ad283aa400af464c76d713c07ad', '索鹏', '2023004045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (621, '2023004046', '25d55ad283aa400af464c76d713c07ad', '冉芳', '2023004046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (622, '2023004047', '25d55ad283aa400af464c76d713c07ad', '咸勇', '2023004047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (623, '2023004048', '25d55ad283aa400af464c76d713c07ad', '寇倩', '2023004048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (624, '2023004049', '25d55ad283aa400af464c76d713c07ad', '赖涛', '2023004049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (625, '2023004050', '25d55ad283aa400af464c76d713c07ad', '昌萍', '2023004050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (626, '2023004051', '25d55ad283aa400af464c76d713c07ad', '郁杰', '2023004051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (627, '2023004052', '25d55ad283aa400af464c76d713c07ad', '别婷', '2023004052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (628, '2023004053', '25d55ad283aa400af464c76d713c07ad', '厍峰', '2023004053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (629, '2023004054', '25d55ad283aa400af464c76d713c07ad', '蒯欣', '2023004054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (630, '2023004055', '25d55ad283aa400af464c76d713c07ad', '吕超', '2023004055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (631, '2023004056', '25d55ad283aa400af464c76d713c07ad', '令静', '2023004056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (632, '2023004057', '25d55ad283aa400af464c76d713c07ad', '欧龙', '2023004057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (633, '2023004058', '25d55ad283aa400af464c76d713c07ad', '和红', '2023004058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (634, '2023004059', '25d55ad283aa400af464c76d713c07ad', '劳斌', '2023004059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (635, '2023004060', '25d55ad283aa400af464c76d713c07ad', '须洁', '2023004060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (636, '2023004061', '25d55ad283aa400af464c76d713c07ad', '冷军', '2023004061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (637, '2023004062', '25d55ad283aa400af464c76d713c07ad', '屈艳', '2023004062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (638, '2023004063', '25d55ad283aa400af464c76d713c07ad', '丛磊', '2023004063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (639, '2023004064', '25d55ad283aa400af464c76d713c07ad', '宗娟', '2023004064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (640, '2023004065', '25d55ad283aa400af464c76d713c07ad', '曾伟', '2023004065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (641, '2023004066', '25d55ad283aa400af464c76d713c07ad', '班丽', '2023004066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (642, '2023004067', '25d55ad283aa400af464c76d713c07ad', '原鹏', '2023004067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (643, '2023004068', '25d55ad283aa400af464c76d713c07ad', '应芳', '2023004068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (644, '2023004069', '25d55ad283aa400af464c76d713c07ad', '盛勇', '2023004069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (645, '2023004070', '25d55ad283aa400af464c76d713c07ad', '幸倩', '2023004070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (646, '2023004071', '25d55ad283aa400af464c76d713c07ad', '危涛', '2023004071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (647, '2023004072', '25d55ad283aa400af464c76d713c07ad', '左萍', '2023004072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (648, '2023004073', '25d55ad283aa400af464c76d713c07ad', '富杰', '2023004073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (649, '2023004074', '25d55ad283aa400af464c76d713c07ad', '初婷', '2023004074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (650, '2023004075', '25d55ad283aa400af464c76d713c07ad', '栗峰', '2023004075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (651, '2023004076', '25d55ad283aa400af464c76d713c07ad', '钮欣', '2023004076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (652, '2023004077', '25d55ad283aa400af464c76d713c07ad', '饶超', '2023004077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (653, '2023004078', '25d55ad283aa400af464c76d713c07ad', '訾静', '2023004078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (654, '2023004079', '25d55ad283aa400af464c76d713c07ad', '厉龙', '2023004079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (655, '2023004080', '25d55ad283aa400af464c76d713c07ad', '利红', '2023004080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (656, '2023004081', '25d55ad283aa400af464c76d713c07ad', '於斌', '2023004081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (657, '2023004082', '25d55ad283aa400af464c76d713c07ad', '哈洁', '2023004082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (658, '2023004083', '25d55ad283aa400af464c76d713c07ad', '房军', '2023004083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (659, '2023004084', '25d55ad283aa400af464c76d713c07ad', '年艳', '2023004084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (660, '2023004085', '25d55ad283aa400af464c76d713c07ad', '钦磊', '2023004085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (661, '2023004086', '25d55ad283aa400af464c76d713c07ad', '乔娟', '2023004086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (662, '2023004087', '25d55ad283aa400af464c76d713c07ad', '贝伟', '2023004087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (663, '2023004088', '25d55ad283aa400af464c76d713c07ad', '皮丽', '2023004088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (664, '2023004089', '25d55ad283aa400af464c76d713c07ad', '东鹏', '2023004089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (665, '2023004090', '25d55ad283aa400af464c76d713c07ad', '卫芳', '2023004090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (666, '2023004091', '25d55ad283aa400af464c76d713c07ad', '司勇', '2023004091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (667, '2023004092', '25d55ad283aa400af464c76d713c07ad', '桑倩', '2023004092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (668, '2023004093', '25d55ad283aa400af464c76d713c07ad', '寿涛', '2023004093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (669, '2023004094', '25d55ad283aa400af464c76d713c07ad', '通萍', '2023004094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (670, '2023004095', '25d55ad283aa400af464c76d713c07ad', '归杰', '2023004095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (671, '2023004096', '25d55ad283aa400af464c76d713c07ad', '干婷', '2023004096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (672, '2023004097', '25d55ad283aa400af464c76d713c07ad', '况峰', '2023004097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (673, '2023004098', '25d55ad283aa400af464c76d713c07ad', '靳欣', '2023004098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (674, '2023004099', '25d55ad283aa400af464c76d713c07ad', '广超', '2023004099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (675, '2023004100', '25d55ad283aa400af464c76d713c07ad', '蔚静', '2023004100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (676, '2024001005', '25d55ad283aa400af464c76d713c07ad', '林海', '2024001005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (677, '2024001006', '25d55ad283aa400af464c76d713c07ad', '董雪', '2024001006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (678, '2024001007', '25d55ad283aa400af464c76d713c07ad', '郝强', '2024001007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (679, '2024001008', '25d55ad283aa400af464c76d713c07ad', '汤丽', '2024001008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (680, '2024001009', '25d55ad283aa400af464c76d713c07ad', '邹鹏', '2024001009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (681, '2024001010', '25d55ad283aa400af464c76d713c07ad', '熊敏', '2024001010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (682, '2024001011', '25d55ad283aa400af464c76d713c07ad', '武涛', '2024001011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (683, '2024001012', '25d55ad283aa400af464c76d713c07ad', '康娜', '2024001012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (684, '2024001013', '25d55ad283aa400af464c76d713c07ad', '于杰', '2024001013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (685, '2024001014', '25d55ad283aa400af464c76d713c07ad', '苏婷', '2024001014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (686, '2024001015', '25d55ad283aa400af464c76d713c07ad', '范峰', '2024001015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (687, '2024001016', '25d55ad283aa400af464c76d713c07ad', '白欣', '2024001016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (688, '2024001017', '25d55ad283aa400af464c76d713c07ad', '孟超', '2024001017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (689, '2024001018', '25d55ad283aa400af464c76d713c07ad', '常静', '2024001018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (690, '2024001019', '25d55ad283aa400af464c76d713c07ad', '江龙', '2024001019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (691, '2024001020', '25d55ad283aa400af464c76d713c07ad', '施红', '2024001020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (692, '2024001021', '25d55ad283aa400af464c76d713c07ad', '柳斌', '2024001021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (693, '2024001022', '25d55ad283aa400af464c76d713c07ad', '查洁', '2024001022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (694, '2024001023', '25d55ad283aa400af464c76d713c07ad', '俞军', '2024001023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (695, '2024001024', '25d55ad283aa400af464c76d713c07ad', '姜艳', '2024001024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (696, '2024001025', '25d55ad283aa400af464c76d713c07ad', '凌磊', '2024001025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (697, '2024001026', '25d55ad283aa400af464c76d713c07ad', '齐娟', '2024001026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (698, '2024001027', '25d55ad283aa400af464c76d713c07ad', '薛伟', '2024001027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (699, '2024001028', '25d55ad283aa400af464c76d713c07ad', '费丽', '2024001028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (700, '2024001029', '25d55ad283aa400af464c76d713c07ad', '庄鹏', '2024001029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (701, '2024001030', '25d55ad283aa400af464c76d713c07ad', '成芳', '2024001030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (702, '2024001031', '25d55ad283aa400af464c76d713c07ad', '顾勇', '2024001031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (703, '2024001032', '25d55ad283aa400af464c76d713c07ad', '卜倩', '2024001032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (704, '2024001033', '25d55ad283aa400af464c76d713c07ad', '倪涛', '2024001033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (705, '2024001034', '25d55ad283aa400af464c76d713c07ad', '荣萍', '2024001034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (706, '2024001035', '25d55ad283aa400af464c76d713c07ad', '季杰', '2024001035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (707, '2024001036', '25d55ad283aa400af464c76d713c07ad', '简婷', '2024001036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (708, '2024001037', '25d55ad283aa400af464c76d713c07ad', '阎峰', '2024001037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (709, '2024001038', '25d55ad283aa400af464c76d713c07ad', '蓝欣', '2024001038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (710, '2024001039', '25d55ad283aa400af464c76d713c07ad', '易超', '2024001039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (711, '2024001040', '25d55ad283aa400af464c76d713c07ad', '温静', '2024001040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (712, '2024001041', '25d55ad283aa400af464c76d713c07ad', '游龙', '2024001041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (713, '2024001042', '25d55ad283aa400af464c76d713c07ad', '农红', '2024001042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (714, '2024001043', '25d55ad283aa400af464c76d713c07ad', '卞斌', '2024001043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (715, '2024001044', '25d55ad283aa400af464c76d713c07ad', '尚洁', '2024001044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (716, '2024001045', '25d55ad283aa400af464c76d713c07ad', '席军', '2024001045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (717, '2024001046', '25d55ad283aa400af464c76d713c07ad', '练艳', '2024001046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (718, '2024001047', '25d55ad283aa400af464c76d713c07ad', '晋磊', '2024001047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (719, '2024001048', '25d55ad283aa400af464c76d713c07ad', '冼娟', '2024001048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (720, '2024001049', '25d55ad283aa400af464c76d713c07ad', '管伟', '2024001049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (721, '2024001050', '25d55ad283aa400af464c76d713c07ad', '裴丽', '2024001050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (722, '2024001051', '25d55ad283aa400af464c76d713c07ad', '安鹏', '2024001051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (723, '2024001052', '25d55ad283aa400af464c76d713c07ad', '柯芳', '2024001052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (724, '2024001053', '25d55ad283aa400af464c76d713c07ad', '芦勇', '2024001053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (725, '2024001054', '25d55ad283aa400af464c76d713c07ad', '花倩', '2024001054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (726, '2024001055', '25d55ad283aa400af464c76d713c07ad', '朱涛', '2024001055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (727, '2024001056', '25d55ad283aa400af464c76d713c07ad', '甄萍', '2024001056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (728, '2024001057', '25d55ad283aa400af464c76d713c07ad', '娄杰', '2024001057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (729, '2024001058', '25d55ad283aa400af464c76d713c07ad', '曲婷', '2024001058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (730, '2024001059', '25d55ad283aa400af464c76d713c07ad', '苗峰', '2024001059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (731, '2024001060', '25d55ad283aa400af464c76d713c07ad', '符欣', '2024001060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (732, '2024001061', '25d55ad283aa400af464c76d713c07ad', '解超', '2024001061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (733, '2024001062', '25d55ad283aa400af464c76d713c07ad', '储静', '2024001062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (734, '2024001063', '25d55ad283aa400af464c76d713c07ad', '路龙', '2024001063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (735, '2024001064', '25d55ad283aa400af464c76d713c07ad', '权红', '2024001064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (736, '2024001065', '25d55ad283aa400af464c76d713c07ad', '糜斌', '2024001065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (737, '2024001066', '25d55ad283aa400af464c76d713c07ad', '仇洁', '2024001066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (738, '2024001067', '25d55ad283aa400af464c76d713c07ad', '景军', '2024001067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (739, '2024001068', '25d55ad283aa400af464c76d713c07ad', '党艳', '2024001068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (740, '2024001069', '25d55ad283aa400af464c76d713c07ad', '裘磊', '2024001069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (741, '2024001070', '25d55ad283aa400af464c76d713c07ad', '巫娟', '2024001070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (742, '2024001071', '25d55ad283aa400af464c76d713c07ad', '扈伟', '2024001071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (743, '2024001072', '25d55ad283aa400af464c76d713c07ad', '岑丽', '2024001072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (744, '2024001073', '25d55ad283aa400af464c76d713c07ad', '楼鹏', '2024001073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (745, '2024001074', '25d55ad283aa400af464c76d713c07ad', '闵芳', '2024001074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (746, '2024001075', '25d55ad283aa400af464c76d713c07ad', '殷勇', '2024001075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (747, '2024001076', '25d55ad283aa400af464c76d713c07ad', '戚倩', '2024001076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (748, '2024001077', '25d55ad283aa400af464c76d713c07ad', '聂涛', '2024001077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (749, '2024001078', '25d55ad283aa400af464c76d713c07ad', '喻萍', '2024001078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (750, '2024001079', '25d55ad283aa400af464c76d713c07ad', '师杰', '2024001079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (751, '2024001080', '25d55ad283aa400af464c76d713c07ad', '翁婷', '2024001080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (752, '2024001081', '25d55ad283aa400af464c76d713c07ad', '洪峰', '2024001081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (753, '2024001082', '25d55ad283aa400af464c76d713c07ad', '颜欣', '2024001082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (754, '2024001083', '25d55ad283aa400af464c76d713c07ad', '宁超', '2024001083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (755, '2024001084', '25d55ad283aa400af464c76d713c07ad', '祁静', '2024001084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (756, '2024001085', '25d55ad283aa400af464c76d713c07ad', '柴龙', '2024001085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (757, '2024001086', '25d55ad283aa400af464c76d713c07ad', '燕红', '2024001086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (758, '2024001087', '25d55ad283aa400af464c76d713c07ad', '古斌', '2024001087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (759, '2024001088', '25d55ad283aa400af464c76d713c07ad', '籍洁', '2024001088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (760, '2024001089', '25d55ad283aa400af464c76d713c07ad', '禹军', '2024001089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (761, '2024001090', '25d55ad283aa400af464c76d713c07ad', '边艳', '2024001090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (762, '2024001091', '25d55ad283aa400af464c76d713c07ad', '戴磊', '2024001091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (763, '2024001092', '25d55ad283aa400af464c76d713c07ad', '申娟', '2024001092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (764, '2024001093', '25d55ad283aa400af464c76d713c07ad', '漆伟', '2024001093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (765, '2024001094', '25d55ad283aa400af464c76d713c07ad', '莫丽', '2024001094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (766, '2024001095', '25d55ad283aa400af464c76d713c07ad', '蒲鹏', '2024001095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (767, '2024001096', '25d55ad283aa400af464c76d713c07ad', '殳芳', '2024001096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (768, '2024001097', '25d55ad283aa400af464c76d713c07ad', '郭勇', '2024001097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (769, '2024001098', '25d55ad283aa400af464c76d713c07ad', '贝倩', '2024001098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (770, '2024001099', '25d55ad283aa400af464c76d713c07ad', '尤涛', '2024001099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (771, '2024001100', '25d55ad283aa400af464c76d713c07ad', '仲萍', '2024001100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (772, '2024002005', '25d55ad283aa400af464c76d713c07ad', '柏杰', '2024002005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (773, '2024002006', '25d55ad283aa400af464c76d713c07ad', '伍婷', '2024002006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (774, '2024002007', '25d55ad283aa400af464c76d713c07ad', '焦峰', '2024002007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (775, '2024002008', '25d55ad283aa400af464c76d713c07ad', '桂欣', '2024002008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (776, '2024002009', '25d55ad283aa400af464c76d713c07ad', '衡超', '2024002009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (777, '2024002010', '25d55ad283aa400af464c76d713c07ad', '鲁静', '2024002010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (778, '2024002011', '25d55ad283aa400af464c76d713c07ad', '全龙', '2024002011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (779, '2024002012', '25d55ad283aa400af464c76d713c07ad', '阳红', '2024002012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (780, '2024002013', '25d55ad283aa400af464c76d713c07ad', '鞠斌', '2024002013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (781, '2024002014', '25d55ad283aa400af464c76d713c07ad', '艾洁', '2024002014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (782, '2024002015', '25d55ad283aa400af464c76d713c07ad', '荀军', '2024002015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (783, '2024002016', '25d55ad283aa400af464c76d713c07ad', '虞艳', '2024002016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (784, '2024002017', '25d55ad283aa400af464c76d713c07ad', '郎磊', '2024002017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (785, '2024002018', '25d55ad283aa400af464c76d713c07ad', '佟娟', '2024002018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (786, '2024002019', '25d55ad283aa400af464c76d713c07ad', '束伟', '2024002019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (787, '2024002020', '25d55ad283aa400af464c76d713c07ad', '容丽', '2024002020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (788, '2024002021', '25d55ad283aa400af464c76d713c07ad', '涂鹏', '2024002021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (789, '2024002022', '25d55ad283aa400af464c76d713c07ad', '井芳', '2024002022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (790, '2024002023', '25d55ad283aa400af464c76d713c07ad', '时勇', '2024002023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (791, '2024002024', '25d55ad283aa400af464c76d713c07ad', '项倩', '2024002024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (792, '2024002025', '25d55ad283aa400af464c76d713c07ad', '庞涛', '2024002025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (793, '2024002026', '25d55ad283aa400af464c76d713c07ad', '慎萍', '2024002026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (794, '2024002027', '25d55ad283aa400af464c76d713c07ad', '雍杰', '2024002027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (795, '2024002028', '25d55ad283aa400af464c76d713c07ad', '晁婷', '2024002028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (796, '2024002029', '25d55ad283aa400af464c76d713c07ad', '顾峰', '2024002029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (797, '2024002030', '25d55ad283aa400af464c76d713c07ad', '暨欣', '2024002030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (798, '2024002031', '25d55ad283aa400af464c76d713c07ad', '辛超', '2024002031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (799, '2024002032', '25d55ad283aa400af464c76d713c07ad', '松静', '2024002032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (800, '2024002033', '25d55ad283aa400af464c76d713c07ad', '厉龙', '2024002033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (801, '2024002034', '25d55ad283aa400af464c76d713c07ad', '印红', '2024002034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (802, '2024002035', '25d55ad283aa400af464c76d713c07ad', '宿斌', '2024002035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (803, '2024002036', '25d55ad283aa400af464c76d713c07ad', '郜洁', '2024002036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (804, '2024002037', '25d55ad283aa400af464c76d713c07ad', '隆军', '2024002037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (805, '2024002038', '25d55ad283aa400af464c76d713c07ad', '东艳', '2024002038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (806, '2024002039', '25d55ad283aa400af464c76d713c07ad', '车磊', '2024002039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (807, '2024002040', '25d55ad283aa400af464c76d713c07ad', '平娟', '2024002040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (808, '2024002041', '25d55ad283aa400af464c76d713c07ad', '巩伟', '2024002041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (809, '2024002042', '25d55ad283aa400af464c76d713c07ad', '奚丽', '2024002042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (810, '2024002043', '25d55ad283aa400af464c76d713c07ad', '邢鹏', '2024002043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (811, '2024002044', '25d55ad283aa400af464c76d713c07ad', '商芳', '2024002044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (812, '2024002045', '25d55ad283aa400af464c76d713c07ad', '戈勇', '2024002045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (813, '2024002046', '25d55ad283aa400af464c76d713c07ad', '滑倩', '2024002046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (814, '2024002047', '25d55ad283aa400af464c76d713c07ad', '毕涛', '2024002047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (815, '2024002048', '25d55ad283aa400af464c76d713c07ad', '翟萍', '2024002048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (816, '2024002049', '25d55ad283aa400af464c76d713c07ad', '连杰', '2024002049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (817, '2024002050', '25d55ad283aa400af464c76d713c07ad', '强婷', '2024002050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (818, '2024002051', '25d55ad283aa400af464c76d713c07ad', '匡峰', '2024002051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (819, '2024002052', '25d55ad283aa400af464c76d713c07ad', '苟欣', '2024002052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (820, '2024002053', '25d55ad283aa400af464c76d713c07ad', '相超', '2024002053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (821, '2024002054', '25d55ad283aa400af464c76d713c07ad', '麻静', '2024002054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (822, '2024002055', '25d55ad283aa400af464c76d713c07ad', '宫龙', '2024002055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (823, '2024002056', '25d55ad283aa400af464c76d713c07ad', '国红', '2024002056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (824, '2024002057', '25d55ad283aa400af464c76d713c07ad', '步斌', '2024002057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (825, '2024002058', '25d55ad283aa400af464c76d713c07ad', '丁洁', '2024002058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (826, '2024002059', '25d55ad283aa400af464c76d713c07ad', '元军', '2024002059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (827, '2024002060', '25d55ad283aa400af464c76d713c07ad', '井艳', '2024002060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (828, '2024002061', '25d55ad283aa400af464c76d713c07ad', '怀磊', '2024002061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (829, '2024002062', '25d55ad283aa400af464c76d713c07ad', '米娟', '2024002062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (830, '2024002063', '25d55ad283aa400af464c76d713c07ad', '党伟', '2024002063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (831, '2024002064', '25d55ad283aa400af464c76d713c07ad', '习丽', '2024002064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (832, '2024002065', '25d55ad283aa400af464c76d713c07ad', '阮鹏', '2024002065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (833, '2024002066', '25d55ad283aa400af464c76d713c07ad', '栾芳', '2024002066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (834, '2024002067', '25d55ad283aa400af464c76d713c07ad', '胥勇', '2024002067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (835, '2024002068', '25d55ad283aa400af464c76d713c07ad', '能倩', '2024002068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (836, '2024002069', '25d55ad283aa400af464c76d713c07ad', '索涛', '2024002069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (837, '2024002070', '25d55ad283aa400af464c76d713c07ad', '冉萍', '2024002070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (838, '2024002071', '25d55ad283aa400af464c76d713c07ad', '咸杰', '2024002071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (839, '2024002072', '25d55ad283aa400af464c76d713c07ad', '寇婷', '2024002072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (840, '2024002073', '25d55ad283aa400af464c76d713c07ad', '赖峰', '2024002073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (841, '2024002074', '25d55ad283aa400af464c76d713c07ad', '昌欣', '2024002074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (842, '2024002075', '25d55ad283aa400af464c76d713c07ad', '郁超', '2024002075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (843, '2024002076', '25d55ad283aa400af464c76d713c07ad', '别静', '2024002076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (844, '2024002077', '25d55ad283aa400af464c76d713c07ad', '厍龙', '2024002077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (845, '2024002078', '25d55ad283aa400af464c76d713c07ad', '蒯红', '2024002078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (846, '2024002079', '25d55ad283aa400af464c76d713c07ad', '吕斌', '2024002079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (847, '2024002080', '25d55ad283aa400af464c76d713c07ad', '令洁', '2024002080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (848, '2024002081', '25d55ad283aa400af464c76d713c07ad', '欧军', '2024002081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (849, '2024002082', '25d55ad283aa400af464c76d713c07ad', '和艳', '2024002082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (850, '2024002083', '25d55ad283aa400af464c76d713c07ad', '劳磊', '2024002083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (851, '2024002084', '25d55ad283aa400af464c76d713c07ad', '须娟', '2024002084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (852, '2024002085', '25d55ad283aa400af464c76d713c07ad', '冷伟', '2024002085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (853, '2024002086', '25d55ad283aa400af464c76d713c07ad', '屈丽', '2024002086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (854, '2024002087', '25d55ad283aa400af464c76d713c07ad', '丛鹏', '2024002087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (855, '2024002088', '25d55ad283aa400af464c76d713c07ad', '宗芳', '2024002088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (856, '2024002089', '25d55ad283aa400af464c76d713c07ad', '曾勇', '2024002089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (857, '2024002090', '25d55ad283aa400af464c76d713c07ad', '班倩', '2024002090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (858, '2024002091', '25d55ad283aa400af464c76d713c07ad', '原涛', '2024002091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (859, '2024002092', '25d55ad283aa400af464c76d713c07ad', '应萍', '2024002092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (860, '2024002093', '25d55ad283aa400af464c76d713c07ad', '盛杰', '2024002093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (861, '2024002094', '25d55ad283aa400af464c76d713c07ad', '幸婷', '2024002094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (862, '2024002095', '25d55ad283aa400af464c76d713c07ad', '危峰', '2024002095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (863, '2024002096', '25d55ad283aa400af464c76d713c07ad', '左欣', '2024002096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (864, '2024002097', '25d55ad283aa400af464c76d713c07ad', '富超', '2024002097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (865, '2024002098', '25d55ad283aa400af464c76d713c07ad', '初静', '2024002098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (866, '2024002099', '25d55ad283aa400af464c76d713c07ad', '栗龙', '2024002099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (867, '2024002100', '25d55ad283aa400af464c76d713c07ad', '钮红', '2024002100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (868, '2024003005', '25d55ad283aa400af464c76d713c07ad', '饶斌', '2024003005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (869, '2024003006', '25d55ad283aa400af464c76d713c07ad', '訾洁', '2024003006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (870, '2024003007', '25d55ad283aa400af464c76d713c07ad', '厉军', '2024003007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (871, '2024003008', '25d55ad283aa400af464c76d713c07ad', '利艳', '2024003008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (872, '2024003009', '25d55ad283aa400af464c76d713c07ad', '於磊', '2024003009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (873, '2024003010', '25d55ad283aa400af464c76d713c07ad', '哈娟', '2024003010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (874, '2024003011', '25d55ad283aa400af464c76d713c07ad', '房伟', '2024003011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (875, '2024003012', '25d55ad283aa400af464c76d713c07ad', '年丽', '2024003012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (876, '2024003013', '25d55ad283aa400af464c76d713c07ad', '钦鹏', '2024003013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (877, '2024003014', '25d55ad283aa400af464c76d713c07ad', '乔芳', '2024003014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (878, '2024003015', '25d55ad283aa400af464c76d713c07ad', '贝勇', '2024003015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (879, '2024003016', '25d55ad283aa400af464c76d713c07ad', '皮倩', '2024003016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (880, '2024003017', '25d55ad283aa400af464c76d713c07ad', '东涛', '2024003017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (881, '2024003018', '25d55ad283aa400af464c76d713c07ad', '卫萍', '2024003018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (882, '2024003019', '25d55ad283aa400af464c76d713c07ad', '司杰', '2024003019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (883, '2024003020', '25d55ad283aa400af464c76d713c07ad', '桑婷', '2024003020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (884, '2024003021', '25d55ad283aa400af464c76d713c07ad', '寿峰', '2024003021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (885, '2024003022', '25d55ad283aa400af464c76d713c07ad', '通欣', '2024003022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (886, '2024003023', '25d55ad283aa400af464c76d713c07ad', '归超', '2024003023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (887, '2024003024', '25d55ad283aa400af464c76d713c07ad', '干静', '2024003024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (888, '2024003025', '25d55ad283aa400af464c76d713c07ad', '况龙', '2024003025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (889, '2024003026', '25d55ad283aa400af464c76d713c07ad', '靳红', '2024003026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (890, '2024003027', '25d55ad283aa400af464c76d713c07ad', '广斌', '2024003027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (891, '2024003028', '25d55ad283aa400af464c76d713c07ad', '蔚洁', '2024003028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (892, '2024003029', '25d55ad283aa400af464c76d713c07ad', '蓬军', '2024003029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (893, '2024003030', '25d55ad283aa400af464c76d713c07ad', '隗艳', '2024003030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (894, '2024003031', '25d55ad283aa400af464c76d713c07ad', '郦磊', '2024003031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (895, '2024003032', '25d55ad283aa400af464c76d713c07ad', '怡娟', '2024003032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (896, '2024003033', '25d55ad283aa400af464c76d713c07ad', '养伟', '2024003033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (897, '2024003034', '25d55ad283aa400af464c76d713c07ad', '红丽', '2024003034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (898, '2024003035', '25d55ad283aa400af464c76d713c07ad', '刁鹏', '2024003035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (899, '2024003036', '25d55ad283aa400af464c76d713c07ad', '鱼芳', '2024003036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (900, '2024003037', '25d55ad283aa400af464c76d713c07ad', '修勇', '2024003037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (901, '2024003038', '25d55ad283aa400af464c76d713c07ad', '揭倩', '2024003038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (902, '2024003039', '25d55ad283aa400af464c76d713c07ad', '充涛', '2024003039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (903, '2024003040', '25d55ad283aa400af464c76d713c07ad', '茅萍', '2024003040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (904, '2024003041', '25d55ad283aa400af464c76d713c07ad', '爱杰', '2024003041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (905, '2024003042', '25d55ad283aa400af464c76d713c07ad', '苏婷', '2024003042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (906, '2024003043', '25d55ad283aa400af464c76d713c07ad', '向峰', '2024003043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (907, '2024003044', '25d55ad283aa400af464c76d713c07ad', '后欣', '2024003044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (908, '2024003045', '25d55ad283aa400af464c76d713c07ad', '红超', '2024003045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (909, '2024003046', '25d55ad283aa400af464c76d713c07ad', '益静', '2024003046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (910, '2024003047', '25d55ad283aa400af464c76d713c07ad', '皇龙', '2024003047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (911, '2024003048', '25d55ad283aa400af464c76d713c07ad', '甫红', '2024003048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (912, '2024003049', '25d55ad283aa400af464c76d713c07ad', '百斌', '2024003049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (913, '2024003050', '25d55ad283aa400af464c76d713c07ad', '里洁', '2024003050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (914, '2024003051', '25d55ad283aa400af464c76d713c07ad', '衣军', '2024003051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (915, '2024003052', '25d55ad283aa400af464c76d713c07ad', '闻艳', '2024003052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (916, '2024003053', '25d55ad283aa400af464c76d713c07ad', '堵磊', '2024003053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (917, '2024003054', '25d55ad283aa400af464c76d713c07ad', '连娟', '2024003054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (918, '2024003055', '25d55ad283aa400af464c76d713c07ad', '澹台伟', '2024003055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (919, '2024003056', '25d55ad283aa400af464c76d713c07ad', '第五丽', '2024003056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (920, '2024003057', '25d55ad283aa400af464c76d713c07ad', '刘鹏', '2024003057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (921, '2024003058', '25d55ad283aa400af464c76d713c07ad', '赵芳', '2024003058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (922, '2024003059', '25d55ad283aa400af464c76d713c07ad', '钱勇', '2024003059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (923, '2024003060', '25d55ad283aa400af464c76d713c07ad', '孙倩', '2024003060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (924, '2024003061', '25d55ad283aa400af464c76d713c07ad', '李涛', '2024003061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (925, '2024003062', '25d55ad283aa400af464c76d713c07ad', '周萍', '2024003062@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (926, '2024003063', '25d55ad283aa400af464c76d713c07ad', '吴杰', '2024003063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (927, '2024003064', '25d55ad283aa400af464c76d713c07ad', '郑婷', '2024003064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (928, '2024003065', '25d55ad283aa400af464c76d713c07ad', '王峰', '2024003065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (929, '2024003066', '25d55ad283aa400af464c76d713c07ad', '冯欣', '2024003066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (930, '2024003067', '25d55ad283aa400af464c76d713c07ad', '陈超', '2024003067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (931, '2024003068', '25d55ad283aa400af464c76d713c07ad', '褚静', '2024003068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (932, '2024003069', '25d55ad283aa400af464c76d713c07ad', '卫龙', '2024003069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (933, '2024003070', '25d55ad283aa400af464c76d713c07ad', '蒋红', '2024003070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (934, '2024003071', '25d55ad283aa400af464c76d713c07ad', '沈斌', '2024003071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (935, '2024003072', '25d55ad283aa400af464c76d713c07ad', '韩洁', '2024003072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (936, '2024003073', '25d55ad283aa400af464c76d713c07ad', '杨军', '2024003073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (937, '2024003074', '25d55ad283aa400af464c76d713c07ad', '朱艳', '2024003074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (938, '2024003075', '25d55ad283aa400af464c76d713c07ad', '秦磊', '2024003075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (939, '2024003076', '25d55ad283aa400af464c76d713c07ad', '尤娟', '2024003076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (940, '2024003077', '25d55ad283aa400af464c76d713c07ad', '许伟', '2024003077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (941, '2024003078', '25d55ad283aa400af464c76d713c07ad', '何丽', '2024003078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (942, '2024003079', '25d55ad283aa400af464c76d713c07ad', '吕鹏', '2024003079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (943, '2024003080', '25d55ad283aa400af464c76d713c07ad', '施芳', '2024003080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (944, '2024003081', '25d55ad283aa400af464c76d713c07ad', '张勇', '2024003081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (945, '2024003082', '25d55ad283aa400af464c76d713c07ad', '孔倩', '2024003082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (946, '2024003083', '25d55ad283aa400af464c76d713c07ad', '曹涛', '2024003083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (947, '2024003084', '25d55ad283aa400af464c76d713c07ad', '严萍', '2024003084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (948, '2024003085', '25d55ad283aa400af464c76d713c07ad', '华杰', '2024003085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (949, '2024003086', '25d55ad283aa400af464c76d713c07ad', '金婷', '2024003086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (950, '2024003087', '25d55ad283aa400af464c76d713c07ad', '魏峰', '2024003087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (951, '2024003088', '25d55ad283aa400af464c76d713c07ad', '陶欣', '2024003088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (952, '2024003089', '25d55ad283aa400af464c76d713c07ad', '姜超', '2024003089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (953, '2024003090', '25d55ad283aa400af464c76d713c07ad', '戚静', '2024003090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (954, '2024003091', '25d55ad283aa400af464c76d713c07ad', '谢龙', '2024003091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (955, '2024003092', '25d55ad283aa400af464c76d713c07ad', '邹红', '2024003092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (956, '2024003093', '25d55ad283aa400af464c76d713c07ad', '喻斌', '2024003093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (957, '2024003094', '25d55ad283aa400af464c76d713c07ad', '柏洁', '2024003094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (958, '2024003095', '25d55ad283aa400af464c76d713c07ad', '水军', '2024003095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (959, '2024003096', '25d55ad283aa400af464c76d713c07ad', '窦艳', '2024003096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (960, '2024003097', '25d55ad283aa400af464c76d713c07ad', '章磊', '2024003097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (961, '2024003098', '25d55ad283aa400af464c76d713c07ad', '云娟', '2024003098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (962, '2024003099', '25d55ad283aa400af464c76d713c07ad', '苏伟', '2024003099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (963, '2024003100', '25d55ad283aa400af464c76d713c07ad', '潘丽', '2024003100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (964, '2024004005', '25d55ad283aa400af464c76d713c07ad', '葛鹏', '2024004005@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (965, '2024004006', '25d55ad283aa400af464c76d713c07ad', '奚芳', '2024004006@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (966, '2024004007', '25d55ad283aa400af464c76d713c07ad', '范勇', '2024004007@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (967, '2024004008', '25d55ad283aa400af464c76d713c07ad', '彭倩', '2024004008@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (968, '2024004009', '25d55ad283aa400af464c76d713c07ad', '郎涛', '2024004009@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (969, '2024004010', '25d55ad283aa400af464c76d713c07ad', '鲁萍', '2024004010@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (970, '2024004011', '25d55ad283aa400af464c76d713c07ad', '钱杰', '2024004011@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (971, '2024004012', '25d55ad283aa400af464c76d713c07ad', '钟婷', '2024004012@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (972, '2024004013', '25d55ad283aa400af464c76d713c07ad', '盛峰', '2024004013@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (973, '2024004014', '25d55ad283aa400af464c76d713c07ad', '汤欣', '2024004014@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (974, '2024004015', '25d55ad283aa400af464c76d713c07ad', '陆超', '2024004015@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (975, '2024004016', '25d55ad283aa400af464c76d713c07ad', '安静', '2024004016@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (976, '2024004017', '25d55ad283aa400af464c76d713c07ad', '齐龙', '2024004017@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (977, '2024004018', '25d55ad283aa400af464c76d713c07ad', '皮红', '2024004018@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (978, '2024004019', '25d55ad283aa400af464c76d713c07ad', '卞斌', '2024004019@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (979, '2024004020', '25d55ad283aa400af464c76d713c07ad', '舒洁', '2024004020@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (980, '2024004021', '25d55ad283aa400af464c76d713c07ad', '苗军', '2024004021@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (981, '2024004022', '25d55ad283aa400af464c76d713c07ad', '凌艳', '2024004022@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (982, '2024004023', '25d55ad283aa400af464c76d713c07ad', '包磊', '2024004023@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (983, '2024004024', '25d55ad283aa400af464c76d713c07ad', '柏娟', '2024004024@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (984, '2024004025', '25d55ad283aa400af464c76d713c07ad', '耿伟', '2024004025@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (985, '2024004026', '25d55ad283aa400af464c76d713c07ad', '关丽', '2024004026@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (986, '2024004027', '25d55ad283aa400af464c76d713c07ad', '明鹏', '2024004027@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (987, '2024004028', '25d55ad283aa400af464c76d713c07ad', '臧芳', '2024004028@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (988, '2024004029', '25d55ad283aa400af464c76d713c07ad', '伏勇', '2024004029@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (989, '2024004030', '25d55ad283aa400af464c76d713c07ad', '宣倩', '2024004030@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (990, '2024004031', '25d55ad283aa400af464c76d713c07ad', '童涛', '2024004031@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (991, '2024004032', '25d55ad283aa400af464c76d713c07ad', '赖萍', '2024004032@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (992, '2024004033', '25d55ad283aa400af464c76d713c07ad', '祝杰', '2024004033@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (993, '2024004034', '25d55ad283aa400af464c76d713c07ad', '阮婷', '2024004034@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (994, '2024004035', '25d55ad283aa400af464c76d713c07ad', '毕峰', '2024004035@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (995, '2024004036', '25d55ad283aa400af464c76d713c07ad', '龚欣', '2024004036@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (996, '2024004037', '25d55ad283aa400af464c76d713c07ad', '汪超', '2024004037@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (997, '2024004038', '25d55ad283aa400af464c76d713c07ad', '植静', '2024004038@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (998, '2024004039', '25d55ad283aa400af464c76d713c07ad', '融龙', '2024004039@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (999, '2024004040', '25d55ad283aa400af464c76d713c07ad', '郁红', '2024004040@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1000, '2024004041', '25d55ad283aa400af464c76d713c07ad', '权斌', '2024004041@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1001, '2024004042', '25d55ad283aa400af464c76d713c07ad', '蒙洁', '2024004042@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1002, '2024004043', '25d55ad283aa400af464c76d713c07ad', '穆军', '2024004043@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1003, '2024004044', '25d55ad283aa400af464c76d713c07ad', '别艳', '2024004044@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1004, '2024004045', '25d55ad283aa400af464c76d713c07ad', '游磊', '2024004045@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1005, '2024004046', '25d55ad283aa400af464c76d713c07ad', '席娟', '2024004046@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1006, '2024004047', '25d55ad283aa400af464c76d713c07ad', '隆伟', '2024004047@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1007, '2024004048', '25d55ad283aa400af464c76d713c07ad', '晏丽', '2024004048@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1008, '2024004049', '25d55ad283aa400af464c76d713c07ad', '边鹏', '2024004049@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1009, '2024004050', '25d55ad283aa400af464c76d713c07ad', '麴芳', '2024004050@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1010, '2024004051', '25d55ad283aa400af464c76d713c07ad', '殷勇', '2024004051@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1011, '2024004052', '25d55ad283aa400af464c76d713c07ad', '国倩', '2024004052@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1012, '2024004053', '25d55ad283aa400af464c76d713c07ad', '欧涛', '2024004053@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1013, '2024004054', '25d55ad283aa400af464c76d713c07ad', '文萍', '2024004054@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1014, '2024004055', '25d55ad283aa400af464c76d713c07ad', '冉杰', '2024004055@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1015, '2024004056', '25d55ad283aa400af464c76d713c07ad', '寿婷', '2024004056@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1016, '2024004057', '25d55ad283aa400af464c76d713c07ad', '解峰', '2024004057@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1017, '2024004058', '25d55ad283aa400af464c76d713c07ad', '简欣', '2024004058@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1018, '2024004059', '25d55ad283aa400af464c76d713c07ad', '应超', '2024004059@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1019, '2024004060', '25d55ad283aa400af464c76d713c07ad', '荆静', '2024004060@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1020, '2024004061', '25d55ad283aa400af464c76d713c07ad', '饶龙', '2024004061@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1021, '2024004062', '25d55ad283aa400af464c76d713c07ad', '宁红', '2024004062@stud.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1022, '2024004063', '25d55ad283aa400af464c76d713c07ad', '储斌', '2024004063@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1023, '2024004064', '25d55ad283aa400af464c76d713c07ad', '桓洁', '2024004064@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1024, '2024004065', '25d55ad283aa400af464c76d713c07ad', '井军', '2024004065@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1025, '2024004066', '25d55ad283aa400af464c76d713c07ad', '麻艳', '2024004066@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1026, '2024004067', '25d55ad283aa400af464c76d713c07ad', '卜磊', '2024004067@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1027, '2024004068', '25d55ad283aa400af464c76d713c07ad', '扈娟', '2024004068@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1028, '2024004069', '25d55ad283aa400af464c76d713c07ad', '单伟', '2024004069@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1029, '2024004070', '25d55ad283aa400af464c76d713c07ad', '管丽', '2024004070@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1030, '2024004071', '25d55ad283aa400af464c76d713c07ad', '戎鹏', '2024004071@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1031, '2024004072', '25d55ad283aa400af464c76d713c07ad', '郜芳', '2024004072@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1032, '2024004073', '25d55ad283aa400af464c76d713c07ad', '屈勇', '2024004073@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1033, '2024004074', '25d55ad283aa400af464c76d713c07ad', '幸倩', '2024004074@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1034, '2024004075', '25d55ad283aa400af464c76d713c07ad', '姬涛', '2024004075@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1035, '2024004076', '25d55ad283aa400af464c76d713c07ad', '能萍', '2024004076@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1036, '2024004077', '25d55ad283aa400af464c76d713c07ad', '慕杰', '2024004077@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1037, '2024004078', '25d55ad283aa400af464c76d713c07ad', '居婷', '2024004078@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1038, '2024004079', '25d55ad283aa400af464c76d713c07ad', '习峰', '2024004079@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1039, '2024004080', '25d55ad283aa400af464c76d713c07ad', '党欣', '2024004080@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1040, '2024004081', '25d55ad283aa400af464c76d713c07ad', '米超', '2024004081@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1041, '2024004082', '25d55ad283aa400af464c76d713c07ad', '栾静', '2024004082@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1042, '2024004083', '25d55ad283aa400af464c76d713c07ad', '阳龙', '2024004083@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1043, '2024004084', '25d55ad283aa400af464c76d713c07ad', '充红', '2024004084@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1044, '2024004085', '25d55ad283aa400af464c76d713c07ad', '封斌', '2024004085@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1045, '2024004086', '25d55ad283aa400af464c76d713c07ad', '左洁', '2024004086@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1046, '2024004087', '25d55ad283aa400af464c76d713c07ad', '舒军', '2024004087@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1047, '2024004088', '25d55ad283aa400af464c76d713c07ad', '虞艳', '2024004088@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1048, '2024004089', '25d55ad283aa400af464c76d713c07ad', '皮磊', '2024004089@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1049, '2024004090', '25d55ad283aa400af464c76d713c07ad', '乐娟', '2024004090@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1050, '2024004091', '25d55ad283aa400af464c76d713c07ad', '卫伟', '2024004091@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1051, '2024004092', '25d55ad283aa400af464c76d713c07ad', '逄丽', '2024004092@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1052, '2024004093', '25d55ad283aa400af464c76d713c07ad', '滕鹏', '2024004093@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1053, '2024004094', '25d55ad283aa400af464c76d713c07ad', '伊芳', '2024004094@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1054, '2024004095', '25d55ad283aa400af464c76d713c07ad', '单勇', '2024004095@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1055, '2024004096', '25d55ad283aa400af464c76d713c07ad', '晏倩', '2024004096@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1056, '2024004097', '25d55ad283aa400af464c76d713c07ad', '巩涛', '2024004097@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1057, '2024004098', '25d55ad283aa400af464c76d713c07ad', '隋萍', '2024004098@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1058, '2024004099', '25d55ad283aa400af464c76d713c07ad', '訾杰', '2024004099@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1059, '2024004100', '25d55ad283aa400af464c76d713c07ad', '车婷', '2024004100@student.edu.cn', NULL, 'student', 1, '/src/assets/images/default-avatar.svg', 0);
INSERT INTO `sys_user` VALUES (1060, '2025002002', '25d55ad283aa400af464c76d713c07ad', '陈八', 'FaniaConwaya6x9h@learncolumbus.us', NULL, 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1061, '2025002006', '25d55ad283aa400af464c76d713c07ad', '陈二四', 'indiamilagros@ryjsd.asia', NULL, 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1062, '2025002003', '25d55ad283aa400af464c76d713c07ad', '杨九', 'test@student.edu.cn', NULL, 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1063, '2025004001', '25d55ad283aa400af464c76d713c07ad', '许十五', 'test1@student.edu.cn', NULL, 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1064, 'wei', '25d55ad283aa400af464c76d713c07ad', '韦管理员', 'wei@dormitory.edu.cn', '13143532121', 'admin', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1065, '2025004007', '25d55ad283aa400af464c76d713c07ad', '吕三三', 'JohnaKhan7lk7e@gghs96.org', '13143532122', 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1066, '2025004005', '25d55ad283aa400af464c76d713c07ad', '许三一', 'ToniaCline2eadf@gghs96.org', '13123412131', 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1067, '2025002008', '25d55ad283aa400af464c76d713c07ad', '周二六', '2025002008@qq.com', '13111121211', 'student', 1, NULL, 0);
INSERT INTO `sys_user` VALUES (1068, 'ma', '25d55ad283aa400af464c76d713c07ad', '马管理员', '111@qq.com', '13111212121', 'admin', 1, NULL, 1);

-- ----------------------------
-- Table structure for visitor
-- ----------------------------
DROP TABLE IF EXISTS `visitor`;
CREATE TABLE `visitor`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访客ID',
  `student_id` bigint(20) NULL DEFAULT NULL COMMENT '预约学生ID',
  `student_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预约学生姓名',
  `student_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学号',
  `student_room` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生宿舍（楼栋-房间号）',
  `building_id` bigint(20) NULL DEFAULT NULL COMMENT '楼栋ID（用于权限过滤）',
  `visitor_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访客电话',
  `visitor_id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '访客身份证号',
  `visit_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来访事由',
  `visit_date` date NOT NULL COMMENT '来访日期',
  `visit_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来访时段',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核，APPROVED-已批准，REJECTED-已拒绝，COMPLETED-已完成，CANCELLED-已取消',
  `admin_id` bigint(20) NULL DEFAULT NULL COMMENT '审核管理员ID',
  `admin_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核管理员姓名',
  `admin_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '管理员审批意见',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '拒绝原因',
  `approved_at` datetime NULL DEFAULT NULL COMMENT '审批时间',
  `checked_in_at` datetime NULL DEFAULT NULL COMMENT '登记时间',
  `checked_out_at` datetime NULL DEFAULT NULL COMMENT '离开时间',
  `remarks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  `deleted` int(11) NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_id`(`student_id`) USING BTREE,
  INDEX `idx_visit_date`(`visit_date`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_deleted`(`deleted`) USING BTREE,
  INDEX `idx_visitor_name`(`visitor_name`) USING BTREE,
  INDEX `idx_visitor_id_card`(`visitor_id_card`) USING BTREE,
  INDEX `idx_building_id`(`building_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '访客管理表' ROW_FORMAT = Compact;

-- ----------------------------
-- Records of visitor
-- ----------------------------
INSERT INTO `visitor` VALUES (3, 202, '封婷', '2023002002', '101', 3, '刘涛', '13112345678', '450913200107124317', '看望学生', '2025-12-11', '00:00', 'COMPLETED', 275, '李管理员', '审核通过', NULL, '2025-12-12 14:31:43', '2025-12-12 14:36:51', '2025-12-12 14:36:56', '无', '2025-12-12 14:31:09', 0);
INSERT INTO `visitor` VALUES (4, 202, '封婷', '2023002002', '101', 3, '五涛', '13112345611', '450913200107124316', '探望同学', '2025-12-15', '00:00', 'CHECKED_IN', 275, '李管理员', '同意来访', NULL, '2025-12-15 01:26:31', '2025-12-15 01:27:04', NULL, NULL, '2025-12-15 01:10:23', 0);

SET FOREIGN_KEY_CHECKS = 1;
