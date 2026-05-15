package cn.pzhdv.blog.service;

import cn.pzhdv.blog.entity.ArticleCategory;
import cn.pzhdv.blog.vo.category.CategoryPieChartVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章分类表 服务类
 * </p>
 *
 * @author PanZonghui
 * @since 2026-05-11 15:07:36
 */
public interface ArticleCategoryService extends IService<ArticleCategory> {

    /**
     * 递归查询分类及其所有子分类
     *
     * @param parentId 父分类ID
     * @return 分类树
     */
    List<ArticleCategory> listAllChildren(Long parentId);

    /**
     * 根据分类ID删除文章分类（含业务校验，返回详细结果）
     *
     * <p>核心业务逻辑： 1. 校验分类是否存在，不存在则返回失败 2. 检查是否有子分类，有则返回失败（需先删子分类） 3. 检查是否关联文章，关联则返回失败（需先解除关联） 4.
     * 无上述情况则执行删除（先清关联关系，再删分类）
     *
     * @param categoryId 分类ID（必填，正整数，如1001）
     * @return 结果Map，固定包含两个键：
     *     <ul>
     *       <li>success：布尔值，true=删除成功，false=删除失败
     *       <li>reason：字符串，成功时为"删除成功"，失败时为具体原因（如"分类不存在"）
     *     </ul>
     */
    Map<String, Object> deleteArticleCategoryById(Long categoryId);

    List<CategoryPieChartVO> queryArticleCategoryPieChart();
}

