<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreate">新建公告</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="220">
          <template #default="{ row }">
            <span class="muted">{{ row.validFrom }}</span>
            <span> ~ </span>
            <span class="muted">{{ row.validUntil }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="handlePublish(row.id)">发布</el-button>
            <el-button v-else link type="warning" @click="handleUnpublish(row.id)">下架</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="255" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="8000" show-word-limit placeholder="支持纯文本，换行保留" />
        </el-form-item>
        <el-form-item label="生效时间" prop="validFrom">
          <el-date-picker
            v-model="form.validFrom"
            type="datetime"
            placeholder="开始"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="截止时间" prop="validUntil">
          <el-date-picker
            v-model="form.validUntil"
            type="datetime"
            placeholder="结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!editingId" label=" ">
          <el-checkbox v-model="form.publish">保存后直接发布</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAdminAnnouncements,
  createAdminAnnouncement,
  updateAdminAnnouncement,
  publishAdminAnnouncement,
  unpublishAdminAnnouncement,
  deleteAdminAnnouncement,
  type SiteAnnouncementAdminItem,
} from '@/api/adminApi'

const loading = ref(false)
const tableData = ref<SiteAnnouncementAdminItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑公告' : '新建公告'))
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  content: '',
  validFrom: '' as string | null,
  validUntil: '' as string | null,
  publish: true,
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  validFrom: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  validUntil: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminAnnouncements({ page: page.value, size: size.value })
    const d = res.data.data
    tableData.value = d.records
    total.value = d.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SiteAnnouncementAdminItem) {
  editingId.value = row.id
  form.title = row.title
  form.content = row.content
  form.validFrom = row.validFrom.replace(' ', 'T').slice(0, 19)
  form.validUntil = row.validUntil.replace(' ', 'T').slice(0, 19)
  form.publish = false
  dialogVisible.value = true
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.validFrom = null
  form.validUntil = null
  form.publish = true
}

async function submit() {
  await formRef.value?.validate().catch(() => Promise.reject())
  if (form.validFrom && form.validUntil && form.validUntil < form.validFrom) {
    ElMessage.warning('截止时间不能早于生效时间')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAdminAnnouncement(editingId.value, {
        title: form.title,
        content: form.content,
        validFrom: form.validFrom!,
        validUntil: form.validUntil!,
      })
      ElMessage.success('已保存')
    } else {
      await createAdminAnnouncement({
        title: form.title,
        content: form.content,
        validFrom: form.validFrom!,
        validUntil: form.validUntil!,
        publish: form.publish,
      })
      ElMessage.success(form.publish ? '已创建并发布' : '已保存为草稿')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handlePublish(id: number) {
  await publishAdminAnnouncement(id)
  ElMessage.success('已发布')
  await load()
}

async function handleUnpublish(id: number) {
  await unpublishAdminAnnouncement(id)
  ElMessage.success('已下架为草稿')
  await load()
}

async function handleDelete(row: SiteAnnouncementAdminItem) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '删除确认', { type: 'warning' })
  await deleteAdminAnnouncement(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped lang="scss">
.page {
  max-width: 1200px;
}
.toolbar {
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.muted {
  color: $text-secondary;
  font-size: 13px;
}
</style>
