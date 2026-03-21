package com.campus.love.ai.prompt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 与 {@code test.py} 中 {@code STYLES} 逐字一致，禁止擅自改写。
 */
public final class AvatarStudioPrompts {

    private static final Map<String, String> PROMPTS = new LinkedHashMap<>();

    static {
        PROMPTS.put(
                "油画风",
                "将这张照片转换为油画风格人像。笔触明显可见,用宽笔触归纳皮肤和背景的色块,但面部五官区域笔触细化,确保眼睛、鼻子、嘴巴清晰可辨认。色调以暖褐、赭石、金棕为主,颜料有堆叠肌理感。风格参考萨金特人物肖像——整体有油画质感和笔触,但人物面部保持足够清晰度,一眼认得出本人。严格保持人物面部特征和五官比例不变。");
        PROMPTS.put(
                "动漫风",
                "将这张照片转换为写实向日系动漫风格人像。眼睛严格按照原图大小和形状还原,禁止放大眼睛,禁止添加过多高光和星型反光,眼形与真人保持一致。皮肤平涂细腻,赛璐珞着色风格。线条干净,发丝分组流畅。整体风格参考《你的名字》《天气之子》中的写实人物面孔,动漫质感但五官比例完全贴近真人。严格保持人物面部特征、五官大小和位置不变。");
        PROMPTS.put(
                "素描风",
                "将这张照片转换为铅笔素描人像。面部用细密短排线充分刻画明暗结构,鼻翼、眼窝、颧骨、下巴阴影区域排线叠加2-3层,体现立体感。皮肤亮部用轻排线铺底而非完全留白,保留纸面质感但不空洞。发丝用流畅细线根根分明。背景轻淡排线简单交代空间,不完全留白。整体如美术生课堂作业级别的完成度,扎实而不沉闷。严格保持人物面部特征和五官比例不变。");
        PROMPTS.put(
                "赛博风",
                "将这张照片转换为高度赛博朋克化人像大片。人物佩戴大框全息数字墨镜,镜片为深色单向透视材质,镜面上有流动的红色或蓝色数据代码反射,镜框有发光边缘和悬浮全息小屏幕。人物面部有霓虹光投影,皮肤局部隐约可见发光电路纹路。背景为超高密度霓虹招牌未来都市夜景,空气中有轻微雨雾颗粒。色彩以品红、电蓝、荧光绿三原色霓虹为主,强烈色差,顶级赛博朋克概念艺术质感。严格保持人物面部特征和五官比例不变。");
        PROMPTS.put(
                "高管工作照",
                "将这张照片转换为高端商业人像,苹果高管发布会风格。画面构图：半身构图,人物轻微转向镜头(随机向左或者向右),侧脸角度,头部自然微倾,眼神直视镜头,表情自信从容略带微笑,姿态放松而专业。服装与形象：极简商务正装,整洁利落,无多余装饰。光影：单侧柔光灯打亮面部轮廓,眼睛产生自然高光点,面部阴影过渡细腻,皮肤质感真实通透。背景：浅灰纯色渐变背景,主体与背景边缘自然分离,无杂乱元素。整体氛围：极简主义构图,主体偏离画面中心,留白充足,精致胶片颗粒感,沉稳、高端、永恒质感,世界500强高管形象。禁止出现：文字、Logo、杂物、过度美颜、塑料感皮肤。");
        PROMPTS.put(
                "柯达胶片",
                "柯达 Portra 400 胶片,高级时尚大片质感,皮肤细腻均匀无瑕疵,五官阴影自然立体,眼白清澈,嘴唇饱满有光泽,低对比度奶油色调,自然颗粒感,法式优雅,真实摄影质感,皮肤毛孔纹理自然可见,超清锐利细节,85mm 定焦镜头人像,专业自然光,杂志封面级别");
    }

    private AvatarStudioPrompts() {}

    public static Set<String> styleKeys() {
        return Collections.unmodifiableSet(PROMPTS.keySet());
    }

    public static String promptForStyle(String styleKey) {
        return PROMPTS.get(styleKey);
    }

    public static boolean isValidStyle(String styleKey) {
        return styleKey != null && PROMPTS.containsKey(styleKey);
    }
}
