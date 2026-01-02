using System;
using System.Windows.Forms;

namespace KaiosMarketDownloader
{
    internal static class Program
    {
        /// <summary>
        /// 应用程序的主入口点。
        /// </summary>
        [STAThread]
        static void Main()
        {
            // ServicePointManager 在 .NET 8 中已废弃，默认支持 TLS 1.2+
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new Form1());
        }
    }
}
