package dev.edge.apkrenamer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApkAdapter(
    private val onSelectionChanged: () -> Unit = {}
) : RecyclerView.Adapter<ApkAdapter.Holder>() {

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
        holder.bind(data[position], onSelectionChanged)
    }

    override fun getItemCount(): Int = data.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvOldName: TextView = itemView.findViewById(R.id.tvOldName)
        private val tvNewName: TextView = itemView.findViewById(R.id.tvNewName)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(item: ApkItem, onSelectionChanged: () -> Unit) {
            ivIcon.setImageDrawable(item.icon ?: itemView.context.getDrawable(android.R.drawable.sym_def_app_icon))
            tvAppName.text = item.appName
            tvOldName.text = "当前: ${item.currentName}"
            tvNewName.text = "新名: ${item.plannedName}"
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = item.isSelected
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onSelectionChanged()
            }
        }
    }
}
