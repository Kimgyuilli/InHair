package com.example.InHair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InHair.Room.MyAppDatabase
import com.example.InHair.Room.MyEntity
import com.example.InHair.databinding.DataRoomBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomTest : Fragment() {
    private var _binding: DataRoomBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: MyAppDatabase
    private lateinit var adapter: MyEntityAdapter
    private val entityList = mutableListOf<MyEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataRoomBinding.inflate(inflater, container, false)
        database = MyAppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        setupDeleteButton()
        loadData()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = MyEntityAdapter(entityList) {
            // 개별 항목 삭제 로직 (필요한 경우)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupDeleteButton() {
        binding.deleteButton.setOnClickListener {
            deleteAllEntities()
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            val entities = database.myDao().getAllEntities()
            withContext(Dispatchers.Main) {
                entityList.clear()
                entityList.addAll(entities)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteAllEntities() {
        CoroutineScope(Dispatchers.IO).launch {
            database.myDao().deleteAll()
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}