# AGENTS.md — CMP JSXGraph 工程约定（强约束，任何对话都必须先读并遵守）

本文件是 `cmp_jsxgraph` 仓库的**不可协商约定**。新开对话、切换模型、换 Agent 都必须先读本文件并严格遵守。若某条指令与本文件冲突，以本文件为准，并向用户说明冲突点。

## 1. 目标与路线：忠实翻译（Translation Route）

- 本项目把 **JSXGraph** 翻译为纯 Kotlin Multiplatform / Compose Multiplatform 实现，**不使用 WebView，不内嵌 JS 引擎**，最终由 Compose Canvas 渲染。
- **必须走"忠实翻译"路线**，不是"凭记忆重写"或"自由发挥"。每一处 Kotlin 实现都应能对照上游 JSXGraph 的具体源码文件/函数，目的是**能持续跟随官方版本迭代**。
  - 上游参考源码位于 `third_party/jsxgraph-src/`（当前基线版本见下）。该目录被 `.gitignore` 忽略，不提交进仓库；缺失时用 §6 的命令重新拉取。
  - 翻译时在 Kotlin 端用注释标注对应的上游文件与函数名（例如 `// JSXGraph: src/base/coords.js -> usr2screen`），方便日后 diff 官方更新。
  - 命名尽量贴近上游（类名/方法名/字段名），除非与 Kotlin 习惯或平台限制冲突；冲突时保留可追溯的注释映射。
- **当前基线：JSXGraph `1.13.3`。** 升级基线时更新本文件、`THIRD_PARTY_NOTICES.md`，并复核受影响的翻译切片。

## 2. 范围裁剪

- **暂不实现 Symbolic（CAS，符号代数）模块**（`src/math/symbolic.js` 及其依赖的符号求导 / locus 轨迹符号推导）。需要时用数值方法降级，或显式标注为未支持。其余模块按需翻译。

## 3. 许可证与合规（必须遵守）

- JSXGraph 是 **LGPL v3 或 MIT 双许可**。本项目**选用 MIT 分支**。所有涉及 JSXGraph 的声明、文档都必须写明"依 MIT 许可"，**不要引入 LGPL 分支**（避免 copyleft 传染）。
- 本仓库自身以 **MIT** 发布（见 `LICENSE`），版权归属 `swithun`。
- 保留被翻译源文件头部的 JSXGraph 版权署名信息，并在 `THIRD_PARTY_NOTICES.md` 汇总。

## 4. 绝对红线：禁止引入公司相关内容

- **严禁**把任何公司相关的代码、标识、内部包名、内部服务地址、内部文档、密钥、配置、业务数据、内部人名/邮箱等带入本仓库的任何文件、注释、提交信息或历史。
- 本仓库只包含：JSXGraph 的公开 OSS 翻译成果、通用 KMP/CMP 工程代码、公开可得的第三方依赖。
- 提交前自查：diff 中不得出现任何内部/私有信息。

## 5. Git 提交约定

- 提交人固定为：`swithun <2571021108@qq.com>`。
- 每个仓库提交前确认 `git config user.name` / `user.email` 为上述值，或用 `git -c user.name=... -c user.email=...` 显式指定。
- 只在用户明确要求时才创建提交。

## 6. 命名与坐标（已定死，写进每个文件）

- 基础包名：`com.swithun.jsxgraph`
- 模块名：`jsxgraph-core`、`jsxgraph-compose`（后续）
- 发布坐标：`com.swithun:jsxgraph-core`、`com.swithun:jsxgraph-compose`
- Android namespace：`com.swithun.jsxgraph.core` 等对应模块名。

拉取/更新上游参考源码（不提交）：

```bash
git clone --depth 1 --branch <tag> https://github.com/jsxgraph/jsxgraph.git third_party/jsxgraph-src
```

## 7. 代码风格（swithun-code-style）

- 遵循 `swithun-code-style` 技能的全部规则。要点：
  - 可控签名的**解析 / 校验 / 状态 / I/O / 业务失败**必须用 `GMResult<T, E>` 显式表达，不用 `throw` / `!!` / 不安全类型转换隐藏失败。
  - 纯几何数值翻译（如坐标变换）按上游语义忠实移植 `Double` 运算（含 NaN/Infinity 传播），这类无"预期失败"的纯算术不强套 `GMResult`；但**解析上游数据、DSL/JSON 输入**这类边界必须用 `GMResult`。
  - 先区分新增代码与存量/共享代码，最小化对已有行为的高风险改动。
  - 讲解变更时给出可核对的"改前/改后"，标注新增/修改/未改。

## 8. 正确性验证

- 翻译的几何/数值逻辑要有对拍思路：以官方 JSXGraph 行为为参考，对关键变换/算法写单元测试验证。
- 提交的代码必须能通过 `./gradlew :jsxgraph-core:allTests`（或对应任务）。
