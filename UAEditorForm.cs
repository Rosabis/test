using KaiosMarketDownloader.Beans;
using KaiosMarketDownloader.utils;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace KaiosMarketDownloader
{
    public partial class UAEditorForm : Form
    {
        private List<UAEntry> uaList;

        public List<UAEntry> UAList => uaList;
        public UAEntry SelectedUAEntry { get; private set; }

        private int dragIndex = -1;
        private bool isDragging = false;

        public UAEditorForm(List<UAEntry> initialList = null, UAEntry selected = null)
        {
            InitializeComponent();
            uaList = initialList ?? new List<UAEntry>();
            
            // 如果列表为空，从文件加载
            if (uaList.Count == 0)
            {
                uaList = LoadUAListFromIni();
            }
            
            // 确保所有项都有 Order
            for (int i = 0; i < uaList.Count; i++)
            {
                if (uaList[i].Order == 0 && i > 0)
                {
                    uaList[i].Order = i;
                }
            }
            
            // 按 Order 排序（保持用户自定义顺序），如果 Order 相同则按名字排序
            uaList = uaList.OrderBy(x => x.Order).ThenBy(x => x.Remark, StringComparer.OrdinalIgnoreCase).ToList();
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
            listBox1.DataSource = null;
            listBox1.DataSource = uaList;
            listBox1.DisplayMember = "Remark";
            listBox1.ValueMember = "UA";
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

            var entry = new UAEntry { Remark = remark, UA = ua, Order = uaList.Count > 0 ? uaList.Max(x => x.Order) + 1 : 0 };
            uaList.Add(entry);
            // 按 Order 排序，如果 Order 相同则按名字排序
            uaList = uaList.OrderBy(x => x.Order).ThenBy(x => x.Remark, StringComparer.OrdinalIgnoreCase).ToList();
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
                    // 更新所有 Order（基于新的列表顺序）
                    for (int i = 0; i < uaList.Count; i++)
                    {
                        uaList[i].Order = i;
                    }
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
            // 保存当前顺序
            UpdateOrderFromListBox();
            SaveUAList();
            this.DialogResult = DialogResult.OK;
            this.Close();
        }

        private void UpdateOrderFromListBox()
        {
            for (int i = 0; i < listBox1.Items.Count; i++)
            {
                if (listBox1.Items[i] is UAEntry entry)
                {
                    entry.Order = i;
                }
            }
        }

        private void listBox1_MouseDown(object sender, MouseEventArgs e)
        {
            if (e.Button == MouseButtons.Left)
            {
                int index = listBox1.IndexFromPoint(e.Location);
                if (index >= 0)
                {
                    dragIndex = index;
                    isDragging = true;
                }
            }
        }

        private void listBox1_MouseMove(object sender, MouseEventArgs e)
        {
            if (isDragging && dragIndex >= 0 && e.Button == MouseButtons.Left)
            {
                if (Math.Abs(e.Y - listBox1.GetItemRectangle(dragIndex).Y) > 5)
                {
                    listBox1.DoDragDrop(listBox1.Items[dragIndex], DragDropEffects.Move);
                    isDragging = false;
                }
            }
        }

        private void listBox1_MouseUp(object sender, MouseEventArgs e)
        {
            isDragging = false;
            dragIndex = -1;
        }

        private void listBox1_DragOver(object sender, DragEventArgs e)
        {
            e.Effect = DragDropEffects.Move;
            Point point = listBox1.PointToClient(new Point(e.X, e.Y));
            int index = listBox1.IndexFromPoint(point);
            
            if (index >= 0)
            {
                // 检查鼠标是在项目的上半部分还是下半部分
                Rectangle itemRect = listBox1.GetItemRectangle(index);
                if (point.Y > itemRect.Top + itemRect.Height / 2)
                {
                    // 鼠标在项目下半部分，插入到下一个位置
                    index++;
                }
                
                if (index != dragIndex && index >= 0 && index <= listBox1.Items.Count)
                {
                    // 高亮显示目标位置（通过选中）
                    if (index < listBox1.Items.Count)
                    {
                        listBox1.SelectedIndex = index;
                    }
                    else
                    {
                        listBox1.SelectedIndex = listBox1.Items.Count - 1;
                    }
                }
            }
            else
            {
                // 如果不在任何项目上，检查是否在列表底部
                if (point.Y > listBox1.GetItemRectangle(listBox1.Items.Count - 1).Bottom)
                {
                    listBox1.SelectedIndex = listBox1.Items.Count - 1;
                }
            }
        }

        private void listBox1_DragDrop(object sender, DragEventArgs e)
        {
            if (dragIndex < 0) return;

            Point point = listBox1.PointToClient(new Point(e.X, e.Y));
            int targetIndex = listBox1.IndexFromPoint(point);
            
            // 如果找不到目标位置，检查是否在列表底部
            if (targetIndex < 0)
            {
                if (listBox1.Items.Count > 0)
                {
                    Rectangle lastRect = listBox1.GetItemRectangle(listBox1.Items.Count - 1);
                    if (point.Y > lastRect.Bottom)
                    {
                        targetIndex = listBox1.Items.Count;
                    }
                    else
                    {
                        targetIndex = listBox1.Items.Count - 1;
                    }
                }
                else
                {
                    targetIndex = 0;
                }
            }
            else
            {
                // 检查鼠标是在项目的上半部分还是下半部分
                Rectangle itemRect = listBox1.GetItemRectangle(targetIndex);
                if (point.Y > itemRect.Top + itemRect.Height / 2)
                {
                    // 鼠标在项目下半部分，插入到下一个位置
                    targetIndex++;
                }
            }
            
            // 确保 targetIndex 在有效范围内
            if (targetIndex > listBox1.Items.Count)
            {
                targetIndex = listBox1.Items.Count;
            }
            if (targetIndex < 0)
            {
                targetIndex = 0;
            }

            // 只有当目标位置与源位置不同时才执行移动
            if (dragIndex != targetIndex && dragIndex >= 0 && dragIndex < uaList.Count && targetIndex >= 0 && targetIndex <= uaList.Count)
            {
                var draggedItem = uaList[dragIndex];
                uaList.RemoveAt(dragIndex);
                
                // 如果目标位置在源位置之后，移除项目后索引会前移，需要调整
                int insertIndex = targetIndex;
                if (targetIndex > dragIndex)
                {
                    insertIndex = targetIndex - 1;
                }
                
                // 确保 insertIndex 在有效范围内
                if (insertIndex < 0) insertIndex = 0;
                if (insertIndex > uaList.Count) insertIndex = uaList.Count;
                
                uaList.Insert(insertIndex, draggedItem);
                
                // 更新所有 Order（基于新的列表顺序）
                for (int i = 0; i < uaList.Count; i++)
                {
                    uaList[i].Order = i;
                }
                
                LoadUAList();
                listBox1.SelectedIndex = insertIndex;
                SaveUAList();
            }

            dragIndex = -1;
            isDragging = false;
        }

        private void SaveUAList()
        {
            try
            {
                SaveUAListToIni(uaList);
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
                List<UAEntry> list = null;
                
                // 首先尝试从 ualist.json 读取（新方式）
                var jsonPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "ualist.json");
                if (System.IO.File.Exists(jsonPath))
                {
                    var json = System.IO.File.ReadAllText(jsonPath);
                    list = JsonConvert.DeserializeObject<List<UAEntry>>(json);
                }
                
                // 如果不存在，尝试从旧的 ini 读取并迁移
                if (list == null || list.Count == 0)
                {
                    var jsonIni = OperateIniFile.ReadIniString("UA", "List", "[]");
                    list = JsonConvert.DeserializeObject<List<UAEntry>>(jsonIni);
                }
                
                if (list == null || list.Count == 0)
                {
                    return new List<UAEntry>();
                }
                
                // 如果某些项没有 Order，按名字排序并分配 Order
                bool needOrderUpdate = list.Any(x => x.Order == 0 && list.IndexOf(x) > 0);
                if (needOrderUpdate)
                {
                    // 按名字排序并分配 Order
                    list = list.OrderBy(x => x.Remark, StringComparer.OrdinalIgnoreCase).ToList();
                    for (int i = 0; i < list.Count; i++)
                    {
                        list[i].Order = i;
                    }
                }
                else
                {
                    // 按 Order 排序
                    list = list.OrderBy(x => x.Order).ToList();
                }
                
                return list;
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
                // 使用 ualist.json 保存，避免 INI 长度限制
                var json = JsonConvert.SerializeObject(list, Formatting.Indented);
                var jsonPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "ualist.json");
                System.IO.File.WriteAllText(jsonPath, json);
            }
            catch
            {
                // 忽略保存错误
            }
        }
    }
}

