package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.jay.travellog.util.ImageUtils

class ListFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: TravelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    private var sortByDateDesc = true

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
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

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

    private fun toggleSort() {
        sortByDateDesc = !sortByDateDesc
        toast(if (sortByDateDesc) "날짜순으로 정렬" else "이름순으로 정렬")
        loadData()
    }

    private fun confirmDeleteOne(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제")
            .setMessage("'${record.place}' 기록을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                if (dbHelper.deleteRecord(record.no) > 0) {
                    ImageUtils.deleteInternalPhoto(requireContext(), record.photoUri)
                    loadData()
                    toast("삭제되었습니다")
                } else {
                    toast("삭제에 실패했습니다")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmDeleteAll() {
        val all = dbHelper.getAllRecords()
        if (all.isEmpty()) {
            toast("삭제할 기록이 없습니다")
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("전체 삭제")
            .setMessage("모든 여행 기록을 삭제할까요?\n되돌릴 수 없습니다.")
            .setPositiveButton("전체 삭제") { _, _ ->
                if (dbHelper.deleteAll() > 0) {
                    all.forEach { ImageUtils.deleteInternalPhoto(requireContext(), it.photoUri) }
                    loadData()
                    toast("전체 삭제되었습니다")
                } else {
                    toast("삭제에 실패했습니다")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle("앱 정보")
            .setMessage("여행 기록 (Travel Log)\n버전 1.0\n\n모바일 프로그래밍 기말 프로젝트")
            .setPositiveButton("확인", null)
            .setNegativeButton("관리자 모드") { _, _ -> showAdminLogin() }
            .show()
    }

    /** 관리자 인증 다이얼로그. abc / 123 일치 시 관리자 화면으로 이동. */
    private fun showAdminLogin() {
        val view = layoutInflater.inflate(R.layout.dialog_admin_login, null)
        val etId = view.findViewById<EditText>(R.id.etAdminId)
        val etPw = view.findViewById<EditText>(R.id.etAdminPw)

        AlertDialog.Builder(requireContext())
            .setTitle("관리자 인증")
            .setView(view)
            .setPositiveButton("확인") { _, _ ->
                val id = etId.text.toString().trim()
                val pw = etPw.text.toString()
                if (id == ADMIN_ID && pw == ADMIN_PW) {
                    startActivity(Intent(requireContext(), AdminActivity::class.java))
                } else {
                    toast("인증 실패: 아이디 또는 비밀번호가 올바르지 않습니다")
                }
            }
            .setNegativeButton("취소", null)
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

    companion object {
        // 개발/디버그용 소프트 게이트 (실제 보안 아님)
        private const val ADMIN_ID = "abc"
        private const val ADMIN_PW = "123"
    }
}
