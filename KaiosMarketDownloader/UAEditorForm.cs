using KaiosMarketDownloader.Beans;
using KaiosMarketDownloader.utils;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Forms;

namespace KaiosMarketDownloader
{
    public partial class UAEditorForm : Form
    {
        private List<UAEntry> uaList;

        public List<UAEntry> UAList => uaList;
        public UAEntry SelectedUAEntry { get; private set; }

        public UAEditorForm(List<UAEntry> initialList = null, UAEntry selected = null)
        {
            InitializeComponent();
            uaList = initialList ?? new List<UAEntry>();
            LoadUAList();
            if (selected != null)
            {
                listBox1.SelectedItem = selected;
            }
            else if (listBox1.Items.Count > 0)
            {
                listBox1.SelectedIndex = 0;
            }
        }

        private void LoadUAList()
        {
            listBox1.DisplayMember = "Remark";
            listBox1.ValueMember = "UA";
            listBox1.DataSource = null;
            listBox1.DataSource = uaList;
            UpdateEditFields();
        }

        private void UpdateEditFields()
        {
            if (listBox1.SelectedItem is UAEntry entry)
            {
                SelectedUAEntry = entry;
                textBoxRemark.Text = entry.Remark ?? string.Empty;
                textBoxUA.Text = entry.UA ?? string.Empty;
            }
            else
            {
                SelectedUAEntry = null;
                textBoxRemark.Text = string.Empty;
                textBoxUA.Text = string.Empty;
            }
        }

        private void listBox1_SelectedIndexChanged(object sender, EventArgs e)
        {
            UpdateEditFields();
        }

        private void buttonAdd_Click(object sender, EventArgs e)
        {
            var remark = textBoxRemark.Text?.Trim();
            var ua = textBoxUA.Text?.Trim();

            if (string.IsNullOrEmpty(remark) || string.IsNullOrEmpty(ua))
            {
                MessageBox.Show("备注和 UA 内容不能为空！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            if (uaList.Any(x => x.Remark == remark))
            {
                MessageBox.Show("该备注已存在，请更换。", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            var entry = new UAEntry { Remark = remark, UA = ua };
            uaList.Add(entry);
            LoadUAList();
            listBox1.SelectedItem = entry;
            SaveUAList();
        }

        private void buttonSave_Click(object sender, EventArgs e)
        {
            if (SelectedUAEntry == null)
            {
                MessageBox.Show("请先选择要编辑的项！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var remark = textBoxRemark.Text?.Trim();
            var ua = textBoxUA.Text?.Trim();

            if (string.IsNullOrEmpty(remark) || string.IsNullOrEmpty(ua))
            {
                MessageBox.Show("备注和 UA 内容不能为空！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            if (uaList.Any(x => x != SelectedUAEntry && x.Remark == remark))
            {
                MessageBox.Show("该备注已存在，请更换。", "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            SelectedUAEntry.Remark = remark;
            SelectedUAEntry.UA = ua;
            LoadUAList();
            listBox1.SelectedItem = SelectedUAEntry;
            SaveUAList();
        }

        private void buttonDelete_Click(object sender, EventArgs e)
        {
            if (listBox1.SelectedItem is UAEntry entry)
            {
                if (MessageBox.Show($"确定要删除：{entry.Remark} ?", "确认",
                    MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes)
                {
                    uaList.Remove(entry);
                    LoadUAList();
                    if (listBox1.Items.Count > 0)
                    {
                        listBox1.SelectedIndex = 0;
                    }
                    SaveUAList();
                }
            }
            else
            {
                MessageBox.Show("请先选择要删除的项！", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
        }

        private void buttonOK_Click(object sender, EventArgs e)
        {
            SaveUAList();
            this.DialogResult = DialogResult.OK;
            this.Close();
        }

        private void SaveUAList()
        {
            try
            {
                var json = JsonConvert.SerializeObject(uaList);
                OperateIniFile.WriteIniString("UA", "List", json);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"保存 UA 列表失败：{ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        public static List<UAEntry> LoadUAListFromIni()
        {
            try
            {
                var json = OperateIniFile.ReadIniString("UA", "List", "[]");
                var list = JsonConvert.DeserializeObject<List<UAEntry>>(json);
                return list ?? new List<UAEntry>();
            }
            catch
            {
                return new List<UAEntry>();
            }
        }

        public static void SaveUAListToIni(List<UAEntry> list)
        {
            try
            {
                var json = JsonConvert.SerializeObject(list);
                OperateIniFile.WriteIniString("UA", "List", json);
            }
            catch
            {
                // 忽略保存错误
            }
        }
    }
}

