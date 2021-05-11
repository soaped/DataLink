package com.datalink.user.controller;

import com.datalink.base.model.PageResult;
import com.datalink.base.model.Result;
import com.datalink.base.model.Menu;
import com.datalink.user.service.MenuService;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
/**
 * 菜单 Controller
 *
 * @author wenmo
 * @since 2021-05-10
 */
@Slf4j
@Api(tags = "菜单模块api")
@RestController
@RequestMapping("/menu")
public class MenuController {
    @Autowired
    private MenuService menuService;

    /**
     * 新增或者更新
     */

    @ApiOperation(value = "动态新增修改")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "菜单ID", required = true, dataType = "int(11)"),
            @ApiImplicitParam(name = "parentId", value = "父ID", required = true , dataType = "int(11)"),
            @ApiImplicitParam(name = "name", value = "名称", required = true , dataType = "varchar(64)"),
            @ApiImplicitParam(name = "url", value = "Url", required =  false , dataType = "varchar(1024)"),
            @ApiImplicitParam(name = "path", value = "路由", required =  false , dataType = "varchar(1024)"),
            @ApiImplicitParam(name = "pathMethod", value = "方法", required =  false , dataType = "varchar(10)"),
            @ApiImplicitParam(name = "css", value = "样式", required =  false , dataType = "varchar(32)"),
            @ApiImplicitParam(name = "sort", value = "排序", required = true , dataType = "int(11)"),
            @ApiImplicitParam(name = "createTime", value = "创建时间", required = true, dataType = "datetime"),
            @ApiImplicitParam(name = "updateTime", value = "更新时间", required = true, dataType = "datetime"),
            @ApiImplicitParam(name = "type", value = "类型", required = true , dataType = "tinyint(1)"),
            @ApiImplicitParam(name = "hidden", value = "隐藏", required = true , dataType = "tinyint(1)"),
            @ApiImplicitParam(name = "tenantId", value = "租户字段", required =  false , dataType = "varchar(32)"),
    })
    @PostMapping("/saveOrUpdate")
    public Result saveOrUpdate(@RequestBody Menu menu) throws Exception {
        return menuService.saveOrUpdateMenu(menu);
    }

    /**
     * 动态查询列表
     */
    @ApiOperation(value = "动态查询列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页起始位置", required = false, dataType = "Integer"),
            @ApiImplicitParam(name = "limit", value = "分页结束位置", required = false, dataType = "Integer"),
            @ApiImplicitParam(name = "sortField", value = "排序字段", required = false, dataType = "String"),
            @ApiImplicitParam(name = "sortValue", value = "排序值", required = false, dataType = "String"),
            @ApiImplicitParam(name = "searchField", value = "搜索字段", required = false, dataType = "String"),
            @ApiImplicitParam(name = "searchValue", value = "搜索值", required = false, dataType = "String")
    })
    @PostMapping("/list")
    public PageResult<Menu> listMenus(@RequestBody JsonNode para) {
        return menuService.selectForCTable(para);
    }

    /**
     * 批量删除
     */
    @ApiOperation(value = "批量删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "ID数组字符串", required = true, dataType = "String")
    })
    @DeleteMapping(value = "")
    public Result deleteMul(String ids) {
        if(ids==null||ids.equals("")){
            return Result.failed("请选择要删除的记录");
        }
        String[] idstrs = ids.split(",");
        List<Integer> error = new ArrayList<>();
        for (int i = 0; i < idstrs.length; i++) {
            Integer id =Integer.valueOf(idstrs[i]);
            if(!menuService.removeById(id)){
                error.add(id);
            }
        }
        if(error.size()==0) {
        return Result.succeed("删除成功");
        }else{
        return Result.succeed("删除部分成功，但"+error.toString()+"删除失败，共"+error.size()+"次失败。");
        }
    }

    /**
     * 获取指定ID的信息
     */
    @ApiOperation(value = "获取指定ID的信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户ID", required = true, dataType = "Integer")
    })
    @PostMapping("/getOneById")
    public Result getOneById(@RequestBody Menu menu) throws Exception {
        menu = menuService.getById(menu.getId());
        return Result.succeed(menu,"获取成功");
    }
}
