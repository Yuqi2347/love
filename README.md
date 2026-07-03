# <div align="center">Campal</div>

<div align="center">
  <img src="campus-love-frontend/public/logo.png" alt="Campal Logo" width="96" />
</div>

<div align="center">
  <h3>用虚拟 AI 推动真实社交</h3>
  <p>一个围绕校园场景设计的 AI 社交产品原型，重点不是让人更久停留在线上，而是帮助人更自然地走向真实连接。</p>
</div>

<div align="center">
  <a href="http://campal.social"><img alt="在线预览" src="https://img.shields.io/badge/在线预览-campal.social-EF6A5B?style=for-the-badge"></a>
  <a href="docs/vision.md"><img alt="项目愿景" src="https://img.shields.io/badge/项目愿景-中文文档-1F2937?style=for-the-badge"></a>
  <a href="README.en.md"><img alt="English README" src="https://img.shields.io/badge/README-English-4B5563?style=for-the-badge"></a>
</div>

<br />

> Campal 想解决的不是“线上陪伴”，而是“真实连接的起步门槛”。

很多学生不是不想社交，而是卡在第一步：

- 想出去玩，却找不到能一起去的人
- 对某个人有好感，却不知道怎么开始第一句话
- 有表达欲、有情绪、有观点，却缺少一个能形成共鸣的公共空间

我们希望 AI 在这里扮演的角色，不是替代关系，而是帮助关系发生。

完整愿景见 [docs/vision.md](docs/vision.md)。

## 在线预览

<table>
  <tr>
    <td width="64%">
      <strong>公开预览地址</strong><br />
      <a href="http://campal.social">http://campal.social</a><br /><br />
      说明：项目线上站点可供外部预览，但由于校园身份与注册限制，非目标用户通常无法完整进入核心功能流。<br />
      因此本 README 同时提供产品理念、功能动线和界面展示，帮助你在不登录的情况下理解项目。
    </td>
    <td width="36%">
      <strong>项目定位</strong><br />
      AI 驱动的校园社交原型<br />
      重点展示产品想法、关系机制与功能设计，而不是单纯技术堆叠。
    </td>
  </tr>
</table>

## 我们想做什么

<table>
  <tr>
    <td width="33%">
      <strong>不是沉迷线上</strong><br />
      我们不想把 Campal 做成一个让人无限刷、无限聊的社交容器。
    </td>
    <td width="33%">
      <strong>而是推动起步</strong><br />
      我们更关心怎么把“想认识”“想靠近”“想一起做点什么”真正推进到现实里。
    </td>
    <td width="33%">
      <strong>虚拟是桥</strong><br />
      AI、内容流、画像、匹配都只是桥，真实连接才是目的地。
    </td>
  </tr>
</table>

## 产品界面预览

<p>由于线上完整体验对身份有限制，这里先用公开图片和界面说明展示产品的主要表达方式。</p>

<table>
  <tr>
    <td width="50%" align="center">
      <img src="campus-love-frontend/public/images/welcome/szu-1.webp" alt="Campal 校园欢迎页" width="100%" />
      <br />
      <strong>校园欢迎页</strong><br />
      用学校氛围、真实场景和轻叙事感，把产品从“工具”拉回“校园社交现场”。
    </td>
    <td width="50%" align="center">
      <img src="campus-love-frontend/public/images/welcome/szu-2.webp" alt="Campal 校园氛围页" width="100%" />
      <br />
      <strong>关系发生的场景感</strong><br />
      Campal 希望用户进入时先感受到“校园里真实的人与场”，而不是冷冰冰的功能入口。
    </td>
  </tr>
</table>

<table>
  <tr>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/原图.jpg" alt="原图" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/动漫风.jpg" alt="动漫风" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/赛博风.jpg" alt="赛博风" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/柯达胶片.jpg" alt="柯达胶片" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/油画风.jpg" alt="油画风" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/素描风.jpg" alt="素描风" width="100%" /></td>
    <td width="14%" align="center"><img src="campus-love-frontend/public/images/head_portrait/高管工作照.jpg" alt="高管工作照" width="100%" /></td>
  </tr>
