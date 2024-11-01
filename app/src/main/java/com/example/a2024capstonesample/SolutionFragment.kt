package com.example.a2024capstonesample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.a2024capstonesample.Room.MyAppDatabase
import com.example.a2024capstonesample.Room.MyEntity
import com.example.a2024capstonesample.databinding.FragmentSolutionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SolutionFragment : Fragment() {
    private var _binding: FragmentSolutionBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: MyAppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolutionBinding.inflate(inflater, container, false)
        database = MyAppDatabase.getDatabase(requireContext()) // 데이터베이스 인스턴스 초기화
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAndDisplayData() // 데이터 로드 및 UI 업데이트
    }

    private fun loadAndDisplayData() {
        CoroutineScope(Dispatchers.IO).launch {
            val entityList = database.myDao().getAllEntities() // 모든 데이터베이스 항목 불러오기
            withContext(Dispatchers.Main) {
                updateUI(entityList)
            }
        }
    }

    // UI 업데이트 함수
    private fun updateUI(entityList: List<MyEntity>) {
        if (entityList.isEmpty() || _binding == null) return

        // 날짜별로 그룹화하여 평균 수치 계산
        val groupedData = entityList.groupBy { it.date }
        val averageData = groupedData.mapValues { entry ->
            val scores = entry.value.map { it.score }
            scores.average() // 평균 수치를 저장
        }.toSortedMap(compareByDescending { it }) // 날짜 내림차순 정렬

        // 기존 뷰 제거 후 새로 추가
        binding.dateSolutionList.removeAllViews()

        for ((date, averageScore) in averageData) {
            val message = if (averageScore >= 0.5) "양호합니다." else "주의가 필요합니다."

            // 카드뷰 생성 및 설정
            val cardView = CardView(requireContext()).apply {
                radius = 12f
                cardElevation = 6f
                setContentPadding(24, 24, 24, 24)
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
            }

            val dateTextView = TextView(requireContext()).apply {
                text = "$date\n양호 수치가 ${"%.2f".format(averageScore)}으로 $message"
                textSize = 16f
            }

            // 카드뷰에 추가
            cardView.addView(dateTextView)
            binding.dateSolutionList.addView(cardView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
