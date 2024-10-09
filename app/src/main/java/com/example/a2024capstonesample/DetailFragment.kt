package com.example.a2024capstonesample

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.a2024capstonesample.databinding.FragmentDetailBinding
import com.example.a2024capstonesample.data.PhotoDataManager
import com.example.a2024capstonesample.data.PhotoData

class DetailFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI() // Fragment가 보일 때마다 UI 업데이트
    }


    // 세부 기록 페이지 업데이트
    private fun updateUI() {
        val photoDataList = PhotoDataManager.getAllPhotoData()
        if (photoDataList.isEmpty() || _binding == null) return

        // 로그로 리스트의 모든 데이터 출력
        Log.d("DetailFragment", "현재 PhotoDataManager에 저장된 데이터:")
        photoDataList.forEachIndexed { index, photoData ->
            Log.d("DetailFragment", "[$index] 날짜: ${photoData.date}, 경로: ${photoData.photoPath}, 수치: ${photoData.measurement}")
        }

        // 날짜별로 그룹화하여 평균 수치 계산
        val groupedData = photoDataList.groupBy { it.date }
        val averageData = groupedData.mapValues { entry ->
            val measurements = entry.value.map { it.measurement }
            measurements.average()
        }

        // 기존 뷰 제거 후 새로 추가
        binding.dateDetailList.removeAllViews()

        for ((date, average) in averageData) {
            // 클릭 가능한 버튼 생성
            val button = Button(requireContext()).apply {
                text = "$date - 평균 수치: ${"%.2f".format(average)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setOnClickListener {
                    // 버튼 클릭 시 할 작업 (추후 상세 정보 보여주기 등 추가 가능)
                }
            }

            binding.dateDetailList.addView(button) // 버튼 추가
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}