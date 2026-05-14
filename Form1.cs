using KaiosMarketDownloader.Beans;
using KaiosMarketDownloader.utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace KaiosMarketDownloader
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
            this.Load += Form1_Load;
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            numericUpDown1.Value = OperateIniFile.ReadIniInt("setting", "threadCount", threadCount);
            checkBox1.Checked = OperateIniFile.ReadIniInt("setting", "ZengLiang", ZengLiang ? 1 : 0) == 1 ? true : false;
            var savedProxy = OperateIniFile.ReadIniString("setting", "HttpProxy", string.Empty);
            txtProxy.Text = savedProxy;

            // 加载 UA 列表
            uaList = UAEditorForm.LoadUAListFromIni();
            selectedUARemark = OperateIniFile.ReadIniString("setting", "SelectedUARemark", string.Empty);

            // 如果 UA 列表为空，添加所有 Go Flip UA
            if (uaList.Count == 0)
            {
                InitializeDefaultUAList();
            }

            UpdateUAComboBox();
            comboBoxUA.Visible = true;
            labelUA.Visible = true;
        }

        private void InitializeDefaultUAList()
        {
            int order = 0;

            // Firefox 内核 37 - KaiOS/1.0
            uaList.Add(new UAEntry { Remark = "Go Flip 1.0", UA = KaiSton.V1Str, Order = order++ });

            // Firefox 内核 48 - KaiOS 2.x 系列
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5", UA = KaiSton.V2_5Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.1", UA = KaiSton.V2_5_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.1.1", UA = KaiSton.V2_5_1_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.1.2", UA = KaiSton.V2_5_1_2Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.2", UA = KaiSton.V2_5_2Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.2.1", UA = KaiSton.V2_5_2_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.2.2", UA = KaiSton.V2_5_2_2Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.3", UA = KaiSton.V2_5_3Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.3.1", UA = KaiSton.V2_5_3_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.3.2", UA = KaiSton.V2_5_3_2Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.4", UA = KaiSton.V2_5_4Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.5.4.1", UA = KaiSton.V2_5_4_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 2.6", UA = KaiSton.V2_6Str, Order = order++ });

            // Firefox 内核 84 - KaiOS 3.x 系列
            uaList.Add(new UAEntry { Remark = "Go Flip 3.0", UA = KaiSton.V3_0Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 3.1", UA = KaiSton.V3_1Str, Order = order++ });
            uaList.Add(new UAEntry { Remark = "Go Flip 3.2", UA = KaiSton.V3_2Str, Order = order++ });

            // Firefox 内核 123 - KaiOS 4.0
            uaList.Add(new UAEntry { Remark = "Go Flip 4.0", UA = KaiSton.V4Str, Order = order++ });

            UAEditorForm.SaveUAListToIni(uaList);
        }
        
        private void UpdateUAComboBox()
        {
            if (comboBoxUA != null)
            {
                comboBoxUA.DataSource = null;
                comboBoxUA.DataSource = uaList;
                comboBoxUA.DisplayMember = "Remark";
                comboBoxUA.ValueMember = "UA";
                
                if (!string.IsNullOrEmpty(selectedUARemark))
                {
                    var selected = uaList.FirstOrDefault(x => x.Remark == selectedUARemark);
                    if (selected != null)
                    {
                        comboBoxUA.SelectedItem = selected;
                    }
                }
            }
        }
        private string currentKaiOSVersion = "3.1";
        Thread thread = null;
        private bool CustomUA = true;
        private List<UAEntry> uaList = new List<UAEntry>();
        private string selectedUARemark = string.Empty;
        private bool SelectedIsDefaultV3OrV2(UAEntry selected)
        {
            if (selected == null || string.IsNullOrEmpty(selected.UA)) return false;
            var ua = selected.UA.Trim();
            // 检查是否是默认的V3.1或V2.5.4.1
            return ua == KaiSton.V3_1Str || ua == KaiSton.V2_5_4_1Str;
        }

        private string GetKaiOSVersion(UAEntry selected)
        {
            try
            {
                var js = Newtonsoft.Json.Linq.JObject.Parse(selected.UA);
                var ver = js["dev"]?["version"]?.ToString() ?? "3.1";
                return ver;
            }
            catch
            {
                return "3.1";
            }
        }
        private string MakeSafeFolderToken(string text)
        {
            if (string.IsNullOrWhiteSpace(text)) return "custom";
            var parts = text.Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
            var token = parts.Length > 0 ? parts[parts.Length - 1] : text;
            foreach (var c in Path.GetInvalidFileNameChars())
            {
                token = token.Replace(c.ToString(), "_");
            }
            return token.ToLower();
        }

        private string MakeSafeFileName(string text)
        {
            if (string.IsNullOrWhiteSpace(text)) return "unknown";
            foreach (var c in Path.GetInvalidFileNameChars())
            {
                text = text.Replace(c.ToString(), " ");
            }
            return text;
        }

        private string GetDownloadPath()
        {
            if (CustomUA)
            {
                var suffix = MakeSafeFolderToken(selectedUARemark);
                return Directory.GetCurrentDirectory() + "\\eggs_" + suffix + "\\";
            }
            return Directory.GetCurrentDirectory() + "\\eggs_" + currentKaiOSVersion + "\\";
        }

        private string GetOldPath()
        {
            if (CustomUA)
            {
                var suffix = MakeSafeFolderToken(selectedUARemark);
                return Directory.GetCurrentDirectory() + "\\eggs_" + suffix + "_old\\";
            }
            return Directory.GetCurrentDirectory() + "\\eggs_" + currentKaiOSVersion + "_old\\";
        }

        private void ApplyUASettings(UAEntry entry)
        {
            if (entry != null && !string.IsNullOrEmpty(entry.UA))
            {
                try
                {
                    KaiSton.settingsStr = entry.UA;
                    KaiSton.jsonSetting = JObject.Parse(entry.UA);
                }
                catch
                {
                    KaiSton.settingsStr = KaiSton.V3_1Str;
                    KaiSton.jsonSetting = JObject.Parse(KaiSton.V3_1Str);
                }
                currentKaiOSVersion = GetKaiOSVersion(entry);
                CustomUA = !SelectedIsDefaultV3OrV2(entry);
            }
            else
            {
                KaiSton.settingsStr = KaiSton.V3_1Str;
                KaiSton.jsonSetting = JObject.Parse(KaiSton.V3_1Str);
                currentKaiOSVersion = "3.1";
                CustomUA = false;
            }
        }

         private void button1_Click(object sender, EventArgs e)
        {
            try
            {
                File.Delete("log.txt");
            }
            catch (Exception ex)
            {

            }
            ZengLiang = checkBox1.Checked;
            threadCount = Convert.ToInt32(numericUpDown1.Value);
            // 始终使用所选UA
            OperateIniFile.WriteIniInt("setting", "threadCount", threadCount);
            OperateIniFile.WriteIniInt("setting", "ZengLiang", ZengLiang ? 1 : 0);
            OperateIniFile.WriteIniString("setting", "HttpProxy", txtProxy.Text ?? string.Empty);
            KaiSton.SetProxy(txtProxy.Text ?? string.Empty);
            
            // 保存选中的 UA
            if (comboBoxUA?.SelectedItem is UAEntry selected)
            {
                selectedUARemark = selected.Remark;
                OperateIniFile.WriteIniString("setting", "SelectedUARemark", selectedUARemark);
            }

            // 应用所选UA
            ApplyUASettings(comboBoxUA?.SelectedItem as UAEntry);


            if (button1.Text == "开始下崽")
            {
                button1.Enabled = false;
                thread = new Thread(DownloadThread);
                thread.IsBackground = true;
                thread.Start();
                button1.Text = "停止下崽";
                button1.Enabled = true;
            }
            else
            {
                button1.Enabled = false;
                try
                {
                    if (thread != null)
                    {

                        thread.Abort();
                    }
                }
                catch (Exception ex)
                {

                }
                foreach (Thread t in threadlist)
                {
                    try
                    {
                        if (t != null)
                        {
                            t.Abort();
                        }
                    }
                    catch (Exception ex)
                    {

                    }
                }
                threadlist.Clear();
                button1.Text = "开始下崽";
                button1.Enabled = true;
            }
        }

        private void Log(string msg)
        {
            msg = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss -> ") + msg + "\r\n";
            Console.WriteLine(msg);
            this.Invoke(new Action(() =>
            {
                txt_log.AppendText(msg);
                txt_log.SelectionStart = txt_log.Text.Length;
                txt_log.ScrollToCaret();

            }));
            lock (locker)
            {
                File.AppendAllText("log.txt", msg);
            }
        }
        int threadCount = 5;
        int count = 0;
        private List<string> downloadedZipFiles = new List<string>();
        private void UpdateLabel()
        {
            label1.Invoke(new Action(() =>
            {
                label1.Text = string.Format("当前{0}/共{1}", count - downlist.Count, count);
            }));
        }
        private object locker = new object();
        private void DownThread()
        {
            while (true)
            {
                try
                {
                    if (downlist.Count == 0)
                    {
                        try
                        {
                            lock (locker)
                            {
                                threadlist.Remove(threadlist.First(p => p.Name == Thread.CurrentThread.Name));
                            }
                        }
                        catch (Exception ex)
                        {

                        }
                        if (threadlist.Count == 0)
                        {
                            Finished();
                        }
                        return;
                    }
                    KaiosStoneItem item = null;
                    lock (locker)
                    {
                        item = downlist.Dequeue();
                    }
                    UpdateLabel();
                    int i = item.nowid;
                    string rename = item.rename;
                    string savename = item.savename;

                    for (int trycount = 1; trycount <= 5; trycount++)
                    {
                        try
                        {
                            Log("第" + Thread.CurrentThread.Name + "只母鸡，正在努力下第" + (i + 1) + "/" + count + "只崽,崽的名字叫：" + rename + "！");

                            var downlink = item.package_path;

                            var res = KaiSton.RequestDown("GET", downlink, "");

                            if (res.Length < 4096)
                            {
                                try
                                {
                                    string data = "";
                                    data = Encoding.UTF8.GetString(res);
                                    var jsonobj = JObject.Parse(data);
                                    if (jsonobj["code"].ToString() == "401")
                                    {
                                        KaiSton.getKey();
                                        continue;
                                    }
                                }
                                catch (Exception ex)
                                {

                                }
                            }
                            lock (locker)
                            {
                                File.WriteAllBytes(savename, res);
                                downloadedZipFiles.Add(savename);
                            }

                            Log("第" + Thread.CurrentThread.Name + "只母鸡，" + "第" + (i + 1) + "/" + count + "只崽 " + rename + " 已经下完！");

                            break;
                        }
                        catch (Exception ex)
                        {
                            Log("第" + Thread.CurrentThread.Name + "只母鸡，" + "第" + (i + 1) + "/" + count + "只崽 " + rename + " 不肯出窝并说\"" + ex.Message + "\"，重试第" + trycount + "次！");
                        }
                    }
                }
                catch (Exception ex)
                {
                    Thread.Sleep(500);
                }
            }
        }
        private void Finished()
        {
            Log("恭喜，全部下崽完成！！！");

            // 更新所有下载的zip文件的日期
            if (downloadedZipFiles.Count > 0)
            {
                Log("开始更新zip文件日期...");
                int updateCount = 0;
                foreach (var zipPath in downloadedZipFiles)
                {
                    UpdateZipFileDate(zipPath);
                    updateCount++;
                }
                Log("zip文件日期更新完成，共更新" + updateCount + "个文件！");
                downloadedZipFiles.Clear();
            }

            this.Invoke(new Action(() =>
            {
                button1.Text = "开始下崽";
            }));
        }
        Queue<KaiosStoneItem> downlist = new Queue<KaiosStoneItem>();
        List<Thread> threadlist = new List<Thread>();
        bool ZengLiang = false;
        private void DownloadThread()
        {
            try
            {
                downloadedZipFiles.Clear();
                Log("开始下崽...");
                KaiSton.getKey();
                Log("正在寻找母鸡...");
                var ret = "";

                ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_" + currentKaiOSVersion + "&locale=zh-CN", "");
                var retjson = JObject.Parse(ret);
                var apps = retjson.ToString(Formatting.Indented);
                File.WriteAllText("appsdata_" + KaiSton.model + ".json", apps);
                var allapps = JsonConvert.DeserializeObject<List<KaiosStoneItem>>(retjson["apps"].ToString());

                Log("母鸡已经找到，共有" + allapps.Count + "个崽，开始准备下崽...");
                string downloadpath = GetDownloadPath();

                try
                {
                    if (!Directory.Exists(downloadpath))
                    {
                        Directory.CreateDirectory(downloadpath);
                    }
                }
                catch { }

                downlist.Clear();
                count = allapps.Count;
                UpdateLabel();
                for (int i = 0; i < allapps.Count; i++)
                {
                    KaiosStoneItem item = allapps[i];
                    item.nowid = i;
                    string rename = (item.display?.Replace(" ", " ") ?? item.name?.Replace(" ", " ")) + " " + item.version + ".zip";
                    rename = rename.Replace("\\", " ");

                    rename = rename.Replace("/", " ");

                    rename = rename.Replace(":", " ");

                    rename = rename.Replace("*", " ");

                    rename = rename.Replace("\"", " ");

                    rename = rename.Replace("<", " ");

                    rename = rename.Replace(">", " ");

                    rename = rename.Replace("|", " ");

                    rename = rename.Replace("?", " ");

                    var savename = downloadpath + "\\" + rename;
                    item.rename = rename;
                    item.savename = savename;
                    if (ZengLiang)
                    {
                        if (File.Exists(savename))
                        {
                            try
                            {
                                if (new FileInfo(savename).Length < 4096)
                                {
                                    string filecontent = File.ReadAllText(savename);

                                    var jsonobj = JObject.Parse(filecontent);
                                    if (jsonobj["code"].ToString() == "401")
                                    {
                                        Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 是坏崽，删除！");
                                        File.Delete(savename);
                                    }
                                    else
                                    {
                                        Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                        continue;
                                    }
                                }
                                else
                                {
                                    Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                    continue;
                                }
                            }
                            catch (Exception ex)
                            {
                                Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                continue;
                            }
                        }
                    }
                    if (string.IsNullOrWhiteSpace(item.package_path))
                    {
                        Log("第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 可能是云崽，不用下崽了！");
                        continue;
                    }
                    downlist.Enqueue(item);
                }
                if(downlist.Count==0)
                {
                    Finished();
                    return;
                }
                if (downlist.Count > threadCount)
                {
                    threadCount = downlist.Count;
                }

                for (int i = 0; i < threadCount; i++)
                {
                    var threadnow = new Thread(DownThread);
                    threadnow.IsBackground = true;
                    threadnow.Start();
                    threadnow.Name = (i + 1).ToString();
                    threadlist.Add(threadnow);
                }
            }
            catch (Exception ex)
            {
                Log("悲，崽崽难产了！！！" + ex.Message);
                this.Invoke(new Action(() =>
                {
                    button1.Text = "开始下崽";
                }));
            }
        }
        private bool isrunning = false;
        private bool isAllUARunning = false;
        private void button2_Click(object sender, EventArgs e)
        {
            try
            {
                ZengLiang = checkBox1.Checked;
                threadCount = Convert.ToInt32(numericUpDown1.Value);
                OperateIniFile.WriteIniString("setting", "HttpProxy", txtProxy.Text ?? string.Empty);
                KaiSton.SetProxy(txtProxy.Text ?? string.Empty);
                OperateIniFile.WriteIniInt("setting", "threadCount", threadCount);
                OperateIniFile.WriteIniInt("setting", "ZengLiang", ZengLiang ? 1 : 0);
                
                // 保存选中的 UA
                if (comboBoxUA?.SelectedItem is UAEntry selected)
                {
                    selectedUARemark = selected.Remark;
                    OperateIniFile.WriteIniString("setting", "SelectedUARemark", selectedUARemark);
                }

                // 应用所选UA
                ApplyUASettings(comboBoxUA?.SelectedItem as UAEntry);


                if (isrunning)
                {

                    Log("正在整理崽崽，请稍等...");
                    return;
                }

                if (button1.Text == "开始下崽")
                {
                    isrunning = true;
                    Thread taskThread = new Thread(() =>
                    {
                        try
                        {
                            Log("开始整理崽崽...");
                            KaiSton.getKey();
                            Log("正在寻找崽崽的索引...");
                            var ret = "";

                            ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_" + currentKaiOSVersion + "&locale=zh-CN", "");
                            var retjson = JObject.Parse(ret);
                            var apps = retjson.ToString(Formatting.Indented);
                            File.WriteAllText("appsdata_" + KaiSton.model + ".json", apps);
                            var allapps = JsonConvert.DeserializeObject<List<KaiosStoneItem>>(retjson["apps"].ToString());

                            string downloadpath = GetDownloadPath();
                            string oldpath = GetOldPath();
                            if (!Directory.Exists(oldpath))
                            {
                                try
                                {
                                    Directory.CreateDirectory(oldpath);
                                }
                                catch (Exception ex)
                                {
                                }
                            }
                            List<string> saveNames = new List<string>();
                            for (int i = 0; i < allapps.Count; i++)
                            {
                                KaiosStoneItem item = allapps[i];
                                item.nowid = i;
                                string rename = (item.display?.Replace(" ", " ") ?? item.name?.Replace(" ", " ")) + " " + item.version + ".zip";
                                rename = rename.Replace("\\", " ");

                                rename = rename.Replace("/", " ");

                                rename = rename.Replace(":", " ");

                                rename = rename.Replace("*", " ");

                                rename = rename.Replace("\"", " ");

                                rename = rename.Replace("<", " ");

                                rename = rename.Replace(">", " ");

                                rename = rename.Replace("|", " ");

                                rename = rename.Replace("?", " ");

                                var savename = rename;
                                saveNames.Add(savename);
                            }
                            int cnt = 0;
                            List<string> paths = Directory.GetFiles(downloadpath).ToList();

                            foreach (string path in paths)
                            {
                                string pth = Path.GetFileName(path);
                                if (!saveNames.Contains(pth))
                                {
                                    try
                                    {
                                        File.Move(path, oldpath + pth);

                                        Log(pth + "已移动！");
                                        cnt++;
                                    }
                                    catch (Exception ex)
                                    {
                                        Log("悲，报错了！！！" + ex.Message);

                                    }
                                }
                            }

                            Log("操作完成，移动了" + cnt + "个旧崽！");
                        }
                        catch (Exception ex)
                        {

                            Log("悲，报错了！！！" + ex.Message);
                        }
                        finally
                        {
                            isrunning = false;
                        }
                    });
                    taskThread.IsBackground = true;
                    taskThread.Start();
                }
                else
                {
                    MessageBox.Show("正在下崽，请等待结束后再执行整理操作！");
                }
            }
            catch (Exception ex)
            {
                Log("悲，报错了！！！" + ex.Message);

            }
        }

        private void button3_Click(object sender, EventArgs e)
        {
            var editor = new UAEditorForm(uaList, uaList.FirstOrDefault(x => x.Remark == selectedUARemark));
            var result = editor.ShowDialog();

            // 只有点击了保存按钮才重新加载 UA 列表
            if (result == DialogResult.OK)
            {
                // 重新从文件加载最新的 UA 列表
                uaList = UAEditorForm.LoadUAListFromIni();

                // 刷新 ComboBox
                UpdateUAComboBox();

                // 尝试保持之前的选择
                if (!string.IsNullOrEmpty(selectedUARemark))
                {
                    var selected = uaList.FirstOrDefault(x => x.Remark == selectedUARemark);
                    if (selected != null)
                    {
                        comboBoxUA.SelectedItem = selected;
                    }
                }

                // 保存选中的 UA
                if (comboBoxUA?.SelectedItem is UAEntry currentSelected)
                {
                    selectedUARemark = currentSelected.Remark;
                    OperateIniFile.WriteIniString("setting", "SelectedUARemark", selectedUARemark);
                }
            }
        }

        private void buttonSaveSettings_Click(object sender, EventArgs e)
        {
            try
            {
                threadCount = Convert.ToInt32(numericUpDown1.Value);
                ZengLiang = checkBox1.Checked;
                var proxy = txtProxy.Text.Trim();
                
                OperateIniFile.WriteIniInt("setting", "threadCount", threadCount);
                OperateIniFile.WriteIniInt("setting", "ZengLiang", ZengLiang ? 1 : 0);
                OperateIniFile.WriteIniString("setting", "HttpProxy", proxy);
                KaiSton.SetProxy(proxy);

                if (comboBoxUA?.SelectedItem is UAEntry selected)
                {
                    selectedUARemark = selected.Remark;
                    OperateIniFile.WriteIniString("setting", "SelectedUARemark", selectedUARemark);
                }
                
                MessageBox.Show("设置已保存！");
            }
            catch (Exception ex)
            {
                MessageBox.Show("保存失败：" + ex.Message);
            }
        }

        private void buttonAllUA_Click(object sender, EventArgs e)
        {
            try
            {
                System.Windows.Forms.Button btn = sender as System.Windows.Forms.Button;
                if (btn.Text == "一键下整")
                {
                    // 开始一键下整
                    if (button1.Text != "开始下崽" || isrunning)
                    {
                        MessageBox.Show("正在下崽或整理，请等待结束后再执行！");
                        return;
                    }
                    isAllUARunning = true;
                    btn.Text = "停止下整";
                    Thread taskThread = new Thread(() =>
                    {
                        try
                        {
                            ZengLiang = checkBox1.Checked;
                            threadCount = Convert.ToInt32(numericUpDown1.Value);
                            OperateIniFile.WriteIniString("setting", "HttpProxy", txtProxy.Text ?? string.Empty);
                            KaiSton.SetProxy(txtProxy.Text ?? string.Empty);
                            for (int idx = 0; idx < uaList.Count && isAllUARunning; idx++)
                            {
                                if (!isAllUARunning) break;

                                var entry = uaList[idx];
                                selectedUARemark = entry.Remark;
                                OperateIniFile.WriteIniString("setting", "SelectedUARemark", selectedUARemark);
                                ApplyUASettings(entry);
                                DownloadAndOrganizeForCurrentUA();
                            }
                            if (isAllUARunning)
                            {
                                this.Invoke(new Action(() =>
                                {
                                    MessageBox.Show("一键下整完成！");
                                }));
                            }
                            else
                            {
                                this.Invoke(new Action(() =>
                                {
                                    MessageBox.Show("一键下整已停止！");
                                }));
                            }
                        }
                        catch (Exception ex)
                        {
                            this.Invoke(new Action(() =>
                            {
                                MessageBox.Show("执行失败：" + ex.Message);
                            }));
                        }
                        finally
                        {
                            isAllUARunning = false;
                            this.Invoke(new Action(() =>
                            {
                                btn.Text = "一键下整";
                            }));
                        }
                    });
                    taskThread.IsBackground = true;
                    taskThread.Start();
                }
                else
                {
                    // 停止一键下整
                    isAllUARunning = false;
                    Log("正在停止一键下整...");
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("执行失败：" + ex.Message);
            }
        }

        private void UpdateZipFileDate(string zipFilePath)
        {
            int maxRetries = 5;
            int retryDelayMs = 100;
            DateTime targetDate = DateTime.Now;

            // 第一步：读取zip文件内最新文件的日期
            for (int retry = 0; retry < maxRetries; retry++)
            {
                try
                {
                    if (!File.Exists(zipFilePath))
                    {
                        return;
                    }

                    // 使用 ZipArchive 读取zip文件内的条目信息（兼容 .NET 4.0）
                    using (var fileStream = new FileStream(zipFilePath, FileMode.Open, FileAccess.Read, FileShare.Read))
                    using (var archive = new System.IO.Compression.ZipArchive(fileStream, System.IO.Compression.ZipArchiveMode.Read))
                    {
                        var entryDates = new List<KeyValuePair<string, DateTime>>();

                        foreach (var entry in archive.Entries)
                        {
                            // 跳过目录条目
                            if (!string.IsNullOrEmpty(entry.Name))
                            {
                                entryDates.Add(new KeyValuePair<string, DateTime>(entry.FullName, entry.LastWriteTime.DateTime));
                            }
                        }

                        if (entryDates.Count > 0)
                        {
                            // 按日期降序排序
                            entryDates.Sort((a, b) => b.Value.CompareTo(a.Value));

                            DateTime today = DateTime.Now.Date;
                            targetDate = today; // 默认使用今天日期
                            bool foundValidDate = false;

                            // 从最新开始检查，找到第一个不超过今天的文件日期
                            foreach (var entry in entryDates)
                            {
                                if (entry.Value <= today)
                                {
                                    targetDate = entry.Value;
                                    foundValidDate = true;
                                    Log(Path.GetFileName(zipFilePath) + " 使用文件日期：" + entry.Key + " -> " + targetDate.ToString("yyyy-MM-dd HH:mm:ss"));
                                    break;
                                }
                            }

                            // 如果所有文件的日期都超过今天，使用今天日期
                            if (!foundValidDate)
                            {
                                targetDate = today;
                                Log(Path.GetFileName(zipFilePath) + " 所有文件日期都超过今天，使用今天日期：" + targetDate.ToString("yyyy-MM-dd HH:mm:ss"));
                            }
                        }
                        else
                        {
                            return;
                        }
                    }
                    // 读取成功，跳出循环
                    break;
                }
                catch (IOException)
                {
                    // 文件被占用，等待后重试
                    if (retry < maxRetries - 1)
                    {
                        Thread.Sleep(retryDelayMs);
                        retryDelayMs *= 2; // 指数退避
                    }
                    else
                    {
                        Log("读取zip文件日期失败：" + Path.GetFileName(zipFilePath) + " - 文件被占用，重试" + maxRetries + "次后仍失败");
                        return;
                    }
                }
                catch (Exception ex)
                {
                    Log("读取zip文件日期失败：" + Path.GetFileName(zipFilePath) + " - " + ex.Message);
                    return;
                }
            }

            // 第二步：修改zip文件的日期（确保文件已关闭）
            retryDelayMs = 100;
            for (int retry = 0; retry < maxRetries; retry++)
            {
                try
                {
                    // 修改zip文件的日期
                    File.SetCreationTime(zipFilePath, targetDate);
                    File.SetLastWriteTime(zipFilePath, targetDate);
                    File.SetLastAccessTime(zipFilePath, targetDate);
                    return;
                }
                catch (IOException)
                {
                    // 文件被占用，等待后重试
                    if (retry < maxRetries - 1)
                    {
                        Thread.Sleep(retryDelayMs);
                        retryDelayMs *= 2; // 指数退避
                    }
                    else
                    {
                        Log("修改zip文件日期失败：" + Path.GetFileName(zipFilePath) + " - 文件被占用，重试" + maxRetries + "次后仍失败");
                    }
                }
                catch (Exception ex)
                {
                    Log("修改zip文件日期失败：" + Path.GetFileName(zipFilePath) + " - " + ex.Message);
                    return;
                }
            }
        }

        private void DownloadAndOrganizeForCurrentUA()
        {
            try
            {
                // 检查是否应该停止
                if (!isAllUARunning) return;

                downloadedZipFiles.Clear();
                Log("开始下崽...");
                KaiSton.getKey();
                Log("正在寻找母鸡...");
                var ret = "";
                ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_" + currentKaiOSVersion + "&locale=zh-CN", "");
                var retjson = JObject.Parse(ret);
                var apps = retjson.ToString(Formatting.Indented);
                File.WriteAllText("appsdata_" + KaiSton.model + ".json", apps);
                var allapps = JsonConvert.DeserializeObject<List<KaiosStoneItem>>(retjson["apps"].ToString());

                Log("母鸡已经找到，共有" + allapps.Count + "个崽，开始准备下崽...");
                string downloadpath = GetDownloadPath();
                if (!Directory.Exists(downloadpath))
                {
                    Directory.CreateDirectory(downloadpath);
                }
                List<string> saveNames = new List<string>();
                for (int i = 0; i < allapps.Count; i++)
                {
                    // 检查是否应该停止
                    if (!isAllUARunning) break;

                    var item = allapps[i];
                    string rename = (item.display?.Replace(" ", " ") ?? item.name?.Replace(" ", " ")) + " " + item.version + ".zip";
                    rename = rename.Replace("\\", " ");
                    rename = rename.Replace("/", " ");
                    rename = rename.Replace(":", " ");
                    rename = rename.Replace("*", " ");
                    rename = rename.Replace("\"", " ");
                    rename = rename.Replace("<", " ");
                    rename = rename.Replace(">", " ");
                    rename = rename.Replace("|", " ");
                    rename = rename.Replace("?", " ");
                    var savename = downloadpath + "\\" + rename;
                    saveNames.Add(rename);
                    if (ZengLiang)
                    {
                        if (File.Exists(savename))
                        {
                            try
                            {
                                if (new FileInfo(savename).Length < 4096)
                                {
                                    string filecontent = File.ReadAllText(savename);
                                    var jsonobj = JObject.Parse(filecontent);
                                    if (jsonobj["code"].ToString() == "401")
                                    {
                                        Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 是坏崽，删除！");
                                        File.Delete(savename);
                                    }
                                    else
                                    {
                                        Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                        continue;
                                    }
                                }
                                else
                                {
                                    Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                    continue;
                                }
                            }
                            catch
                            {
                                Log("当前是增量下崽，第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经在窝里了！");
                                continue;
                            }
                        }
                    }
                    if (string.IsNullOrWhiteSpace(item.package_path))
                    {
                        Log("第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 可能是云崽，不用下崽了！");
                        continue;
                    }
                    for (int trycount = 1; trycount <= 5; trycount++)
                    {
                        // 检查是否应该停止
                        if (!isAllUARunning) break;

                        try
                        {
                            Log("正在下第" + (i + 1) + "/" + allapps.Count + "只崽：" + rename + "！");
                            var res = KaiSton.RequestDown("GET", item.package_path, "");
                            if (res.Length < 4096)
                            {
                                try
                                {
                                    string data = Encoding.UTF8.GetString(res);
                                    var jsonobj = JObject.Parse(data);
                                    if (jsonobj["code"].ToString() == "401")
                                    {
                                        KaiSton.getKey();
                                        continue;
                                    }
                                }
                                catch
                                {
                                }
                            }
                            File.WriteAllBytes(savename, res);
                            downloadedZipFiles.Add(savename);
                            Log("第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 已经下完！");
                            break;
                        }
                        catch (Exception ex)
                        {
                            Log("第" + (i + 1) + "/" + allapps.Count + "只崽 " + rename + " 不肯出窝并说\"" + ex.Message + "\"，重试第" + trycount + "次！");
                        }
                    }
                    // 检查是否应该停止
                    if (!isAllUARunning) break;
                }

                // 检查是否应该停止
                if (!isAllUARunning) return;

                Log("开始整理崽崽...");
                string oldpath = GetOldPath();
                if (!Directory.Exists(oldpath))
                {
                    Directory.CreateDirectory(oldpath);
                }
                int cnt = 0;
                List<string> paths = Directory.GetFiles(downloadpath).ToList();
                foreach (var path in paths)
                {
                    // 检查是否应该停止
                    if (!isAllUARunning) break;

                    string pth = Path.GetFileName(path);
                    if (!saveNames.Contains(pth))
                    {
                        try
                        {
                            File.Move(path, oldpath + pth);
                            Log(pth + "已移动！");
                            cnt++;
                        }
                        catch (Exception ex)
                        {
                            Log("悲，报错了！！！" + ex.Message);
                        }
                    }
                }
                Log("操作完成，移动了" + cnt + "个旧崽！");

                // 检查是否应该停止
                if (!isAllUARunning) return;

                // 更新所有下载的zip文件的日期
                if (downloadedZipFiles.Count > 0)
                {
                    Log("开始更新zip文件日期...");
                    int updateCount = 0;
                    foreach (var zipPath in downloadedZipFiles)
                    {
                        // 检查是否应该停止
                        if (!isAllUARunning) break;

                        UpdateZipFileDate(zipPath);
                        updateCount++;
                    }
                    Log("zip文件日期更新完成，共更新" + updateCount + "个文件！");
                    downloadedZipFiles.Clear();
                }
            }
            catch (Exception ex)
            {
                Log("悲，报错了！！！" + ex.Message);
            }
        }
    }
}
