using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace KaiosMarketDownloader.Beans
{
    public class Settings
    {
        public int ThreadCount { get; set; }
        public bool Incremental { get; set; }
        public bool IsV3 { get; set; }
        public bool EnableCustomUA { get; set; }

        public List<UAEntry> UAList { get; set; }
        public string SelectedUA { get; set; }

        public Settings()
        {
            ThreadCount = 5;
            Incremental = true;
            IsV3 = true;
            EnableCustomUA = false;
            UAList = new List<UAEntry>();
            SelectedUA = string.Empty;
        }
    }
}

