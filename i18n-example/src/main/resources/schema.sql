-- ----------------------------
-- Table structure for i18n_message
-- ----------------------------
DROP TABLE IF EXISTS `i18n_message`;
CREATE TABLE `i18n_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `type` VARCHAR(100) DEFAULT NULL COMMENT '类型：常量；枚举；错误码；数据库表；',
  `code` VARCHAR(255) NOT NULL COMMENT '词条编码，例如：com.veystream.entity.Goods.name.1 (实体类全路径.属性名.业务id)',
  `text` TEXT DEFAULT NULL COMMENT '词条内容',
  `language` VARCHAR(50) NOT NULL COMMENT '语言，例如：zh_CN, en_US',
  `created_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_language` (`code`, `language`) COMMENT '词条编码和语言唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化词条表';

-- ----------------------------
-- Table structure for t_goods
-- ----------------------------
DROP TABLE IF EXISTS `t_goods`;
CREATE TABLE `t_goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `name` VARCHAR(255) DEFAULT NULL COMMENT '商品名称 (默认语言)',
  `description` TEXT DEFAULT NULL COMMENT '商品描述 (默认语言)',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片路径 (默认语言)',
  -- 如果 Goods.java 实体类中还有其他非国际化字段，也应在此处添加
  -- 例如: `price` DECIMAL(10,2) DEFAULT NULL COMMENT '商品价格',
  -- `stock_quantity` INT DEFAULT 0 COMMENT '库存数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
