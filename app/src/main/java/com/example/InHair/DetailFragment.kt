package com.example.InHair

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.InHair.Room.MyAppDatabase
import com.example.InHair.Room.MyEntity
import com.example.InHair.databinding.FragmentDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: MyAppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        database = MyAppDatabase.getDatabase(requireContext()) // 데이터베이스 인스턴스 초기화
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAndDisplayData() // Fragment가 보일 때마다 UI 업데이트
    }

    private fun loadAndDisplayData() {
        CoroutineScope(Dispatchers.IO).launch {
            val entityList = database.myDao().getAllEntities() // 모든 데이터베이스 항목 불러오기
            withContext(Dispatchers.Main) {
                updateUI(entityList)
            }
        }
    }

    // 세부 기록 페이지 업데이트
    private fun updateUI(entityList: List<MyEntity>) {
        if (entityList.isEmpty() || _binding == null) return

        // 로그로 리스트의 모든 데이터 출력
        Log.d("DetailFragment", "현재 데이터베이스에 저장된 데이터:")
        entityList.forEachIndexed { index, entity ->
            Log.d("DetailFragment", "[$index] 날짜: ${entity.date}, 경로: ${entity.imagePath}, 양호 수치: ${entity.score}, 포맷된 수치: ${entity.formattedScore}")
        }

        // 날짜별로 그룹화하여 평균 수치 계산
        val groupedData = entityList.groupBy { it.date }
        val averageData = groupedData.mapValues { entry ->
            val scores = entry.value.map { it.score }
            scores.average() // 평균 수치를 저장
        }.toSortedMap(compareByDescending { it }) // 날짜 내림차순 정렬

        // 기존 뷰 제거 후 새로 추가
        binding.dateDetailList.removeAllViews()

        for ((date, averageScore) in averageData) {
            // 클릭 가능한 버튼 생성
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
                    // 버튼 클릭 시 할 작업 (추후 상세 정보 보여주기 등 추가 가능)
                }
            }

            binding.dateDetailList.addView(dateDetailButton) // 버튼 추가
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
