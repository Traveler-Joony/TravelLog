package com.jay.travellog.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jay.travellog.R
import com.jay.travellog.model.TravelRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 관리자 화면용 어댑터. 레코드의 모든 원시 필드 + 타임스탬프를 그대로 보여준다. */
class AdminAdapter(
    private val onEdit: (TravelRecord) -> Unit,
    private val onDelete: (TravelRecord) -> Unit
) : RecyclerView.Adapter<AdminAdapter.VH>() {

    private var items: List<TravelRecord> = emptyList()
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun submit(list: List<TravelRecord>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val fields: TextView = view.findViewById(R.id.tvAdminFields)
        val btnEdit: Button = view.findViewById(R.id.btnAdminEdit)
        val btnDelete: Button = view.findViewById(R.id.btnAdminDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]

        val created = if (r.createdAt > 0L) fmt.format(Date(r.createdAt)) else "—"
        val updated = if (r.updatedAt > 0L) fmt.format(Date(r.updatedAt)) else "—"
        val coord = if (r.latitude != null && r.longitude != null)
            "%.5f, %.5f".format(r.latitude, r.longitude) else "없음"
        val photo = if (!r.photoUri.isNullOrBlank()) r.photoUri else "없음"
        val memo = if (r.memo.isBlank()) "—" else r.memo

        holder.fields.text = buildString {
            appendLine("no         : ${r.no}")
            appendLine("place      : ${r.place}")
            appendLine("visit_date : ${r.visitDate}")
            appendLine("memo       : $memo")
            appendLine("lat/lng    : $coord")
            appendLine("photo_uri  : $photo")
            appendLine("created_at : $created")
            append("updated_at : $updated")
        }

        holder.btnEdit.setOnClickListener { onEdit(r) }
        holder.btnDelete.setOnClickListener { onDelete(r) }
    }

    override fun getItemCount(): Int = items.size
}
