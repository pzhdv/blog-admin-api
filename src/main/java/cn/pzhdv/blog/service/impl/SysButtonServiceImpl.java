package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.constant.MenuConstants;
import cn.pzhdv.blog.entity.SysMenu;
import cn.pzhdv.blog.entity.SysButton;
import cn.pzhdv.blog.mapper.SysButtonMapper;
import cn.pzhdv.blog.service.SysButtonService;
import cn.pzhdv.blog.service.SysMenuService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统按钮权限表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-05 19:36:10
 */
@Service
public class SysButtonServiceImpl extends ServiceImpl<SysButtonMapper, SysButton> implements SysButtonService {

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 条件分页查询按钮权限列表
     */
    @Override
    public Page<SysButton> querySysButtonListByConditionPage(Long menuId, Byte status, Integer current, Integer size) {
        LambdaQueryWrapper<SysButton> queryWrapper = buildQueryWrapper(menuId, status);
        queryWrapper.orderByDesc(SysButton::getMenuId);

        Page<SysButton> page = new Page<>(current, size);
        Page<SysButton> resultPage = baseMapper.selectPage(page, queryWrapper);

        fillMenuInfo(resultPage.getRecords());

        return resultPage;
    }


    /**
     * 校验按钮编码是否存在
     */
    @Override
    public boolean checkButtonCodeExists(String buttonCode) {
        LambdaQueryWrapper<SysButton> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysButton::getButtonCode, buttonCode);
        queryWrapper.select(SysButton::getButtonId);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 条件查询按钮权限列表（不分页）
     */
    @Override
    public List<SysButton> querySysButtonListByCondition(Long menuId, Byte status) {
        LambdaQueryWrapper<SysButton> queryWrapper = buildQueryWrapper(menuId, status);
        queryWrapper.orderByDesc(SysButton::getMenuId);

        List<SysButton> buttonList = this.list(queryWrapper);
        fillMenuInfo(buttonList);
        return buttonList;
    }

    /**
     * 构建按钮权限查询条件
     */
    private LambdaQueryWrapper<SysButton> buildQueryWrapper(Long menuId, Byte status) {
        LambdaQueryWrapper<SysButton> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysButton::getMenuId, menuId);
        QueryWrapperUtil.addLambdaEqCondition(queryWrapper, SysButton::getStatus, status);
        return queryWrapper;
    }

    /**
     * 填充名称和国际化key
     */
    private void fillMenuInfo(List<SysButton> buttonList) {
        if (buttonList == null || buttonList.isEmpty()) {
            return;
        }

        Set<Long> menuIds = buttonList.stream()
                .map(SysButton::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (menuIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SysMenu::getMenuId, menuIds);
        Map<Long, SysMenu> menuMap = sysMenuService.list(menuWrapper).stream()
                .collect(Collectors.toMap(SysMenu::getMenuId, m -> m, (k1, k2) -> k1));

        for (SysButton button : buttonList) {
            SysMenu menu = menuMap.get(button.getMenuId());
            if (menu != null) {
                button.setMenuName(menu.getMenuName());
                button.setMenuI18nKey(menu.getI18nKey());
            }
        }
    }

}
