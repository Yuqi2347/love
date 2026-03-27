<template>
  <div class="legal-page">
    <div class="legal-shell">
      <section class="legal-hero">
        <p class="legal-kicker">LEGAL</p>
        <h1 class="legal-title">{{ doc.title }}</h1>
        <p class="legal-subtitle">{{ doc.subtitle }}</p>
        <span class="legal-date">更新日期：{{ doc.updatedAt }}</span>
      </section>

      <section v-for="section in doc.sections" :key="section.heading" class="legal-section">
        <h2 class="section-heading">{{ section.heading }}</h2>
        <p v-for="(item, idx) in section.items" :key="item" class="section-item">{{ idx + 1 }}. {{ item }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

type DocSection = { heading: string; items: string[] }
type DocDef = { title: string; subtitle: string; updatedAt: string; sections: DocSection[] }

const DOC_MAP: Record<string, DocDef> = {
  'privacy-policy': {
    title: '隐私政策',
    updatedAt: '2026-03-25',
    subtitle: '我们以最小必要原则处理个人信息，并提供查询、更正、删除等权利保障。',
    sections: [
      {
        heading: '1. 收集范围',
        items: [
          '账号信息：邮箱、昵称、头像、学校等注册资料。',
          '资料信息：MBTI、兴趣标签、个人介绍与问卷填写内容。',
          '互动信息：发帖、评论、点赞、邀约、消息记录等你主动产生的数据。',
        ],
      },
      {
        heading: '2. 使用目的',
        items: [
          '用于身份认证、账户安全与风控。',
          '用于社交功能展示、内容推荐与服务优化。',
          '用于处理争议、违规审查与平台治理。',
        ],
      },
      {
        heading: '3. 你的权利',
        items: [
          '你可在设置中更新资料、调整隐私选项。',
          '你可申请更正、删除或导出个人数据。',
          '你可申请解除微信绑定并改用邮箱登录。',
        ],
      },
    ],
  },
  'user-agreement': {
    title: '用户协议',
    updatedAt: '2026-03-25',
    subtitle: '使用平台服务即表示你同意遵守社区规范与账号使用规则。',
    sections: [
      {
        heading: '1. 账号规则',
        items: [
          '你应提供真实、合法、可管理的注册信息。',
          '你需妥善保管账号密码与登录凭证。',
          '账号不得出借、转让或用于违法活动。',
        ],
      },
      {
        heading: '2. 内容规范',
        items: [
          '禁止发布违法、侵权、辱骂、骚扰、诈骗或恶意营销内容。',
          '禁止冒充他人或通过技术手段破坏平台秩序。',
          '平台有权对违规内容采取删除、限制或封禁措施。',
        ],
      },
      {
        heading: '3. 服务声明',
        items: [
          '我们会持续优化服务稳定性与可用性，但不承诺绝对无中断。',
          '因网络、设备、第三方服务或不可抗力导致的问题，平台将尽力修复。',
        ],
      },
    ],
  },
  'personal-info-list': {
    title: '个人信息收集清单',
    updatedAt: '2026-03-25',
    subtitle: '以下说明不同功能会收集哪些信息，以及对应用途。',
    sections: [
      {
        heading: '1. 账号与安全',
        items: [
          '邮箱、验证码、密码：用于注册登录、找回密码与安全验证。',
          '登录状态信息：用于维持会话与防止未授权访问。',
        ],
      },
      {
        heading: '2. 资料与匹配',
        items: [
          '学校、专业、年级、MBTI、兴趣：用于资料展示与推荐排序。',
          '问卷数据：用于画像分析与匹配模型计算。',
        ],
      },
      {
        heading: '3. 互动与通知',
        items: [
          '帖子与评论内容：用于动态展示、社区互动和审核。',
          '聊天与邀约数据：用于实现社交沟通与活动协作。',
        ],
      },
    ],
  },
  'third-party-sharing': {
    title: '第三方信息共享清单',
    updatedAt: '2026-03-25',
    subtitle: '仅在提供必要能力时与第三方共享最小范围信息，不进行个人信息售卖。',
    sections: [
      {
        heading: '1. 微信小程序能力',
        items: [
          '使用 wx.login 临时凭证换取 openid，仅用于绑定和快捷登录。',
          '调用系统相册/相机仅在你主动上传图片时触发。',
        ],
      },
      {
        heading: '2. 基础设施服务',
        items: [
          '对象存储与网络服务用于承载图片、接口与消息能力。',
          '第三方仅能在约定范围内处理必要数据。',
        ],
      },
      {
        heading: '3. 共享原则',
        items: [
          '最小必要：按功能粒度限制字段与时长。',
          '安全可控：通过访问控制、审计与协议约束第三方。',
        ],
      },
    ],
  },
  'feature-description': {
    title: '功能说明',
    updatedAt: '2026-03-25',
    subtitle: '当前版本主要功能及开放状态说明。',
    sections: [
      {
        heading: '1. 共鸣',
        items: [
          '用于浏览校园动态、点赞、评论与互动。',
          '发帖功能当前仅对管理员开放。',
        ],
      },
      {
        heading: '2. 心动与同行',
        items: [
          '心动支持报名、匹配进度与结果查看。',
          '同行支持发起邀约、加入邀约与公开广场浏览。',
        ],
      },
      {
        heading: '3. 个人主页',
        items: [
          '支持编辑资料、设置背景、AI 头像工作室。',
          '支持绑定微信账号，后续可直接微信快捷登录。',
        ],
      },
    ],
  },
}

const route = useRoute()

const doc = computed((): DocDef => {
  const key = String(route.params.docType || 'privacy-policy')
  return (DOC_MAP[key] ?? DOC_MAP['privacy-policy']) as DocDef
})
</script>

<style lang="scss" scoped>
.legal-page {
  min-height: 100vh;
  padding: 28px 16px 60px;
  background:
    radial-gradient(circle at 12% 0%, rgba(243, 180, 199, 0.16), transparent 30%),
    radial-gradient(circle at 88% 6%, rgba(255, 227, 236, 0.2), transparent 24%),
    linear-gradient(180deg, #fff8fb 0%, #fffafd 100%);
}

.legal-shell {
  width: min(860px, 100%);
  margin: 0 auto;
}

.legal-hero {
  padding: 28px 26px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(240, 216, 226, 0.9);
  box-shadow: 0 16px 36px rgba(215, 127, 162, 0.08);
}

.legal-kicker {
  margin: 0;
  color: #b76587;
  font-size: 12px;
  letter-spacing: 1.8px;
  font-weight: 700;
}

.legal-title {
  margin: 10px 0 0;
  font-size: clamp(28px, 4vw, 36px);
  color: #3f2d35;
}

.legal-subtitle {
  margin: 12px 0 0;
  color: #6f5a63;
  line-height: 1.75;
}

.legal-date {
  margin-top: 14px;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #b76587;
  background: rgba(215, 127, 162, 0.1);
  border: 1px solid rgba(215, 127, 162, 0.24);
}

.legal-section {
  margin-top: 16px;
  padding: 22px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(240, 216, 226, 0.82);
}

.section-heading {
  margin: 0;
  font-size: 19px;
  color: #3f2d35;
}

.section-item {
  margin: 12px 0 0;
  line-height: 1.8;
  color: #6f5a63;
}
</style>
