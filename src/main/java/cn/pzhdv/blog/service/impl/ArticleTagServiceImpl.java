package cn.pzhdv.blog.service.impl;

import cn.pzhdv.blog.entity.ArticleTag;
import cn.pzhdv.blog.entity.ArticleTagRelation;
import cn.pzhdv.blog.mapper.ArticleTagMapper;
import cn.pzhdv.blog.mapper.ArticleTagRelationMapper;
import cn.pzhdv.blog.service.ArticleTagService;
import cn.pzhdv.blog.utils.QueryWrapperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * 文章标签表 服务实现类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag>
        implements ArticleTagService {

    // 注入文章-标签关联表Mapper（用于检查标签是否关联文章）
    private final ArticleTagRelationMapper articleTagRelationMapper;
    // 注入标签自身Mapper（也可直接用父类的baseMapper，此处显式声明更清晰）
    private final ArticleTagMapper articleTagMapper;

    /**
     * 根据标签ID删除标签（含关联检查：有关联文章则删除失败）
     *
     * @param articleTagId 标签ID
     * @return true=删除成功，false=删除失败（关联文章/标签不存在）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 加事务，避免异常时数据不一致
    public Map<String, Object> deleteArticleTagById(Long articleTagId) {
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("success", false); // 默认删除失败

        // 1. 先校验标签是否存在（避免删除不存在的标签）
        ArticleTag tag = articleTagMapper.selectById(articleTagId);
        if (tag == null) {
            resultMap.put("reason", "标签不存在或已删除");
            log.warn("标签删除失败 | 标签不存在 | 标签ID: {}", articleTagId);
            return resultMap;
        }


        // 2. 检查标签是否关联文章
        LambdaQueryWrapper<ArticleTagRelation> relationWrapper = new LambdaQueryWrapper<>();
        QueryWrapperUtil.addLambdaEqCondition(relationWrapper, ArticleTagRelation::getArticleTagId, articleTagId);
        Long count = articleTagRelationMapper.selectCount(relationWrapper);
        long relationCount = count == null ? 0 : count;
        if (relationCount > 0) {
            resultMap.put("reason", "该标签已关联" + relationCount + "篇文章，请先解除关联");
            log.warn("标签删除失败 | 标签已关联文章 | 标签ID: {}, 关联文章数量: {}", articleTagId, relationCount);
            return resultMap; // 有关联文章，不允许删除
        }

        // 3. 无关联，执行删除（删除标签自身）
        int deleteRows = articleTagMapper.deleteById(articleTagId);
        if (deleteRows <= 0) {
            resultMap.put("reason", "删除失败，请稍后重试");
            log.error("标签删除失败 | 数据库执行删除无影响行数 | 标签ID: {}", articleTagId);
            return resultMap;
        }

        // 4. 删除成功，打印日志
        resultMap.put("success", true);
        resultMap.put("reason", "删除成功");
        log.info("标签删除成功 | 标签ID: {}, 标签名称: {}", articleTagId, tag.getArticleTagName());
        return resultMap;
    }
}
