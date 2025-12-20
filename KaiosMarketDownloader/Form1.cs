using KaiosMarketDownloader.Beans;
using KaiosMarketDownloader.utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Runtime.Remoting.Messaging;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml.Linq;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;

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
            
            // 如果 UA 列表为空，添加默认的 V2 和 V3 UA
            if (uaList.Count == 0)
            {
                uaList.Add(new UAEntry { Remark = "KaiOS V3 (默认)", UA = KaiSton.V3Str, Order = 0 });
                uaList.Add(new UAEntry { Remark = "KaiOS V2 (默认)", UA = KaiSton.V2Str, Order = 1 });
                UAEditorForm.SaveUAListToIni(uaList);
            }
            
            UpdateUAComboBox();
            comboBoxUA.Visible = true;
            labelUA.Visible = true;
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
        private bool V3 = true;
        Thread thread = null;
        private bool CustomUA = true;
        private List<UAEntry> uaList = new List<UAEntry>();
        private string selectedUARemark = string.Empty;
        private bool SelectedIsDefaultV3OrV2(UAEntry selected)
        {
            if (selected == null || string.IsNullOrEmpty(selected.UA)) return false;
            var ua = selected.UA.Trim();
            return ua == KaiSton.V3Str || ua == KaiSton.V2Str;
        }
        private bool SelectedIsV3(UAEntry selected)
        {
            try
            {
                var js = Newtonsoft.Json.Linq.JObject.Parse(selected.UA);
                var ver = js["dev"]?["version"]?.ToString() ?? "";
                return ver.StartsWith("3");
            }
            catch
            {
                return true;
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
            var selectedUA = comboBoxUA?.SelectedItem as UAEntry;
            if (selectedUA != null && !string.IsNullOrEmpty(selectedUA.UA))
            {
                try
                {
                    var uaJson = JObject.Parse(selectedUA.UA);
                    KaiSton.settingsStr = selectedUA.UA;
                    KaiSton.jsonSetting = uaJson;
                }
                catch
                {
                    var ua = OperateIniFile.ReadIniString("setting", "ua", KaiSton.V3Str);
                    KaiSton.settingsStr = ua;
                    KaiSton.jsonSetting = JObject.Parse(KaiSton.settingsStr);
                }
                V3 = SelectedIsV3(selectedUA);
                CustomUA = !SelectedIsDefaultV3OrV2(selectedUA);
            }
            else
            {
                KaiSton.settingsStr = KaiSton.V3Str;
                KaiSton.jsonSetting = JObject.Parse(KaiSton.settingsStr);
                V3 = true;
                CustomUA = false;
            }


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
        int now = 0;
        int count = 0;
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
                            Log("第" + Thread.CurrentThread.Name + "只母鸡，正在努力下第" + (i + 1) + "只崽,崽的名字叫：" + rename + "！");

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
                            }

                            Log("第" + Thread.CurrentThread.Name + "只母鸡，" + "第" + (i + 1) + "只崽 " + rename + " 已经下完！");
                            break;
                        }
                        catch (Exception ex)
                        {
                            Log("第" + Thread.CurrentThread.Name + "只母鸡，" + "第" + (i + 1) + "只崽 " + rename + " 不肯出窝并说\"" + ex.Message + "\"，重试第" + trycount + "次！");
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
                Log("开始下崽...");
                KaiSton.getKey();
                Log("正在寻找母鸡...");
                var ret = "";

                if (V3)
                {
                    ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_3.1.0.0&locale=zh-CN", "");//&category=30&page_num=1&page_size=20 
                }
                else
                {

                    ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_2.5.4.1&locale=zh-CN", "");//&category=30&page_num=1&page_size=20 
                }
                var retjson = JObject.Parse(ret);
                var apps = retjson.ToString(Formatting.Indented);
                File.WriteAllText("appsdata_" + KaiSton.model + ".json", apps);
                var allapps = JsonConvert.DeserializeObject<List<KaiosStoneItem>>(retjson["apps"].ToString());

                Log("母鸡已经找到，共有" + allapps.Count + "个崽，开始准备下崽...");
                string downloadpath = Directory.GetCurrentDirectory() + "\\eggs\\";
                if (CustomUA)
                {
                    var suffix = MakeSafeFolderToken(selectedUARemark);
                    downloadpath = Directory.GetCurrentDirectory() + "\\eggs_" + suffix + "\\";
                }
                else
                {
                    downloadpath = Directory.GetCurrentDirectory() + (V3 ? "\\eggs_v3\\" : "\\eggs_v2\\");
                }

                try
                {

                    if (!Directory.Exists(downloadpath))
                    {
                        Directory.CreateDirectory(downloadpath);
                    }
                }
                catch (Exception ex)
                {

                }
                downlist.Clear();
                count = allapps.Count;
                UpdateLabel();
                for (int i = 0; i < allapps.Count; i++)
                {
                    now = i + 1;

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
                                        Log("当前是增量下崽，第" + (i + 1) + "只崽 " + rename + " 是坏崽，删除！");
                                        File.Delete(savename);
                                    }
                                    else
                                    {
                                        Log("当前是增量下崽，第" + (i + 1) + "只崽 " + rename + " 已经在窝里了！");
                                        continue;
                                    }
                                }
                                else
                                {
                                    Log("当前是增量下崽，第" + (i + 1) + "只崽 " + rename + " 已经在窝里了！");
                                    continue;
                                }
                            }
                            catch (Exception ex)
                            {
                                Log("当前是增量下崽，第" + (i + 1) + "只崽 " + rename + " 已经在窝里了！");
                                continue;
                            }
                        }
                    }
                    if (string.IsNullOrWhiteSpace(item.package_path))
                    {
                        Log("第" + (i + 1) + "只崽 " + rename + " 可能是云崽，不用下崽了！");
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
                var selectedUA = comboBoxUA?.SelectedItem as UAEntry;
                if (selectedUA != null && !string.IsNullOrEmpty(selectedUA.UA))
                {
                    try
                    {
                        var uaJson = JObject.Parse(selectedUA.UA);
                        KaiSton.settingsStr = selectedUA.UA;
                        KaiSton.jsonSetting = uaJson;
                    }
                    catch
                    {
                        var ua = OperateIniFile.ReadIniString("setting", "ua", KaiSton.V3Str);
                        KaiSton.settingsStr = ua;
                        KaiSton.jsonSetting = JObject.Parse(KaiSton.settingsStr);
                    }
                    V3 = SelectedIsV3(selectedUA);
                    CustomUA = !SelectedIsDefaultV3OrV2(selectedUA);
                }
                else
                {
                    KaiSton.settingsStr = KaiSton.V3Str;
                    KaiSton.jsonSetting = JObject.Parse(KaiSton.settingsStr);
                    V3 = true;
                    CustomUA = false;
                }


                if (isrunning)
                {

                    Log("正在整理崽崽，请稍等...");
                    return;
                }

                if (button1.Text == "开始下崽")
                {
                    isrunning = true;
                    Task.Run(() =>
                    {
                        try
                        {
                            Log("开始整理崽崽...");
                            KaiSton.getKey();
                            Log("正在寻找崽崽的索引...");
                            var ret = "";

                            if (V3)
                            {
                                ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_3.1.0.0&locale=zh-CN", "");//&category=30&page_num=1&page_size=20 
                            }
                            else
                            {

                                ret = KaiSton.Request("GET", "/v3.0/apps?software=KaiOS_2.5.4.1&locale=zh-CN", "");//&category=30&page_num=1&page_size=20 
                            }
                            var retjson = JObject.Parse(ret);
                            var apps = retjson.ToString(Formatting.Indented);
                            File.WriteAllText("appsdata_" + KaiSton.model + ".json", apps);
                            var allapps = JsonConvert.DeserializeObject<List<KaiosStoneItem>>(retjson["apps"].ToString());

                            string downloadpath = Directory.GetCurrentDirectory() + "\\eggs\\";
                            string oldpath = Directory.GetCurrentDirectory() + "\\eggs_old\\";
                            if (CustomUA)
                            {
                                var suffix = MakeSafeFolderToken(selectedUARemark);
                                downloadpath = Directory.GetCurrentDirectory() + "\\eggs_" + suffix + "\\";
                                oldpath = Directory.GetCurrentDirectory() + "\\eggs_" + suffix + "_old\\";
                            }
                            else
                            {
                                downloadpath = Directory.GetCurrentDirectory() + (V3 ? "\\eggs_v3\\" : "\\eggs_v2\\");
                                oldpath = Directory.GetCurrentDirectory() + (V3 ? "\\eggs_v3_old\\" : "\\eggs_v2_old\\");
                            }
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
                                now = i + 1;

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
            editor.ShowDialog();
            
            // 无论是否点击确定，都重新加载/刷新列表，因为在编辑器中可能已经保存了修改
            // 重新从 INI 读取以确保同步，或者直接信任内存中的 uaList (它是引用传递)
            // 为了保险，如果编辑器中进行了保存操作，内存中的 uaList 已经更新了
            // 但为了防止多开或其他情况，这里可以选择重新读取，但考虑到是单用户操作，直接刷新 ComboBox 即可
            
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
    }
}
