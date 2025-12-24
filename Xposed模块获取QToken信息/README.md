# Xposed模块：获取Q-Token相关信息

## 功能说明

这个Xposed模块可以Hook QQ浏览器，实时获取以下信息：
- **Q-GUID**: 设备唯一标识
- **Q-UA2**: 设备信息字符串
- **Kingcard**: 王卡识别码（可能为空）
- **Q-Token**: 认证Token

模块启动一个HTTP服务器（端口8888），提供RESTful API接口，方便Python脚本调用。

## 自动编译

本项目使用GitHub Actions自动编译，每次推送代码或创建Release时会自动构建APK。

### 下载编译好的APK

1. 前往 [Actions](https://github.com/YOUR_USERNAME/YOUR_REPO/actions) 页面
2. 选择最新的workflow运行
3. 在Artifacts部分下载 `qtokenhook-module-apk`

### 手动触发编译

1. 前往 [Actions](https://github.com/YOUR_USERNAME/YOUR_REPO/actions) 页面
2. 选择 "Build Xposed Module" workflow
3. 点击 "Run workflow" 按钮

## 本地编译

### 前置条件

- JDK 11或更高版本
- Android SDK
- Gradle 7.0+

### 编译步骤

```bash
cd Xposed模块获取QToken信息
./gradlew assembleDebug
```

编译后的APK位于：`app/build/outputs/apk/debug/app-debug.apk`

## 安装步骤

### 1. 前置条件

- Android设备已root
- 已安装Xposed Framework或LSPosed/EdXposed

### 2. 安装模块

1. 安装编译好的APK到Android设备
2. 在Xposed管理器中启用模块
3. 重启设备（或重启QQ浏览器）

### 3. 验证安装

启动QQ浏览器后，检查Xposed日志，应该看到：
```
QTokenHook: Hook QQ浏览器包名: com.tencent.mtt
QTokenHook: Hook Q-GUID成功
QTokenHook: Hook Q-UA2成功
QTokenHook: Hook Kingcard成功
QTokenHook: Hook Q-Token成功
QTokenHook: HTTP服务器启动成功，端口: 8888
```

## API接口说明

### 基础URL

```
http://设备IP:8888
```

### 接口列表

#### 1. 获取所有信息

**请求**:
```
GET /api/info
```

**响应**:
```json
{
  "q_guid": "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
  "q_ua2": "QV=3&PL=ADR&PR=QB&PP=com.tencent.mtt&...",
  "kingcard": "",
  "q_token": "xxxxx_token_string_xxxxx"
}
```

#### 2. 获取Q-GUID

**请求**:
```
GET /api/guid
```

**响应**:
```json
{
  "q_guid": "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6"
}
```

#### 3. 获取Q-UA2

**请求**:
```
GET /api/qua2
```

**响应**:
```json
{
  "q_ua2": "QV=3&PL=ADR&PR=QB&PP=com.tencent.mtt&..."
}
```

#### 4. 获取Kingcard

**请求**:
```
GET /api/kingcard
```

**响应**:
```json
{
  "kingcard": ""
}
```

#### 5. 获取Q-Token

**请求**:
```
GET /api/token
```

**响应**:
```json
{
  "q_token": "xxxxx_token_string_xxxxx"
}
```

## Python脚本使用

参考项目根目录的 `get_qtoken_from_xposed.py` 脚本。

```bash
# 使用ADB端口转发
adb forward tcp:8888 tcp:8888
python get_qtoken_from_xposed.py

# 或直接使用设备IP
python get_qtoken_from_xposed.py 192.168.1.100
```

## 注意事项

1. ⚠️ 需要root权限和Xposed框架
2. ⚠️ 仅用于学习和研究
3. ⚠️ 请遵守相关法律法规
4. ⚠️ 确保设备和PC在同一网络，或使用ADB端口转发

## 故障排查

### 问题1：HTTP服务器无法访问

- 检查Xposed日志，确认服务器已启动
- 检查防火墙设置
- 尝试使用ADB端口转发：`adb forward tcp:8888 tcp:8888`

### 问题2：获取的值为空

- 确保QQ浏览器已启动
- 在QQ浏览器中执行一些操作（如打开网页），触发相关方法调用
- 检查Xposed日志，确认Hook是否成功

### 问题3：Hook失败

- 检查类名和方法名是否正确（可能因版本不同而不同）
- 查看Xposed日志中的错误信息
- 确认QQ浏览器版本是否匹配

## License

本项目仅供学习和研究使用。
