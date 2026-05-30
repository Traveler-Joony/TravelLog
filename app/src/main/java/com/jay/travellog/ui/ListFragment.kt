package com.jay.travellog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jay.travellog.R
import com.jay.travellog.adapter.TravelAdapter
import com.jay.travellog.data.DBHelper

class ListFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())
        recyclerView = view.findViewById(R.id.recyclerTravel)
        emptyView = view.findViewById(R.id.txtEmpty)

        adapter = TravelAdapter(emptyList()) { record ->
            // Day 6에서 DetailActivity 실행으로 교체합니다.
            Toast.makeText(
                requireContext(),
                "${record.place} 클릭됨 · Day 6에서 상세 화면 연결 예정",
                Toast.LENGTH_SHORT
            ).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 추가/수정 화면(Day 5~)에서 돌아오면 목록이 자동 갱신되도록 onResume에서 로드
        loadData()
    }

    private fun loadData() {
        val records = dbHelper.getAllRecords()
        adapter.updateData(records)

        val isEmpty = records.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
