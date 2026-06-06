package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jay.travellog.R
import com.jay.travellog.adapter.TravelAdapter
import com.jay.travellog.data.DBHelper
import com.jay.travellog.model.TravelRecord

class ListFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private var sortByDateDesc = true   // true: 날짜순(최신), false: 이름순

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
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_NO, record.no)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(requireContext(), AddEditActivity::class.java))
        }

        setupOptionsMenu()
    }

    // ───────── 옵션 메뉴 (MenuProvider, 최신 API) ─────────
    private fun setupOptionsMenu() {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.list_options_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.action_sort -> { toggleSort(); true }
                    R.id.action_delete_all -> { confirmDeleteAll(); true }
                    R.id.action_about -> { showAbout(); true }
                    else -> false
                }
            // viewLifecycleOwner + RESUMED → 목록 화면일 때만 메뉴가 보임
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // ───────── 컨텍스트 메뉴 (롱클릭) 선택 처리 ─────────
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val record = adapter.getItemAt(adapter.selectedPosition)
            ?: return super.onContextItemSelected(item)
        return when (item.itemId) {
            R.id.context_edit -> {
                val intent = Intent(requireContext(), AddEditActivity::class.java)
                intent.putExtra(AddEditActivity.EXTRA_NO, record.no)
                startActivity(intent)
                true
            }
            R.id.context_delete -> { confirmDeleteOne(record); true }
            else -> super.onContextItemSelected(item)
        }
    }

    // ───────── 정렬 ─────────
    private fun toggleSort() {
        sortByDateDesc = !sortByDateDesc
        toast(if (sortByDateDesc) "날짜순으로 정렬" else "이름순으로 정렬")
        loadData()
    }

    // ───────── 삭제 (AlertDialog 확인) ─────────
    private fun confirmDeleteOne(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제")
            .setMessage("'${record.place}' 기록을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                dbHelper.deleteRecord(record.no)
                loadData()
                toast("삭제되었습니다")
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmDeleteAll() {
        if (dbHelper.getAllRecords().isEmpty()) {
            toast("삭제할 기록이 없습니다")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("전체 삭제")
            .setMessage("모든 여행 기록을 삭제할까요?\n되돌릴 수 없습니다.")
            .setPositiveButton("전체 삭제") { _, _ ->
                dbHelper.deleteAll()
                loadData()
                toast("전체 삭제되었습니다")
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle("앱 정보")
            .setMessage("여행 기록 (Travel Log)\n버전 1.0\n\n모바일 프로그래밍 기말 프로젝트")
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val records = dbHelper.getAllRecords(sortByDateDesc)
        adapter.updateData(records)

        val isEmpty = records.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
