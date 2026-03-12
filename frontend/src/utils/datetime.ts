/**
 * 日期时间工具函数：封装前后端 runAt / 时间字段在本地时区（如北京时区）与 UTC 之间的转换逻辑。
 *
 * 约定说明：
 * 1. 后端 scheduler / delivery 按 UTC 存储与比较 LocalDateTime（字符串格式统一为 yyyy-MM-dd HH:mm:ss）。
 * 2. 前端界面希望展示和录入的都是「本地时间」（例如北京时间 UTC+8）。
 * 3. 因此：
 *    - 往后端提交 runAt 时，需要把「本地时间」转换成对应的 UTC 字符串；
 *    - 从后端返回的 UTC 字符串，需要转换成本地时间再展示给用户。
 *
 * 注意：本文件只做「字符串 <-> Date 对象」的时区换算与格式化，不负责业务含义。
 */

/**
 * 将本地时间的 Date 对象转换为 UTC 字符串，格式为 yyyy-MM-dd HH:mm:ss。
 *
 * 适用场景：
 * - 前端已经通过 new Date() 或日期选择器得到一个「本地时间」的 Date（例如北京时区点击“立即发送”的当前时间 + 1 分钟），
 *   需要按 UTC 传给后端 scheduler 作为 runAt。
 *
 * 实现说明：
 * - Date 内部存储的是一个绝对时间点，通过 getUTC* 系列方法即可拿到该时间点在 UTC 下的年月日时分秒。
 */
export function formatUtcRunAtFromLocalDate(date: Date): string {
  const y = date.getUTCFullYear()
  const m = String(date.getUTCMonth() + 1).padStart(2, '0')
  const d = String(date.getUTCDate()).padStart(2, '0')
  const h = String(date.getUTCHours()).padStart(2, '0')
  const min = String(date.getUTCMinutes()).padStart(2, '0')
  const s = String(date.getUTCSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

/**
 * 将「本地时间字符串」转换为对应的 UTC 字符串，格式保持为 yyyy-MM-dd HH:mm:ss。
 *
 * 输入约定：
 * - localString 格式为 yyyy-MM-dd HH:mm:ss，含义为「用户所在时区的本地时间」。
 *
 * 实现步骤：
 * 1. 按本地时间构造 Date（使用 new Date(year, monthIndex, ...)）；
 * 2. 调用 formatUtcRunAtFromLocalDate，得到该时间在 UTC 下对应的字符串。
 *
 * 典型用法：
 * - 用户在「创建计划」弹窗中手工输入一次性执行时间（本地），提交前调用本方法转换为 UTC 传给后端。
 */
export function convertLocalRunAtStringToUtc(localString: string): string {
  const trimmed = localString?.trim()
  if (!trimmed) {
    return localString
  }

  const [datePart, timePart] = trimmed.split(' ')
  if (!datePart || !timePart) {
    // 非预期格式时直接回传原值，避免在异常情况下破坏用户输入
    return localString
  }

  const [yStr, mStr, dStr] = datePart.split('-')
  const [hStr, minStr, sStr] = timePart.split(':')
  const year = Number(yStr)
  const month = Number(mStr)
  const day = Number(dStr)
  const hour = Number(hStr)
  const minute = Number(minStr)
  const second = Number(sStr)

  // 使用本地时区构造 Date，含义为「本地的 yyyy-MM-dd HH:mm:ss」
  const localDate = new Date(
    Number.isFinite(year) ? year : 1970,
    Number.isFinite(month) ? month - 1 : 0,
    Number.isFinite(day) ? day : 1,
    Number.isFinite(hour) ? hour : 0,
    Number.isFinite(minute) ? minute : 0,
    Number.isFinite(second) ? second : 0,
  )

  if (Number.isNaN(localDate.getTime())) {
    return localString
  }

  return formatUtcRunAtFromLocalDate(localDate)
}

/**
 * 将后端返回的 UTC 时间字符串转换成本地时间字符串，格式仍为 yyyy-MM-dd HH:mm:ss。
 *
 * 输入约定：
 * - utcString 为后端序列化的 LocalDateTime 字符串（无时区信息），语义上为 UTC 时间点。
 *   例如 "2026-03-11 02:18:00" 表示 UTC 时间。
 *
 * 实现步骤：
 * 1. 在字符串末尾补上 'Z'，显式告知 Date 构造函数这是一个 UTC 时间；
 * 2. 通过 Date 的本地时间访问方法（getFullYear / getHours 等）格式化为本地时间。
 *
 * 典型用法：
 * - scheduler / schedule_job 表中的 run_at、create_time、update_time 等字段展示给用户时，
 *   先调用本方法转换，让界面显示为北京时间等本地时区，而非 UTC。
 */
export function convertUtcStringToLocalDisplay(utcString?: string): string | undefined {
  const trimmed = utcString?.trim()
  if (!trimmed) {
    return utcString
  }

  // 将 "yyyy-MM-dd HH:mm:ss" 规范化为 "yyyy-MM-ddTHH:mm:ssZ" 形式，确保按 UTC 解析
  const normalized = trimmed.replace(' ', 'T')
  const withZone = normalized.endsWith('Z') ? normalized : `${normalized}Z`
  const date = new Date(withZone)

  if (Number.isNaN(date.getTime())) {
    // 解析失败时直接返回原始字符串，避免界面完全丢失信息
    return utcString
  }

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

