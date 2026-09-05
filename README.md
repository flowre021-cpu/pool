# Pool

个人规划 Android App：**课表 + 事务**（Task / Opportunity / Event / Reminder），面向大学日常使用。

## 下载

[下载最新版本 APK](https://github.com/flowre021-cpu/pool/releases/latest)

下载 Release 页面中的 `Pool-v1.0.0.apk` 后安装。首次从浏览器安装 APK 时，Android
可能会要求允许该浏览器“安装未知应用”。

## 功能概览

### 主页

- **七天事务**：24h / 节次时间轴，1 / 3 / 7 天视图，morph 列宽，伪无限翻页，当前时间绿线
- **14 天时间轴**：14 天纵向轴，Task / Opportunity / Reminder 标记与日详情
- 可在「外观与偏好」中设置默认主页 Tab

### 课表

- 学期管理、课程编辑、课表网格（重叠课程并排显示）
- 北航 14 节作息，可逐节修改上下课时间
- 课程色卡（默认 20 色 + 自定义色卡）
- **北航教务一键导入**：WebView 登录 → 选择学期 → OkHttp 抓取 → 预览 → 写入 Room（仅覆盖该学期）

### 事务与任务

- Task / Opportunity / Event / Reminder 四类事务
- 列表长按多选批量删除

### 桌面 Widget（Glance）

- **今日安排**：活跃学期课程 + 今日 Event
- **待办 DDL**：未完成且有 deadline 的 Task
- **今日提醒**：今日 Reminder occurrence
- Widget 主题跟随 App 主题色；支持桌面一键完成

### 外观

- 7 套主题色（绿 / 蓝 / 黄 / 粉 / 橙 / 紫 / 灰）
- 两套应用图标（深池 / 清叶），通过 `activity-alias` 切换

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity + Navigation Compose + ViewModel + StateFlow |
| 存储 | Room v10 |
| Widget | Glance 1.2.0-rc01 |
| 后台 | WorkManager |
| 网络 | OkHttp（教务课表抓取，Cookie 来自 WebView，不持久化账号密码） |

## 构建

需要 Android SDK（`compileSdk 37`，`minSdk 26`）。

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug --no-configuration-cache
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

本地需创建 `local.properties`（已被 git 忽略），例如：

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

## 教务导入说明

路径：**课表设置 → 北航教务课表导入**

首次进入空课表时，也可以直接点击页面中央的“从北航教务导入”。

1. 在内置网页中登录北航本研教育管理系统
2. 选择「年份 + 春 / 夏 / 秋」学期
3. 点击“开始”，检查预览后确认导入

如果不使用教务导入，可在空课表页选择“手动添加课程”。

| 学期 | termCode 示例 | 抓取模式 |
|------|----------------|----------|
| 春季 | `2025-2026-2` | 按周 |
| 夏季 | `2025-2026-3` | 按周 |
| 秋季 | `2026-2027-1` | 按周 |

导入后自动创建 / 更新 Pool 学期并设为活跃学期；**不会存储** Cookie 或密码。

## 项目结构（要点）

```
app/src/main/java/com/example/pool/
├── ui/home/          # 双 Tab 主页、外观偏好
├── ui/course/        # 课表、导入、色卡
├── ui/task/          # 任务
├── ui/affair/        # 事务
├── widget/           # Glance 小部件
└── data/             # Room、Repository、教务 import
```

## 许可

个人学习 / 展示项目。如需二次使用请先联系作者。
