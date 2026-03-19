<template>
  <div class="moment-page">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-main">
          <el-input v-model="filters.weekTag" placeholder="活动周，例如 2026-W12" clearable class="field-week" />
          <el-select v-model="filters.pool" placeholder="匹配池" clearable class="field-short">
            <el-option label="MF 异性" value="MF" />
            <el-option label="FF 女生" value="FF" />
            <el-option label="MM 男生" value="MM" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="搜索结果ID / 用户ID / 昵称 / 缘分标题" clearable class="field-keyword" />
        </div>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="panel-header">
          <span>匹配结果中心</span>
          <span class="panel-meta">集中查看配对结果、双方确认进度与生成文案</span>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="id" label="结果ID" width="100" />
        <el-table-column prop="weekTag" label="周次" width="120" />
        <el-table-column label="池" width="120">
          <template #default="{ row }">{{ poolLabel(row.pool) }}</template>
        </el-table-column>
        <el-table-column label="A 用户" min-width="170">
          <template #default="{ row }">{{ row.nicknameA }} (#{{ row.userIdA }})</template>
        </el-table-column>
        <el-table-column label="B 用户" min-width="170">
          <template #default="{ row }">{{ row.nicknameB }} (#{{ row.userIdB }})</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="匹配分" width="100" />
        <el-table-column prop="yuanfenTitle" label="缘分标题" min-width="160" />
        <el-table-column label="确认进度" width="150">
          <template #default="{ row }">
            <el-tag :type="confirmTagType(row.confirmStatus)" effect="light">{{ row.confirmStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="detailLoading && activeId === row.id" @click="openDetail(row.id)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="footer-tip">共 {{ total }} 条匹配结果</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="prev, pager, next, total"
          :total="total"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" size="720px" destroy-on-close>
      <template #header>
        <div class="drawer-title">匹配结果详情</div>
      </template>

      <div v-if="detail" class="detail-body">
        <div class="detail-top">
          <el-card class="user-card">
            <template #header>{{ detail.userA.nickname || `用户 ${detail.userA.userId}` }}</template>
            <div class="user-lines">
              <span>ID：{{ detail.userA.userId || '--' }}</span>
              <span>{{ detail.userA.school || '--' }} / {{ detail.userA.major || '--' }}</span>
              <span>{{ detail.userA.grade || '--' }} / {{ detail.userA.mbti || '--' }}</span>
            </div>
          </el-card>
          <el-card class="user-card">
            <template #header>{{ detail.userB.nickname || `用户 ${detail.userB.userId}` }}</template>
            <div class="user-lines">
              <span>ID：{{ detail.userB.userId || '--' }}</span>
              <span>{{ detail.userB.school || '--' }} / {{ detail.userB.major || '--' }}</span>
              <span>{{ detail.userB.grade || '--' }} / {{ detail.userB.mbti || '--' }}</span>
            </div>
          </el-card>
        </div>

        <el-descriptions :column="2" border class="desc-card">
          <el-descriptions-item label="周次">{{ detail.weekTag }}</el-descriptions-item>
          <el-descriptions-item label="池">{{ poolLabel(detail.pool) }}</el-descriptions-item>
          <el-descriptions-item label="匹配分">{{ detail.totalScore }}</el-descriptions-item>
          <el-descriptions-item label="缘分标题">{{ detail.yuanfenTitle || '--' }}</el-descriptions-item>
          <el-descriptions-item label="确认状态">{{ detail.confirmStatus }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="A 选择">{{ choiceText(detail.choiceA) }}</el-descriptions-item>
          <el-descriptions-item label="B 选择">{{ choiceText(detail.choiceB) }}</el-descriptions-item>
        </el-descriptions>

        <el-card class="content-card" v-if="detail.insightCards.length">
          <template #header>洞察卡片</template>
          <div class="paragraph-list">
            <div v-for="(item, index) in detail.insightCards" :key="index" class="paragraph-item">{{ item }}</div>
          </div>
        </el-card>

        <el-card class="content-card" v-if="detail.goldenSentence || detail.aboutUserA || detail.aboutUserB">
          <template #header>结果文案</template>
          <div class="paragraph-list">
            <div v-if="detail.goldenSentence" class="paragraph-item">金句：{{ detail.goldenSentence }}</div>
            <div v-if="detail.aboutUserA" class="paragraph-item">关于 A：{{ detail.aboutUserA }}</div>
            <div v-if="detail.aboutUserB" class="paragraph-item">关于 B：{{ detail.aboutUserB }}</div>
          </div>
        </el-card>

        <el-card class="content-card" v-if="detail.datePrepA || detail.datePrepB">
          <template #header>双方约会准备</template>
          <div class="prep-grid">
            <section class="prep-panel">
              <div class="prep-panel-title">{{ detail.userA.nickname || `用户 ${detail.userA.userId}` }} 视角</div>
              <template v-if="hasDatePrep(detail.datePrepA)">
                <div class="prep-item">
                  <div class="prep-label">场景类型</div>
                  <div class="prep-text">{{ detail.datePrepA?.dateSceneType || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">约会建议</div>
                  <div class="prep-text">{{ detail.datePrepA?.dateSuggestion || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">破冰话题</div>
                  <div class="topic-list" v-if="prepTopics(detail.datePrepA).length">
                    <div v-for="(topic, index) in prepTopics(detail.datePrepA)" :key="`${topic.title}-${index}`" class="topic-item">
                      <div class="topic-title">{{ topic.title }}</div>
                      <div class="topic-opener">{{ topic.opener }}</div>
                    </div>
                  </div>
                  <div v-else class="prep-text">--</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">小惊喜</div>
                  <div class="prep-text">{{ detail.datePrepA?.surpriseIdea || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">穿搭建议</div>
                  <div class="prep-text">{{ detail.datePrepA?.outfitAdvice || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">心态建议</div>
                  <div class="prep-text">{{ detail.datePrepA?.mindsetAdvice || '--' }}</div>
                </div>
              </template>
              <div v-else class="prep-empty">暂无约会准备内容</div>
            </section>

            <section class="prep-panel">
              <div class="prep-panel-title">{{ detail.userB.nickname || `用户 ${detail.userB.userId}` }} 视角</div>
              <template v-if="hasDatePrep(detail.datePrepB)">
                <div class="prep-item">
                  <div class="prep-label">场景类型</div>
                  <div class="prep-text">{{ detail.datePrepB?.dateSceneType || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">约会建议</div>
                  <div class="prep-text">{{ detail.datePrepB?.dateSuggestion || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">破冰话题</div>
                  <div class="topic-list" v-if="prepTopics(detail.datePrepB).length">
                    <div v-for="(topic, index) in prepTopics(detail.datePrepB)" :key="`${topic.title}-${index}`" class="topic-item">
                      <div class="topic-title">{{ topic.title }}</div>
                      <div class="topic-opener">{{ topic.opener }}</div>
                    </div>
                  </div>
                  <div v-else class="prep-text">--</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">小惊喜</div>
                  <div class="prep-text">{{ detail.datePrepB?.surpriseIdea || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">穿搭建议</div>
                  <div class="prep-text">{{ detail.datePrepB?.outfitAdvice || '--' }}</div>
                </div>
                <div class="prep-item">
                  <div class="prep-label">心态建议</div>
                  <div class="prep-text">{{ detail.datePrepB?.mindsetAdvice || '--' }}</div>
                </div>
              </template>
              <div v-else class="prep-empty">暂无约会准备内容</div>
            </section>
          </div>
        </el-card>

        <el-card class="content-card">
          <template #header>结构化数据</template>
          <div class="json-grid">
            <div>
              <div class="json-title">分数明细</div>
              <pre>{{ prettyJson(detail.scoreDetail) }}</pre>
            </div>
            <div>
              <div class="json-title">维度标签</div>
              <pre>{{ prettyJson(detail.dimensionLabels) }}</pre>
            </div>
            <div>
              <div class="json-title">互补模式</div>
              <pre>{{ prettyJson(detail.complementaryModes) }}</pre>
            </div>
            <div>
              <div class="json-title">约会准备缓存</div>
              <pre>{{ prettyJson(detail.datePrepJson) }}</pre>
            </div>
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMomentAdminResultDetail, getMomentAdminResults } from '@/api/adminApi'
import type { MomentDatePrepInfo, MomentMatchResultDetail, MomentMatchResultItem } from '@/api/adminApi'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const activeId = ref<number | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<MomentMatchResultItem[]>([])
const detail = ref<MomentMatchResultDetail | null>(null)
const filters = reactive({
  weekTag: '',
  pool: '',
  keyword: '',
})

function poolLabel(pool: string) {
  return { MF: 'MF 异性', FF: 'FF 女生', MM: 'MM 男生' }[pool] || pool
}

function formatDateTime(value?: string | null) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function confirmTagType(status: string) {
  if (status.includes('双向')) return 'success'
  if (status.includes('关注')) return 'warning'
  if (status.includes('等待')) return 'info'
  return ''
}

function choiceText(choice?: string | null) {
  return { YUE: '约一下', GUANZHU: '关注一下' }[choice || ''] || '--'
}

function prettyJson(value?: string | null) {
  if (!value) return '--'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function prepTopics(value?: MomentDatePrepInfo | null) {
  return value?.iceBreakTopics || []
}

function hasDatePrep(value?: MomentDatePrepInfo | null) {
  return Boolean(
    value &&
      (value.dateSceneType ||
        value.dateSuggestion ||
        value.surpriseIdea ||
        value.outfitAdvice ||
        value.mindsetAdvice ||
        prepTopics(value).length),
  )
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMomentAdminResults({
      page: page.value,
      size: size.value,
      weekTag: filters.weekTag || undefined,
      pool: filters.pool || undefined,
      keyword: filters.keyword || undefined,
    })
    const data = res.data.data
    rows.value = data.records || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载匹配结果失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  activeId.value = id
  detailLoading.value = true
  try {
    const res = await getMomentAdminResultDetail(id)
    detail.value = res.data.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载结果详情失败')
  } finally {
    detailLoading.value = false
    activeId.value = null
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.weekTag = ''
  filters.pool = ''
  filters.keyword = ''
  handleSearch()
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.moment-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card,
.table-card {
  border-radius: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-main {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.field-week {
  width: 180px;
}

.field-short {
  width: 140px;
}

.field-keyword {
  width: 280px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-meta,
.footer-tip {
  color: #6b7280;
  font-size: 13px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-top {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.user-card,
.content-card,
.desc-card {
  border-radius: 18px;
}

.user-lines,
.paragraph-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.paragraph-item {
  line-height: 1.7;
  color: #374151;
}

.json-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.prep-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.prep-panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fcfdff;
}

.prep-panel-title {
  margin-bottom: 14px;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.prep-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.prep-item + .prep-item {
  margin-top: 14px;
}

.prep-label {
  color: #6b7280;
  font-size: 12px;
}

.prep-text,
.topic-opener,
.prep-empty {
  color: #374151;
  line-height: 1.75;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.topic-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.topic-title {
  margin-bottom: 4px;
  color: #111827;
  font-weight: 600;
}

.json-title {
  margin-bottom: 8px;
  color: #111827;
  font-weight: 600;
}

pre {
  margin: 0;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 900px) {
  .field-week,
  .field-short,
  .field-keyword {
    width: 100%;
  }

  .table-footer,
  .detail-top,
  .prep-grid,
  .json-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
