package com.example.InHair

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.InHair.Room.MyAppDatabase
import com.example.InHair.Room.MyEntity
import com.example.InHair.databinding.FragmentDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: MyAppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        database = MyAppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAndDisplayData()
    }

    private fun loadAndDisplayData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val entityList = database.myDao().getAllEntities()
            withContext(Dispatchers.Main) {
                updateUI(entityList)
            }
        }
    }

    private fun updateUI(entityList: List<MyEntity>) {
        if (entityList.isEmpty() || _binding == null) return

        Log.d("DetailFragment", "현재 데이터베이스에 저장된 데이터:")
        entityList.forEachIndexed { index, entity ->
            Log.d("DetailFragment", "[$index] 날짜: ${entity.date}, 경로: ${entity.imagePath}, 양호 수치: ${entity.score}, 포맷된 수치: ${entity.formattedScore}")
        }

        val groupedData = entityList.groupBy { it.date }
        val averageData = groupedData.mapValues { entry ->
            entry.value.map { it.score }.average()
        }.toSortedMap(compareByDescending { it })

        binding.dateDetailList.removeAllViews()

        for ((date, averageScore) in averageData) {
            val dateDetailButton = Button(requireContext()).apply {
                text = "$date\n 양호 수치: ${"%.2f".format(averageScore)}\n 주의 수치: ${"%.2f".format(1.0 - averageScore)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(24, 24, 24, 24)
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
                setOnClickListener {
                    showDetailsDialog(date, groupedData[date].orEmpty())
                }
            }
            binding.dateDetailList.addView(dateDetailButton)
        }
    }

    private fun showDetailsDialog(date: String, dataList: List<MyEntity>) {
        // 날짜 형식을 변환 ("2024-11-12" -> "2024년 11월 12일")
        val formattedDate = try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
            val parsedDate = originalFormat.parse(date)
            parsedDate?.let { targetFormat.format(it) } ?: date // 변환에 실패하면 원본 그대로 사용
        } catch (e: Exception) {
            date // 예외 발생 시 원본 그대로 사용
        }

        // AlertDialog 커스텀 뷰 생성
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo_details, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = object : RecyclerView.Adapter<PhotoDetailViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoDetailViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_detail, parent, false)
                return PhotoDetailViewHolder(view)
            }

            override fun onBindViewHolder(holder: PhotoDetailViewHolder, position: Int) {
                val entity = dataList[position]
                holder.bind(entity)
            }

            override fun getItemCount(): Int = dataList.size
        }

        AlertDialog.Builder(requireContext())
            .setTitle(formattedDate)
            .setView(dialogView)
            .setPositiveButton("확인", null)
            .show()
    }

    inner class PhotoDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val tvGoodScore: TextView = itemView.findViewById(R.id.tvGoodScore)
        private val tvCautionScore: TextView = itemView.findViewById(R.id.tvCautionScore)

        fun bind(entity: MyEntity) {
            imageView.setImageURI(Uri.parse(entity.imagePath))
            val goodProbability = entity.score * 100
            val cautionProbability = (1 - entity.score) * 100

            tvGoodScore.text = String.format("양호 수치: %.2f%%", goodProbability)
            tvCautionScore.text = String.format("주의 수치: %.2f%%", cautionProbability)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
