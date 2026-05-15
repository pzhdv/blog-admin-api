package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.BlogAuthor;
import cn.pzhdv.blog.exception.BusinessException;
import cn.pzhdv.blog.mapper.BlogAuthorMapper;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.service.BlogAuthorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 用户个人信息表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 19:08:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogAuthorServiceImpl extends ServiceImpl<BlogAuthorMapper, BlogAuthor> implements BlogAuthorService {

    private final BlogAuthorMapper blogAuthorMapper;

    @Override
    public BlogAuthor getUniqueAuthor() {
        // 统计数量，绝对不会返回 null
        LambdaQueryWrapper<BlogAuthor> countWrapper = new LambdaQueryWrapper<>();
        long count = blogAuthorMapper.selectCount(countWrapper);

        // 数据不唯一
        if (count > 1) {
            log.error("博客作者信息不唯一，存在{}条数据", count);
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "作者信息不唯一（" + count + "条数据）");
        }

        // 查询唯一作者
        LambdaQueryWrapper<BlogAuthor> queryWrapper = new LambdaQueryWrapper<>();
        List<BlogAuthor> authorList = blogAuthorMapper.selectList(queryWrapper);

        return authorList.isEmpty() ? null : authorList.get(0);
    }
}
