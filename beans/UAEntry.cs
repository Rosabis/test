using System.ComponentModel;

namespace KaiosMarketDownloader.Beans
{
    public class UAEntry : INotifyPropertyChanged
    {
        private string _remark;
        public string Remark
        {
            get { return _remark; }
            set
            {
                if (_remark != value)
                {
                    _remark = value;
                    OnPropertyChanged("Remark");
                }
            }
        }

        private string _ua;
        public string UA
        {
            get { return _ua; }
            set
            {
                if (_ua != value)
                {
                    _ua = value;
                    OnPropertyChanged("UA");
                }
            }
        }

        private int _order = 0;
        public int Order
        {
            get { return _order; }
            set
            {
                if (_order != value)
                {
                    _order = value;
                    OnPropertyChanged("Order");
                }
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;
        protected void OnPropertyChanged(string propertyName)
        {
            if (PropertyChanged != null)
            {
                PropertyChanged(this, new PropertyChangedEventArgs(propertyName));
            }
        }

        public override string ToString()
        {
            return Remark;
        }
    }
}

