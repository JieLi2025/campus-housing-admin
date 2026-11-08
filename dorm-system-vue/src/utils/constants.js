/**
 * 系统常量配置
 */

// 导入默认头像（Vite 项目使用 ?url 后缀）
import defaultAvatarUrl from '@/assets/images/default-avatar.svg?url'

// 默认头像路径
export const DEFAULT_AVATAR = defaultAvatarUrl

// 导出一个获取头像的工具函数
export function getAvatarUrl(avatar) {
  return avatar || DEFAULT_AVATAR
}
