#!/usr/bin/env python3
"""
缘分解析 Prompt 调试脚本
修改 SYSTEM_PROMPT 和 USER_PROMPT 后直接运行，查看 AI 生成效果。
需要：pip install openai
环境变量：AI_API_KEY（或 OPENAI_API_KEY）用于 DashScope/OpenAI 兼容接口
"""

import os
import json

# 使用 OpenAI 兼容接口（通义千问 DashScope 也兼容）
try:
    from openai import OpenAI
except ImportError:
    print("请先安装: pip install openai")
    exit(1)

# ============= 配置 =============
API_KEY = os.getenv("AI_API_KEY","sk-6d110762b8914167b80ba848e3166bc0") or os.getenv("OPENAI_API_KEY")
BASE_URL = os.getenv("AI_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
MODEL = os.getenv("AI_MODEL", "qwen3-max")

# ============= 编造的两组测试数据（异性） =============
USER_A = {
    "nickname": "小深",
    "gender": "男",
    "age": 22,
    "school": "深圳大学",
    "grade": "研二",
    "mbti": "INTJ",
    "zodiac": "金牛座",
    "interests": "摄影,独立音乐,编程,咖啡",
    "major": "人工智能",
}

USER_B = {
    "nickname": "小美",
    "gender": "女",
    "age": 21,
    "school": "深圳大学",
    "grade": "研一",
    "mbti": "ENFP",
    "zodiac": "双子座",
    "interests": "摄影,电影,旅行,美食,瑜伽",
    "major": "建筑学",
}

# 匹配分数（模拟）
MATCH_SCORES = {
    "totalScore": 78,
    "interestScore": 85,
    "mbtiScore": 72,
    "zodiacScore": 65,
    "baziScore": 70,
    "majorScore": 60,
}

# ============= Prompt 模板（与 Java 中保持一致，可修改调试） =============
SYSTEM_PROMPT = """你是 Campus Love 的「首席AI情感与潜意识分析师」。你精通荣格心理学、MBTI 认知功能理论、行为数据分析以及剧本级别的场景叙事。

你的分析准则：
1. 绝对的“有理有据”：拒绝任何没有数据支撑的空洞抒情。你的每一个结论（如互补、同频、冲突），都必须精准溯源到二人的客观信息（具体分数、MBTI 八维功能、专业壁垒或具体爱好的交集）。
2. 深度剖析：不要停留在“你内向我外向”的表层，要深入到信息处理机制（如 Ni 与 Ne 的信息摄取差异，Te 与 Fi 的决策价值差异）。
3. 电影级场景：在预演场景时，要像王家卫或李安的剧本，注重微小的肢体语言、专业元素的巧妙植入、以及极具张力的心理“推拉感”。
4. 语言质感：兼具学术的严谨与文学的浪漫，充满“智性恋”的审美。

⚠️ 严格格式要求：
- 必须且只能输出一个合法的 JSON 对象。
- 绝对不要输出任何 Markdown 标记（禁止使用 ```json 包裹），禁止输出任何解释性前言或后语。
- JSON 中的所有字符串必须使用英文双引号包裹。
"""

USER_PROMPT_TEMPLATE = """--------------------------------
【用户A客观数据】
- 基础画像：{nicknameA} | {genderA} | {ageA}岁 | {schoolA}{gradeA} | {majorA}专业
- 心理与星象基建：MBTI={mbtiA} | 星座={zodiacA}
- 偏好标签：[{interestsA}]

【用户B客观数据】
- 基础画像：{nicknameB} | {genderB} | {ageB}岁 | {schoolB}{gradeB} | {majorB}专业
- 心理与星象基建：MBTI={mbtiB} | 星座={zodiacB}
- 偏好标签：[{interestsB}]

【系统匹配硬指标】
- 综合缘分评分：{totalScore}/100
- 细分维度得分：兴趣={interestScore} | MBTI={mbtiScore} | 星座={zodiacScore} | 八字={baziScore} | 专业跨度={majorScore}
--------------------------------

【输出结构与深度指令】
请基于上述所有硬数据，深度推演两人的关系网络，并严格按照以下 JSON 字段和篇幅要求输出：

{{
  "_dataReasoning": "（必填草稿，不对用户展示）请先在此处用200字进行硬核逻辑推演。列出支持他们相吸的具体数据点（如：A的什么专业特质完美嵌合了B的什么MBTI功能？85分的兴趣分具体体现在哪两个爱好的化学反应上？低分维度会引发什么具体现实矛盾？）",

  "yuanFenIndex": "（15字以内）基于数据推演出的核心精神羁绊词，需具有物理或建筑学美感。",

  "dataBackedOverview": "（约120字）将冷冰冰的【系统匹配硬指标】翻译成兼具理性和宿命感的综合判词。必须隐晦但不突兀地融入综合分、最高分维度和最低分维度背后的现实意义。例如解释为什么某一项得分不高，但在总体上却构成了绝妙的制衡。",

  "cognitiveArchitecture": "（约400字）MBTI底层逻辑与专业属性的深度融合分析。
      要求：
      1. 必须使用具体的认知功能术语（如Ni, Ne, Ti, Fe等）剖析两人信息处理和决策模型的差异。
      2. 必须将两人的【专业属性】（如人工智能的底层逻辑 vs 建筑学的空间感知）作为认知功能的放大器进行分析。
      3. 举证分析：在面临认知重构或重大决策的场景中，对方的劣势功能如何被己方的优势功能完美接管（实现数据上的互补）。",

  "interestResonance": "（约150字）基于具体的【偏好标签】和【兴趣得分】，进行跨界化学反应分析。不要仅停留在“都有摄影爱好”的重合点，更要深挖“咖啡+瑜伽”或“独立音乐+电影”这种不同爱好之间如何相互渗透，形成更高阶的生活方式闭环。",

  "cinematicScene": "（约250字）高颗粒度、强张力的校园宿命感微电影预演。
      要素要求：
      1. 场景必须是一个能同时触发两人【专业属性】或【爱好】的具体地点（如实验室、模型室、暗房等）。
      2. 必须包含一个具有心理推拉感（Push & Pull）的微小冲突或动作交锋。
      3. 必须包含一句符合双方 MBTI 逻辑的高智感对白。
      4. 描写要侧重光影、声音和微小的肢体语言（如指尖、眼神、咖啡杯的水渍）。",

  "frictionAndEvolution": "（约150字）基于得分最低的维度（或MBTI的天然盲区）推演出的致命摩擦点。
      要求：精准预判在长期相处中，哪种特定场景会触发危机（例如A的系统化冷漠伤害了B的情感阈值），并给出基于双方认知逻辑的“降维化解方案”。",

  "exclusiveQuote": "（20字以内）结合双方专业意象与性格张力，极具诗意和宿命感的专属金句。"
}}

请基于以上设定和 JSON 格式，开始深度解析：
"""

# ============= 编造的同性测试数据（算法与管理的碰撞） =============
USER_A = {
    "nickname": "小齐",
    "gender": "男",
    "age": 24,
    "school": "深圳大学",
    "grade": "研二",
    "mbti": "INTP",
    "zodiac": "水瓶座",
    "interests": "深度学习,遥感影像,摄影,后摇",
    "major": "算法工程",
}

USER_B = {
    "nickname": "小林",
    "gender": "男",
    "age": 23,
    "school": "深圳大学",
    "grade": "研一",
    "mbti": "ENTJ",
    "zodiac": "天蝎座",
    "interests": "项目管理,徒步,咖啡,科幻",
    "major": "科技管理",
}

# 匹配分数（模拟）
MATCH_SCORES = {
    "totalScore": 82,
    "interestScore": 75,
    "mbtiScore": 90,
    "zodiacScore": 70,
    "baziScore": 78,
    "majorScore": 85,
}

# ============= Prompt 模板 =============
SYSTEM_PROMPT = """你是 Campus Love 的「首席AI潜意识与羁绊分析师」。你精通荣格心理学、MBTI 认知功能、以及复杂人际关系（如知己、灵魂伴侣、宿命对手）的深度解构。

你的分析准则：
1. 超越世俗定义：当前面对的是同性用户。你的分析需打破单纯的“恋爱”或“交友”框架，将其升华为一种“灵魂共振”、“镜像互补”或“双星伴月”的极致羁绊。让这段关系在绝佳的挚友与深沉的爱意之间自由流淌。
2. 绝对的数据锚定：你的每一次感性推论，都必须精准溯源到二人的客观硬指标（MBTI功能、专业壁垒、兴趣交集）。
3. 电影级场景叙事：注重同性之间特有的张力——可能是无言的默契、智性的交锋、或是背靠背的信任。细节要精确到光影、微动作和专业元素的碰撞。
4. 语言质感：兼具学术的严谨与文学的锋利，克制而深情，充满“智性恋”的审美。

⚠️ 严格格式要求：
- 必须且只能输出一个合法的 JSON 对象。
- 绝对不要输出任何 Markdown 标记（禁止使用 ```json 包裹），禁止输出任何解释性前言或后语。
- JSON 中的所有字符串必须使用英文双引号包裹。
"""

USER_PROMPT_TEMPLATE = """--------------------------------
【用户A客观数据】
- 基础画像：{nicknameA} | {genderA} | {ageA}岁 | {schoolA}{gradeA} | {majorA}专业
- 心理与星象基建：MBTI={mbtiA} | 星座={zodiacA}
- 偏好标签：[{interestsA}]

【用户B客观数据】
- 基础画像：{nicknameB} | {genderB} | {ageB}岁 | {schoolB}{gradeB} | {majorB}专业
- 心理与星象基建：MBTI={mbtiB} | 星座={zodiacB}
- 偏好标签：[{interestsB}]

【系统匹配硬指标】
- 综合羁绊评分：{totalScore}/100
- 细分维度得分：兴趣={interestScore} | MBTI={mbtiScore} | 星座={zodiacScore} | 八字={baziScore} | 专业跨度={majorScore}
--------------------------------

【输出结构与深度指令】
请基于上述数据，深度推演两人的关系网络，并严格按照以下 JSON 字段要求输出：

{{
  "_dataReasoning": "（必填草稿，不对用户展示）在此处用200字进行硬核逻辑推演。列出支持他们产生深层羁绊的具体数据点（例如：一方的专业逻辑如何补足另一方的思维盲区？同性之间的高MBTI得分意味着怎样的精神镜像？低分维度会带来怎样的良性博弈或现实阵痛？）",

  "yuanFenIndex": "（15字以内）基于数据推演出的核心精神羁绊词，如‘双子星式的智性交锋’或‘同频共振的深海锚点’。",

  "dataBackedOverview": "（约120字）将冷冰冰的【系统匹配硬指标】翻译成兼具理性和宿命感的综合判词。需指明这段高分同性羁绊的底色——是能在顶峰相见的战友，还是能抚平彼此精神褶皱的灵魂伴侣。隐晦地说明最高分与最低分维度的现实意义。",

  "cognitiveArchitecture": "（约400字）MBTI底层逻辑与专业属性的深度融合分析。
      要求：
      1. 使用具体的认知功能术语（如Ti, Te, Ni, Ne等）剖析两人的思维链路。
      2. 结合两人的【专业属性】，分析他们在面对现实挑战或精神探索时，如何形成“镜像互补”或“协同放电”。例如：一个在构建底层算法，一个在统筹全局框架。
      3. 说明对方如何成为了自己内心理想化或被压抑部分的投射，实现 1+1>2 的灵魂补全。",

  "interestResonance": "（约150字）基于具体的【偏好标签】和【兴趣得分】，深挖同性之间特有的互动模式。分析那些看似不相关的兴趣（如一方的遥感影像与另一方的徒步探险）如何通过内在的逻辑或审美统一起来，形成精神上的闭环。",

  "cinematicScene": "（约250字）高颗粒度、强张力的校园/职场微电影预演。
      要素要求：
      1. 场景需触发两人的【专业属性】（如实验室的白板前、深夜的会议室或天台）。
      2. 展现同性之间特有的推拉感：可以是理念的激烈碰撞后相视一笑，也可以是极度疲惫时无需多言的默契递递咖啡。
      3. 包含一句符合双方智力水平与 MBTI 逻辑的高智感对白。
      4. 描写侧重微小的肢体语言、光影切割和沉默中的情绪流转。",

  "frictionAndEvolution": "（约150字）基于得分最低的维度（或MBTI的同极相斥/盲区）推演出的致命摩擦点。精准预判在长期并肩前行中可能爆发的危机（如掌控欲与自由意志的冲突），并给出高维度的破局建议。",

  "exclusiveQuote": "（20字以内）结合双方专业意象与性格张力，极具宿命感与力量感的专属金句（超越性别的狭隘，直击灵魂）。"
}}

请严格按照上述 JSON 格式，为当前两位用户输出深度解析报告：
"""
def build_user_prompt():
    return USER_PROMPT_TEMPLATE.format(
        nicknameA=USER_A["nickname"],
        genderA=USER_A["gender"],
        ageA=USER_A["age"],
        schoolA=USER_A["school"],
        gradeA=USER_A["grade"],
        mbtiA=USER_A["mbti"],
        zodiacA=USER_A["zodiac"],
        interestsA=USER_A["interests"],
        majorA=USER_A["major"],
        nicknameB=USER_B["nickname"],
        genderB=USER_B["gender"],
        ageB=USER_B["age"],
        schoolB=USER_B["school"],
        gradeB=USER_B["grade"],
        mbtiB=USER_B["mbti"],
        zodiacB=USER_B["zodiac"],
        interestsB=USER_B["interests"],
        majorB=USER_B["major"],
        totalScore=MATCH_SCORES["totalScore"],
        interestScore=MATCH_SCORES["interestScore"],
        mbtiScore=MATCH_SCORES["mbtiScore"],
        zodiacScore=MATCH_SCORES["zodiacScore"],
        baziScore=MATCH_SCORES["baziScore"],
        majorScore=MATCH_SCORES["majorScore"],
    )


def main():
    if not API_KEY:
        print("请设置环境变量 AI_API_KEY 或 OPENAI_API_KEY")
        exit(1)

    client = OpenAI(api_key=API_KEY, base_url=BASE_URL)
    user_prompt = build_user_prompt()

    print("=" * 60)
    print("调用 AI 中...")
    print("=" * 60)

    resp = client.chat.completions.create(
        model=MODEL,
        max_tokens=4096,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
    )

    content = resp.choices[0].message.content.strip()
    # 清理可能的 markdown 代码块
    if content.startswith("```"):
        content = content.replace("```json", "").replace("```", "").strip()

    print("\n【原始输出】")
    print(content)

    try:
        parsed = json.loads(content)
        print("\n【解析后 JSON】")
        print(json.dumps(parsed, ensure_ascii=False, indent=2))
    except json.JSONDecodeError as e:
        print(f"\nJSON 解析失败: {e}")


if __name__ == "__main__":
    main()