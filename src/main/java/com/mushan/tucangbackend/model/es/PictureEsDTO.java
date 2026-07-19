package com.mushan.tucangbackend.model.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.model.color.ColorPaletteItem;
import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import com.mushan.tucangbackend.model.color.PictureColorScores;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.utils.ColorPaletteUtils;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片 Elasticsearch 实体类
 */
@Data
@Document(indexName = "picture")
public class PictureEsDTO implements Serializable {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @Field(type = FieldType.Double)
    private Double picScale;

    /**
     * 图片格式
     */
    @Field(type = FieldType.Keyword)
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 编辑时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date editTime;

    /**
     * 更新时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Field(type = FieldType.Text)
    private String reviewMessage;

    /**
     * 审核人 id
     */
    @Field(type = FieldType.Long)
    private Long reviewerId;

    /**
     * 审核时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    /**
     * 点赞数
     */
    private Integer likeCount = 0;

    /**
     * 收藏数
     */
    private Integer favoriteCount = 0;

    /**
     * 图片主色调
     */
    @Field(type = FieldType.Keyword)
    private String picColor;

    /**
     * 图片的 Lab 调色板。
     */
    @Field(type = FieldType.Nested)
    private List<ColorPaletteItem> colorPalette;

    /**
     * 可用于 term filter 的十种标准色标签。
     */
    @Field(type = FieldType.Keyword)
    private List<String> colorTags;

    /**
     * 查询时用于 field value factor 排序的十种标准色离线分数。
     */
    @Field(type = FieldType.Object)
    private PictureColorScores colorScores;

    @Field(type = FieldType.Integer)
    private Integer colorAlgoVersion;

    /**
     * 是否删除
     */
    private Integer isDelete;


    /**
     * 将实体类转换为ES包装类
     *
     * @param picture 图片实体类
     * @return PictureEsDTO
     */
    public static PictureEsDTO objToDto(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureEsDTO pictureEsDTO = new PictureEsDTO();
        BeanUtil.copyProperties(
                picture,
                pictureEsDTO,
                "tags",
                "colorPalette",
                "colorTags",
                "colorScores"
        );
        String tags = picture.getTags();
        if(tags != null){
            pictureEsDTO.setTags(JSONUtil.toList(tags, String.class));
        }
        if (StrUtil.isNotBlank(picture.getColorPalette())) {
            pictureEsDTO.setColorPalette(JSONUtil.toList(picture.getColorPalette(), ColorPaletteItem.class));
        }
        if (StrUtil.isNotBlank(picture.getColorTags())) {
            pictureEsDTO.setColorTags(JSONUtil.toList(picture.getColorTags(), String.class));
        }
        if (StrUtil.isNotBlank(picture.getColorScores())) {
            pictureEsDTO.setColorScores(JSONUtil.toBean(picture.getColorScores(), PictureColorScores.class));
        }
        // 兼容仅保存了旧平均色的数据，重建索引后也能使用 term/rank 搜索。
        if (pictureEsDTO.getColorScores() == null && StrUtil.isNotBlank(picture.getPicColor())) {
            try {
                ColorAnalysisResult fallback = ColorPaletteUtils.fromAverageColor(picture.getPicColor());
                pictureEsDTO.setColorPalette(fallback.getPalette());
                pictureEsDTO.setColorTags(fallback.getColorTags());
                pictureEsDTO.setColorScores(fallback.getColorScores());
                pictureEsDTO.setColorAlgoVersion(fallback.getAlgorithmVersion());
            } catch (IllegalArgumentException ignored) {
                // 历史脏颜色不应阻断整个 ES 全量同步。
            }
        }
        return pictureEsDTO;
    }

    /**
     * 将ES包装类转换为实体类
     *
     * @param pictureEsDTO ES包装类
     * @return Picture
     */
    public static Picture dtoToObj(PictureEsDTO pictureEsDTO) {
        if (pictureEsDTO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(
                pictureEsDTO,
                picture,
                "tags",
                "colorPalette",
                "colorTags",
                "colorScores"
        );
        List<String> tags = pictureEsDTO.getTags();
        if(tags != null){
            picture.setTags(JSONUtil.toJsonStr(tags));
        }
        if (pictureEsDTO.getColorPalette() != null) {
            picture.setColorPalette(JSONUtil.toJsonStr(pictureEsDTO.getColorPalette()));
        }
        if (pictureEsDTO.getColorTags() != null) {
            picture.setColorTags(JSONUtil.toJsonStr(pictureEsDTO.getColorTags()));
        }
        if (pictureEsDTO.getColorScores() != null) {
            picture.setColorScores(JSONUtil.toJsonStr(pictureEsDTO.getColorScores()));
        }
        return picture;
    }
}
