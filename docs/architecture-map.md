# 架构速查图（极简版）

> 目的：给后续“完善功能细节”类任务提供最快定位入口，不追求完整，不代表已完成的目标架构。
> 详细规范见根目录 `AGENTS.md`；深入的目标结构/迁移计划见 `docs/dev/`。

## 核心技术栈

- 语言/构建：Kotlin + Gradle Kotlin DSL（`app/build.gradle.kts`），JDK 21，Android Gradle Plugin。
- UI：渐进式迁移中——遗留 View/XML（`ui/` 下多数模块）与新 Jetpack Compose（`feature/` 下、部分
  `ui/*` 子模块）并存；Material 3 主题、动态取色（MaterialKolor）。
- 应用标识：Kotlin 包名 `io.legado.app`；Android `applicationId` = `io.legato.kazusa`（两者不要混用）。
- 数据层：Room（`data/dao` + `data/entities`）。
- 网络：OkHttp + Cronet；内置 Web 服务基于 Ktor（`web/KtorServer.kt`），配合独立前端 `modules/web`
  (Vue 3)。
- 规则引擎：Rhino（JS 沙箱，独立模块 `:modules:rhino`），用于书源/RSS 规则执行；书源 JS 可调用
  Hutool（仅此用途，见 `AGENTS.md`）。
- 电子书解析：`:modules:book`（epublib/umdlib 等，包名 `me.ag2s.*`）。
- 依赖注入：Koin。
- 阅读渲染：成熟 View 渲染岛（`ui/book/read/page` 翻页引擎），暂不迁移 Compose。
- Gradle 模块：`:app`（主体）、`:modules:book`（电子书格式解析）、`:modules:rhino`（JS 规则引擎）、
  `:baselineprofile`（性能基线）；`modules/web` 是独立 Vue 3 前端工程，不参与 Gradle 构建。

## 核心功能模块 → 包路径

以下路径均相对 `app/src/main/java/io/legado/app/`，除非另外标注。

| 功能模块 | 主要包路径 | 说明 |
| --- | --- | --- |
| **书架**（书籍列表/分组/首页） | `ui/main/bookshelf`、`ui/main/home`、`ui/main/homepage` | 主界面书架、首页自定义模块 |
| **书源管理**（增删改查/校验/调试） | `ui/book/source/{edit,manage,debug}` | 书源编辑、批量管理、规则调试 |
| **换源/搜索** | `ui/book/changesource`、`ui/book/search`、`ui/book/searchContent` | 换源、书籍搜索、正文内搜索 |
| **阅读器**（翻页/排版/菜单） | `ui/book/read/{page,config,sheet,pageestimate}`、`feature/reader` | `page` 是核心 View 翻页渲染岛；
  `feature/reader` 是 Compose-first 新实现区 |
| **朗读/听书** | `ui/book/readaloud`、`service/{BaseReadAloudService,TTSReadAloudService,HttpReadAloudService}`、
  `help/readaloud` | 朗读界面 + 前后台服务 + TTS 帮助类 |
| **有声书/音频播放** | `ui/book/audio`、`service/AudioPlayService`、`help/exoplayer` | 基于 Media3 ExoPlayer |
| **漫画阅读** | `ui/book/manga` | 独立漫画阅读界面 |
| **目录/书签/阅读记录** | `ui/book/{toc,bookmark,readRecord}` | 目录、书签、阅读时长/章节统计 |
| **书籍详情/换封面/知识卡片** | `ui/book/{info,changecover,knowledge}` | 书籍详情页、封面更换、AI 知识卡片 |
| **导入/缓存/管理** | `ui/book/{import,cache,manage}`、`service/{CacheBookService,ExportBookService,DownloadService}` |
  本地导入、离线缓存、批量管理、导出、下载 |
| **发现（书源探索）** | `ui/main/explore`、`ui/book/explore` | 分类/发现页 |
| **RSS 订阅** | `ui/rss`、`ui/main/rss`、`data/entities/{RssSource,RssArticle,RssReadRecord,RssStar}.kt`、
  `model/rss` | RSS 源、文章、收藏、已读记录 |
| **净化替换规则** | `ui/replace`、`data/entities/ReplaceRule.kt` | 正文广告净化/替换规则管理 |
| **AI 功能**（对话/知识库） | `ui/ai`、`data/entities/Ai*.kt` | AI 对话、提示词预设、模型/供应商配置 |
| **主题/外观** | `ui/theme`、`help/config` | Material 3 主题、动态取色、样式配置 |
| **登录/浏览器容器** | `ui/login`、`ui/browser` | 书源登录 WebView、内置浏览器 |
| **文件/工具类界面** | `ui/file`、`ui/util`、`ui/qrcode`、`ui/dict` | 文件选择、通用工具页、二维码、词典 |
| **设置** | `ui/config` | 应用设置各分类页面 |
| **规则/标签组管理** | `ui/tagGroupRule`、`ui/highlightTagRule` | 标签分组规则、高亮标签规则 |
| **数据实体/DAO（Room）** | `data/entities/*.kt`、`data/dao/*.kt` | Book/BookSource/RssSource/ReplaceRule 等全部持久化模型 |
| **网络请求与解析规则执行** | `model/{webBook,analyzeRule,localBook,remote,cache,translation}`、`help/http`、
  `help/rhino`、`:modules:rhino` | 书源请求、规则解析（AnalyzeRule/AnalyzeByJSoup 等）、Rhino JS 执行 |
| **电子书格式解析（TXT/EPUB/UMD）** | `:modules:book`（`me/ag2s/{epublib,umdlib}`） | 独立 Gradle 模块，非
  `io.legado.app` 包 |
| **内置 Web 服务**（局域网管理） | `web/KtorServer.kt`、`web/socket`、`service/WebService.kt`；前端在
  `modules/web`（独立 Vue 3 工程） | 手机端开 HTTP 服务，PC 浏览器通过 `modules/web` 管理书架/书源 |
| **加密/存储/更新等基础设施** | `help/{crypto,storage,update,http,coroutine,glide,coil,exoplayer,webView}` | 通用帮助类，非单一业务功能 |
| **DI 组装** | `di/` | Koin module 定义 |
| **Compose-first 新 Feature 区** | `feature/{reader,onboarding}` | 按 `AGENTS.md` Feature-first 规范新建的
  Compose 页面，未来新 Compose 功能优先落这里 |

## 备注

- `ui/` 是历史 View 迁移区，并非所有子目录都已 Compose 化；具体某个页面是 View 还是 Compose 需要打开
  对应目录实际确认（例如是否存在 `xxxScreen.kt`/`Content.kt` 而非 `xxxActivity.kt`/`xxxFragment.kt`
  + `xxx.xml`）。
- 本文档只做定位索引，不代表模块边界已经用 Gradle 强制隔离；跨模块调用关系以实际 import 为准。
