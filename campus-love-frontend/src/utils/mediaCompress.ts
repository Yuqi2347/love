/**
 * 上传前压缩图片，减轻带宽；服务端仍会二次压缩。
 * GIF 不动；PNG/JPEG/WebP 等转为 JPEG。
 */
export async function compressImageFile(
  file: File,
  maxEdge = 1920,
  quality = 0.82,
): Promise<File> {
  if (!file.type.startsWith('image/')) return file
  if (file.type === 'image/gif' || file.type === 'image/svg+xml') return file

  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      let w = img.naturalWidth || img.width
      let h = img.naturalHeight || img.height
      if (w <= 0 || h <= 0) {
        resolve(file)
        return
      }
      if (w > maxEdge || h > maxEdge) {
        const s = Math.min(maxEdge / w, maxEdge / h)
        w = Math.round(w * s)
        h = Math.round(h * s)
      }
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve(file)
        return
      }
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, w, h)
      ctx.drawImage(img, 0, 0, w, h)
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            resolve(file)
            return
          }
          const name = file.name.replace(/\.[^.]+$/, '') + '.jpg'
          resolve(new File([blob], name, { type: 'image/jpeg', lastModified: Date.now() }))
        },
        'image/jpeg',
        quality,
      )
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(file)
    }
    img.src = url
  })
}

export async function compressAvatarFile(file: File): Promise<File> {
  return compressImageFile(file, 512, 0.86)
}

/** 个人主页背景图：略宽于动态图，仍控制体积 */
export async function compressCoverFile(file: File): Promise<File> {
  return compressImageFile(file, 2560, 0.82)
}
