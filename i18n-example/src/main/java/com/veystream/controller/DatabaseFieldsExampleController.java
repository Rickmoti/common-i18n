package com.veystream.controller;

import com.veystream.dto.GoodsCreationPayload;
import com.veystream.dto.GoodsUpdatePayload;
import com.veystream.entity.Goods;
import com.veystream.http.BaseResult;
import com.veystream.service.GoodsService; // New import
// import com.veystream.service.TestService; // May remove if GoodsService replaces its usage for goods

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 数据库字段多语言
 *
 * @author Vincy.Xi
 */
@RestController
@RequestMapping("/example")
public class DatabaseFieldsExampleController {

    @Resource
    private GoodsService goodsService; // Use GoodsService

    // If TestService is still needed for other things, keep it.
    // @Resource
    // private TestService testService; 

    // POST endpoint to create Goods with translations
    @PostMapping("/goods")
    public BaseResult<Goods> createGoods(@RequestBody GoodsCreationPayload payload) {
        Goods createdGoods = goodsService.createGoods(payload.getGoods(), payload.getTranslations());
        return BaseResult.<Goods>builder().data(createdGoods).build();
    }

    // PUT endpoint to update Goods with translations
    @PutMapping("/goods/{id}")
    public BaseResult<Goods> updateGoods(@PathVariable Long id, @RequestBody GoodsUpdatePayload payload) {
        Goods goodsToUpdate = payload.getGoods();
        goodsToUpdate.setId(id); // Ensure ID from path is used
        Goods updatedGoods = goodsService.updateGoods(goodsToUpdate, payload.getTranslations());
        return BaseResult.<Goods>builder().data(updatedGoods).build();
    }

    // GET endpoint to retrieve Goods with all its translations
    @GetMapping("/goods/{id}")
    public BaseResult<Map<String, Object>> getGoodsWithTranslations(@PathVariable Long id) {
        Map<String, Object> data = goodsService.getGoodsWithAllTranslations(id);
        if (data == null) {
            // Consider returning a 404 or a specific error structure
            return BaseResult.<Map<String, Object>>builder().code("404").message("Goods not found").build();
        }
        return BaseResult.<Map<String, Object>>builder().data(data).build();
    }

    // Existing endpoint, now using GoodsService to list all goods
    // (without extensive translation details for the list view for performance)
    @GetMapping("/databaseFields") // Or change path to "/goods" for consistency
    public BaseResult<List<Goods>> getDatabaseFields() {
        List<Goods> goodsList = goodsService.getAllGoods();
        return BaseResult.<List<Goods>>builder().data(goodsList).build();
    }

    // GET endpoint to search Goods by keyword
    @GetMapping("/goods/search")
    public BaseResult<List<Goods>> searchGoods(@RequestParam(name = "keyword", required = false) String keyword) {
        List<Goods> goodsList = goodsService.searchGoods(keyword);
        return BaseResult.<List<Goods>>builder().data(goodsList).build();
    }
}
