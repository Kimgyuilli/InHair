package com.example.a2024capstonesample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.a2024capstonesample.databinding.FragmentMainBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 솔루션 버튼 클릭 이벤트 처리
        binding.btnSolution.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SolutionFragment)
        }

        // 솔루션 버튼 클릭 이벤트 처리
        binding.btnDetails.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_DetailFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
