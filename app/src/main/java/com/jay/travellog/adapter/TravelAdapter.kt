package com.jay.travellog.adapter

import android.net.Uri
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jay.travellog.R
import com.jay.travellog.model.TravelRecord

class TravelAdapter(
    private var items: List<TravelRecord>,
    private val onItemClick: (TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.VH>() {

    // 컨텍스트 메뉴(롱클릭) 대상이 된 항목의 위치
    var selectedPosition = -1
        private set

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {

        val thumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        val place: TextView = itemView.findViewById(R.id.txtPlace)
        val date: TextView = itemView.findViewById(R.id.txtDate)

        init {
            // 이 뷰를 롱클릭하면 컨텍스트 메뉴가 뜨도록 등록
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            // 어떤 항목을 길게 눌렀는지 위치를 기록 (Fragment에서 꺼내 씀)
            selectedPosition = bindingAdapterPosition
            MenuInflater(v.context).inflate(R.menu.list_context_menu, menu)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val record = items[position]
        holder.place.text = record.place
        holder.date.text = record.visitDate

        // 사진: 지금은 단순 로딩. Day 11에서 코루틴 비동기 로딩으로 교체합니다.
        if (record.photoUri.isNullOrBlank()) {
            holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder)
        } else {
            try {
                holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.thumbnail.setImageURI(Uri.parse(record.photoUri))
            } catch (e: Exception) {
                holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder)
            }
        }

        holder.itemView.setOnClickListener { onItemClick(record) }
    }

    override fun getItemCount(): Int = items.size

    /** 위치로 항목 조회 (컨텍스트 메뉴 처리용). 범위를 벗어나면 null. */
    fun getItemAt(position: Int): TravelRecord? = items.getOrNull(position)

    /** DB에서 새로 읽은 목록으로 갱신. (DiffUtil 적용은 추후 최적화 과제) */
    fun updateData(newItems: List<TravelRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}
