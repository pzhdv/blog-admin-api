package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.constant.StatusConstants;
import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.mapper.SysMenuMapper;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import cn.pzhdv.blog.vo.common.TreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统菜单表 服务实现类
 * 优化点：
 * 1. 移除了未使用的 Comparator 字段
 * 2. 依然利用 SQL 排序 + LinkedHashMap 保持树形结构顺序
 * 3. 彻底消除递归查库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public Page<SysMenu> getMenuListTreePage(String menuName, Byte menuType, Byte status, Long rootParentId, Integer current, Integer size) {
        // 1. 分页查询顶级节点
        LambdaQueryWrapper<SysMenu> queryWrapper = buildBaseQueryWrapper(menuName, menuType, status);
        queryWrapper.eq(SysMenu::getParentId, rootParentId != null ? rootParentId : 0L);
        Page<SysMenu> page = sysMenuMapper.selectPage(new Page<>(current, size), queryWrapper);

        List<SysMenu> topMenus = page.getRecords();
        if (CollectionUtils.isEmpty(topMenus)) {
            return page;
        }

        // 2. 一次性查询所有相关菜单用于构建子树
        List<SysMenu> allPossibleMenus = sysMenuMapper.selectList(buildBaseQueryWrapper(menuName, menuType, status));

        // 3. 构建 ParentId -> Children 映射，使用 LinkedHashMap 保持 SQL 中的排序
        Map<Long, List<SysMenu>> childrenMap = allPossibleMenus.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId, LinkedHashMap::new, Collectors.toList()));

        // 4. 递归构建树
        topMenus.forEach(menu -> buildRecursiveTree(menu, childrenMap));

        return page;
    }

    @Override
    public List<SysMenu> getMenuRoutesByIds(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) return Collections.emptyList();

        // 1. 一次性查出所有启用的菜单
        List<SysMenu> allEnabledMenus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, StatusConstants.MENU_STATUS_ENABLED)
                        .orderByAsc(SysMenu::getOrder)
                        .orderByDesc(SysMenu::getCreateTime)
        );

        Map<Long, SysMenu> menuIdMap = allEnabledMenus.stream()
                .collect(Collectors.toMap(SysMenu::getMenuId, m -> m));

        // 2. 预先构建 ChildrenMap 优化查找性能
        Map<Long, List<SysMenu>> allChildrenMap = allEnabledMenus.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        // 3. 内存递归寻找所有关联 ID
        Set<Long> targetIds = new HashSet<>();
        for (Long id : menuIds) {
            findAllRelatedIds(id, menuIdMap, allChildrenMap, targetIds);
        }

        // 4. 过滤结果
        List<SysMenu> filteredMenus = allEnabledMenus.stream()
                .filter(m -> targetIds.contains(m.getMenuId()))
                .toList();

        // 5. 构建树形结构
        Map<Long, List<SysMenu>> filteredChildrenMap = filteredMenus.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<SysMenu> rootMenus = filteredMenus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0 || !targetIds.contains(m.getParentId()))
                .collect(Collectors.toList());

        rootMenus.forEach(menu -> buildRecursiveTree(menu, filteredChildrenMap));
        return rootMenus;
    }

    @Override
    public List<TreeVO> getAllMenuTree(Long rootParentId) {
        List<SysMenu> allMenus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, StatusConstants.MENU_STATUS_ENABLED)
                        .orderByAsc(SysMenu::getOrder)
                        .orderByDesc(SysMenu::getCreateTime)
        );

        if (CollectionUtils.isEmpty(allMenus)) return Collections.emptyList();

        Map<Long, List<SysMenu>> childrenMap = allMenus.stream()
                .filter(m -> m.getParentId() != null && m.getParentId() != 0)
                .collect(Collectors.groupingBy(SysMenu::getParentId, LinkedHashMap::new, Collectors.toList()));

        final Long finalRootId = rootParentId != null ? rootParentId : 0L;
        return allMenus.stream()
                .filter(m -> finalRootId.equals(m.getParentId()))
                .map(m -> convertToTreeVO(m, childrenMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysMenu> getAllPages(Byte menuType) {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, StatusConstants.MENU_STATUS_ENABLED)
                        .eq(SysMenu::getMenuType, menuType)
                        .orderByAsc(SysMenu::getOrder)
                        .orderByDesc(SysMenu::getCreateTime)
        );
    }

    // ==================== 私有工具方法 ====================

    private LambdaQueryWrapper<SysMenu> buildBaseQueryWrapper(String menuName, Byte menuType, Byte status) {
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaLikeCondition(queryWrapper, SysMenu::getMenuName, menuName);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysMenu::getMenuType, menuType);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysMenu::getStatus, status);
        queryWrapper.orderByAsc(SysMenu::getOrder).orderByDesc(SysMenu::getCreateTime);
        return queryWrapper;
    }

    private void buildRecursiveTree(SysMenu parent, Map<Long, List<SysMenu>> childrenMap) {
        List<SysMenu> children = childrenMap.get(parent.getMenuId());
        if (!CollectionUtils.isEmpty(children)) {
            parent.setChildren(children);
            children.forEach(child -> buildRecursiveTree(child, childrenMap));
        }
    }

    private TreeVO convertToTreeVO(SysMenu menu, Map<Long, List<SysMenu>> childrenMap) {
        TreeVO vo = new TreeVO();
        vo.setValue(menu.getMenuId());
        vo.setLabel(menu.getMenuName());
        vo.setI18nKey(menu.getI18nKey());
        List<SysMenu> children = childrenMap.get(menu.getMenuId());
        if (!CollectionUtils.isEmpty(children)) {
            vo.setChildren(children.stream()
                    .map(c -> convertToTreeVO(c, childrenMap))
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private void findAllRelatedIds(Long currentId, Map<Long, SysMenu> menuIdMap, Map<Long, List<SysMenu>> allChildrenMap, Set<Long> resultIds) {
        if (currentId == null || currentId == 0 || resultIds.contains(currentId)) return;

        SysMenu menu = menuIdMap.get(currentId);
        if (menu == null) return;

        resultIds.add(currentId);
        // 向上找父
        findAllRelatedIds(menu.getParentId(), menuIdMap, allChildrenMap, resultIds);
        // 向下找子 (使用预先构建好的 allChildrenMap 提高性能)
        List<SysMenu> children = allChildrenMap.get(currentId);
        if (children != null) {
            children.forEach(c -> findAllRelatedIds(c.getMenuId(), menuIdMap, allChildrenMap, resultIds));
        }
    }
}