# GitHub Actions 工作流说明

## Build Xposed Module

自动编译Xposed模块的GitHub Actions工作流。

### 触发条件

- 推送到 `main` 或 `master` 分支
- 创建Pull Request到 `main` 或 `master` 分支
- 手动触发（workflow_dispatch）
- 创建Release时自动构建并上传

### 工作流程

1. **Checkout代码** - 检出仓库代码
2. **设置JDK 11** - 配置Java开发环境
3. **设置Android SDK** - 配置Android构建环境
4. **编译APK** - 使用Gradle编译debug版本APK
5. **验证APK** - 检查APK文件是否存在
6. **上传Artifact** - 将APK上传到GitHub Artifacts
7. **创建Release** - 如果是Release事件，自动创建Release并上传APK

### 下载编译结果

1. 前往 [Actions](https://github.com/YOUR_USERNAME/YOUR_REPO/actions) 页面
2. 选择最新的workflow运行
3. 在 **Artifacts** 部分下载 `qtokenhook-module-apk`

### 手动触发

1. 前往 [Actions](https://github.com/YOUR_USERNAME/YOUR_REPO/actions) 页面
2. 选择 "Build Xposed Module" workflow
3. 点击右上角的 "Run workflow" 按钮
4. 选择分支并点击 "Run workflow"

### 注意事项

- 编译需要约2-5分钟
- Artifacts会保留30天
- 如果编译失败，可以查看日志排查问题

