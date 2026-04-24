package dev.edge.apkrenamer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApkAdapter : RecyclerView.Adapter<ApkAdapter.Holder>() {

    private val data = mutableListOf<ApkItem>()

    fun submit(items: List<ApkItem>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_apk, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvInfo: TextView = itemView.findViewById(R.id.tvInfo)

        fun bind(item: ApkItem) {
            tvFileName.text = item.file.name ?: "(unknown)"
            tvInfo.text =
                "app=${item.appName}\npkg=${item.packageName}\nverName=${item.versionName}\nverCode=${item.versionCode}"
        }
    }
}
