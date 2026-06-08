package com.jay.travellog.adapter

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jay.travellog.R
import com.jay.travellog.model.TravelRecord
import com.jay.travellog.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TravelAdapter(
    private var items: List<TravelRecord>,
    private val onItemClick: (TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.VH>() {

    // 이미지 로딩용 스코프 (메인 스레드에서 결과 반영, RecyclerView 분리 시 취소)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 컨텍스트 메뉴(롱클릭) 대상 위치
    var selectedPosition = -1
        private set

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {

        val thumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        val progress: ProgressBar = itemView.findViewById(R.id.progressThumb)
        val place: TextView = itemView.findViewById(R.id.txtPlace)
        val date: TextView = itemView.findViewById(R.id.txtDate)
        var loadJob: Job? = null

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
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
        holder.itemView.setOnClickListener { onItemClick(record) }

        // 이전 로딩 취소 (재활용된 뷰에 엉뚱한 이미지가 뜨는 것 방지)
        holder.loadJob?.cancel()

        if (record.photoUri.isNullOrBlank()) {
            holder.progress.visibility = View.GONE
            holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder)
            return
        }

        // 로딩 시작: ProgressBar 표시, 이미지 비우기
        holder.progress.visibility = View.VISIBLE
        holder.thumbnail.setImageDrawable(null)

        holder.loadJob = scope.launch {
            val bmp = ImageUtils.decodeSampledBitmap(
                holder.itemView.context, record.photoUri!!, 200, 200
            )
            // 여기는 다시 메인 스레드. 위에서 cancel됐으면 이 블록은 실행되지 않음.
            holder.progress.visibility = View.GONE
            if (bmp != null) {
                holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.thumbnail.setImageBitmap(bmp)
            } else {
                holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                holder.thumbnail.setImageResource(R.drawable.ic_image_placeholder)
            }
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.loadJob?.cancel()
        holder.thumbnail.setImageDrawable(null)
        holder.progress.visibility = View.GONE
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.cancel()   // 화면을 떠나면 진행 중인 로딩 모두 취소
    }

    override fun getItemCount(): Int = items.size

    fun getItemAt(position: Int): TravelRecord? = items.getOrNull(position)

    fun updateData(newItems: List<TravelRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}
