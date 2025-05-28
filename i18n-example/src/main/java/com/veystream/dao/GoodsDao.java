package com.veystream.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.veystream.entity.Goods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils; // Using Spring's CollectionUtils

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Vincy.Xi
 * GoodsDao is now an interface extending BaseMapper to support default methods.
 */
public interface GoodsDao extends BaseMapper<Goods> {

    default Set<Long> findIdsByKeywordAndFields(String keyword, List<String> fieldNames) {
        // 1. 参数校验
        if (StringUtils.isBlank(keyword) || CollectionUtils.isEmpty(fieldNames)) {
            return Collections.emptySet();
        }

        // 2. 创建 LambdaQueryWrapper
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();

        // 3. & 4. 构建 OR LIKE 条件组
        // This variable will track if any valid field condition was added.
        final boolean[] hasValidCondition = {false};

        queryWrapper.and(wq -> {
            boolean firstCondition = true;
            for (String fieldName : fieldNames) {
                if ("name".equalsIgnoreCase(fieldName)) {
                    if (!firstCondition) wq.or();
                    wq.like(Goods::getName, keyword);
                    firstCondition = false;
                    hasValidCondition[0] = true;
                } else if ("description".equalsIgnoreCase(fieldName)) {
                    if (!firstCondition) wq.or();
                    wq.like(Goods::getDescription, keyword);
                    firstCondition = false;
                    hasValidCondition[0] = true;
                } else if ("image".equalsIgnoreCase(fieldName)) {
                    if (!firstCondition) wq.or();
                    wq.like(Goods::getImage, keyword);
                    firstCondition = false;
                    hasValidCondition[0] = true;
                } else {
                    System.err.println("Warning: Field '" + fieldName + "' is not configured for search in GoodsDao.findIdsByKeywordAndFields");
                }
            }
        });
        
        // If no valid conditions were added to the queryWrapper (e.g., all fieldNames were unrecognized),
        // then running the query might return all records. Prevent this.
        if (!hasValidCondition[0]) {
            return Collections.emptySet();
        }

        // 5. 执行查询
        List<Goods> goodsList = this.selectList(queryWrapper);

        // 6. 处理空结果
        if (CollectionUtils.isEmpty(goodsList)) {
            return Collections.emptySet();
        }

        // 7. & 8. 收集ID并返回
        return goodsList.stream()
                        .map(Goods::getId)
                        .collect(Collectors.toSet());
    }
}
