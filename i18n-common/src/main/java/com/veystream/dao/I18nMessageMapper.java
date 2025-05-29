package com.veystream.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param; // Added for @Param
import org.springframework.util.CollectionUtils; // Using Spring's CollectionUtils

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vincy.Xi
 */
public interface I18nMessageMapper extends BaseMapper<I18nMessage> {

    default Set<Long> findIdentityKeysByTextSearch(String keyword, String language, String type, String codePrefix) {
        // 1. 参数校验
        if (StringUtils.isBlank(keyword) || StringUtils.isBlank(language) || 
            StringUtils.isBlank(type) || StringUtils.isBlank(codePrefix)) {
            // Log warning or handle as appropriate if parameters are invalid
            // System.err.println("Warning: Invalid parameters for findIdentityKeysByTextSearch. Keyword, language, type, and codePrefix must not be blank.");
            return Collections.emptySet();
        }

        // 2. 创建 LambdaQueryWrapper
        LambdaQueryWrapper<I18nMessage> queryWrapper = new LambdaQueryWrapper<>();

        // 3. 设置查询条件
        queryWrapper.eq(I18nMessage::getLanguage, language)
                    .eq(I18nMessage::getType, type)
                    .likeRight(I18nMessage::getCode, codePrefix) // code LIKE 'codePrefix%'
                    .like(I18nMessage::getText, keyword);      // text LIKE '%keyword%'

        // 4. 执行查询
        List<I18nMessage> messages = this.selectList(queryWrapper);

        // 5. 处理空结果
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptySet();
        }

        // 6. 解析ID并收集
        Set<Long> ids = new HashSet<>();
        for (I18nMessage message : messages) {
            String code = message.getCode();
            if (StringUtils.isNotBlank(code)) {
                try {
                    // 假设 code 的格式为 "prefix.fieldName.ID"
                    // 例如: "Goods.name.123"
                    String idStr = code.substring(code.lastIndexOf('.') + 1);
                    if (StringUtils.isNotBlank(idStr) && idStr.matches("\\d+")) {
                        ids.add(Long.parseLong(idStr));
                    } else {
                        // 可以选择记录日志: code格式不符合预期，无法解析ID
                        System.err.println("Warning: Could not parse ID from I18nMessage code (format or non-numeric ID part): " + code);
                    }
                } catch (NumberFormatException e) {
                    // ID部分不是有效的数字
                     System.err.println("Error: ID part is not a valid number in code: " + code + " - " + e.getMessage());
                } catch (Exception e) {
                    // 其他可能的解析异常, e.g., StringIndexOutOfBoundsException if code has no '.'
                    System.err.println("Error parsing ID from code: " + code + " - " + e.getMessage());
                }
            }
        }
        
        // 7. 返回ID集合
        return ids;
    }

    Set<Long> findIdentityKeysByExactTextAndCodePrefix(@Param("text") String text,
                                                       @Param("language") String language,
                                                       @Param("type") String type,
                                                       @Param("codePrefix") String codePrefix);
}
