[OPEN]

Session: lsposed-module-missing

Problem:
LSPosed 里看不到模块，需要核对当前代码和原始 xhs_1.0 文件夹实现，确认模块是否完整可识别。

Hypotheses:
1. 模块元数据文件位置或格式不符合 LSPosed 识别要求，导致模块未被扫描到。
2. `java_init.list` 中入口类名与实际类名不一致，导致模块入口未加载。
3. `scope.list` 的目标包名配置错误，导致模块未在 LSPosed 中显示为可选模块。
4. 当前重写项目缺少原始模块的某些识别文件或资源，导致“能编译但不能被 LSPosed 识别”。
5. 原始 xhs_1.0 文件夹里存在额外的模块声明或目录结构差异，当前重写未完全复现。

Status:
Collecting evidence.