</table>

<p align="center">
  <strong>AI 头像工作室示例</strong><br />
  这类能力不是为了制造虚拟替身，而是帮助用户更轻松地完成表达、展示风格，并进入社交语境。
</p>

## 三条核心产品线

<table>
  <tr>
    <td width="33%">
      <h3>1. 邀约</h3>
      <strong>解决的问题：</strong>我想做点什么，但不知道找谁一起。<br /><br />
      <strong>功能意图：</strong>把“没有现成圈子”这件事，变成一个可以被快速回应的社交入口。<br /><br />
      <strong>界面重点：</strong>活动卡片、人数状态、时间地点、参与确认、群聊承接。
    </td>
    <td width="33%">
      <h3>2. 心动时刻</h3>
      <strong>解决的问题：</strong>我注意到了某个人，但不知道怎么靠近。<br /><br />
      <strong>功能意图：</strong>通过 AI 辅助匹配、结果解释与约会准备，降低第一次互动的尴尬和压力。<br /><br />
      <strong>界面重点：</strong>报名、匹配结果、关系解读、约会准备、结果确认。
    </td>
    <td width="33%">
      <h3>3. 发现</h3>
      <strong>解决的问题：</strong>缺少一个能被看见、被理解、形成共鸣的公共层。<br /><br />
      <strong>功能意图：</strong>让表达、动态、观点和情绪，先成为理解彼此的上下文。<br /><br />
      <strong>界面重点：</strong>动态流、内容互动、活动挂载、用户画像线索。
    </td>
  </tr>
</table>

## 产品动线

<div align="center">
  <code>表达</code>
  →
  <code>发现</code>
  →
  <code>起步</code>
  →
  <code>过渡</code>
  →
  <code>真实连接</code>
</div>

<br />

Campal 的核心不是单点功能，而是一条关系推进路径：

1. 用户先通过资料、动态、兴趣与行为留下可被理解的痕迹。
2. 用户看到原本不会遇见的人、内容和活动。
3. 通过邀约、匹配和引导式交互降低社交起步成本。
4. 借助 AI 把线上理解转成线下行动建议。
5. 最终目标是发生真实互动，而不是提升应用停留时长。

## 我们如何使用 AI

<table>
  <tr>
    <td width="25%"><strong>理解表达</strong><br />帮助理解用户的兴趣、表达方式与画像线索。</td>
    <td width="25%"><strong>降低门槛</strong><br />帮助用户更容易开始匹配、靠近和破冰。</td>
    <td width="25%"><strong>提供上下文</strong><br />让初次接触少一点尴尬，多一点理由与场景。</td>
    <td width="25%"><strong>推动线下</strong><br />让线上了解更自然地转向真实见面与互动。</td>
  </tr>
</table>

一句话概括：

**AI 的价值，不是让人更久停留在线上，而是让人更愿意走向彼此。**

## 这个仓库代表什么

这个仓库更适合被理解为一个**产品原型与开放展示项目**。

它想展示的是：

- 一个关于“AI 如何推动校园真实社交”的明确产品命题
- 围绕这个命题设计的一组功能机制与交互结构
- 一个已经落到后端、Web 与管理后台多个表面的可运行原型

## 仓库结构

```text
.
├── campus-love-backend/   # Spring Boot 后端
├── campus-love-frontend/  # Vue Web 前端
├── campus-love-admin/     # 管理后台
├── docs/                  # 对外公开文档
└── README.md
```

## 技术实现

项目当前包含：

- Spring Boot
- Vue 3
- TypeScript
- MySQL
- Redis
- Flyway
- WebSocket

技术是实现方式，不是这个项目最想强调的部分。  
Campal 更重要的价值，在于它如何把产品理念、关系机制和 AI 能力组织成一个完整原型。

## 本地运行

运行说明请分别查看：

- [campus-love-backend](campus-love-backend/)
- [campus-love-frontend/README.md](campus-love-frontend/README.md)

本地运行前请自行准备环境变量和第三方服务配置，不要使用任何真实生产密钥或个人凭证。
