package com.example.InHair


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.InHair.databinding.FragmentCamerasurfaceBinding
import java.io.ByteArrayOutputStream


class CamSfFragment : Fragment() {
    // 화면 구성요소들
    private var _binding: FragmentCamerasurfaceBinding? = null
    private val binding get() = _binding!!
    private var camera: Camera? = null // 카메라 객체
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentZoom = 0 // 현재 줌 값 저장
    private var maxZoom = 1f


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCamerasurfaceBinding.inflate(inflater, container, false)
        return binding.root
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                // 줌 인 및 줌 아웃을 위한 비율 조정
                if (scaleFactor > 1f) { // 줌 인
                    if (currentZoom < maxZoom) {
                        currentZoom++
                    }
                } else if (scaleFactor < 1f) { // 줌 아웃
                    if (currentZoom > 0) {
                        currentZoom--
                    }
                }
                setCameraZoom(currentZoom.toFloat())
                return true
            }
        })

        // 터치 이벤트 리스너 설정
        binding.cameraPreviewMain.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

        // SurfaceHolder 설정
        val surfaceHolder = binding.cameraPreviewMain.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initializeCamera()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // SurfaceView가 변경되었을 때 처리
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }
        })

        // 촬영 버튼 클릭 이벤트 처리
        binding.buttonMainCapture.setOnClickListener {
            Log.d("CameraDebug", "btnSurFace")
            takeCapture() // 촬영 후 즉시 메인 페이지로 이동하도록 수정
        }

    }

    private fun initializeCamera() {
        try {
            camera = Camera.open()
            val params = camera?.parameters

            // 줌 지원 여부 확인 및 최대 줌 값 설정
            if (params?.isZoomSupported == true) {
                maxZoom = params.maxZoom.toFloat()
            } else {
                maxZoom = 1f
            }

            // 카메라 프리뷰 회전 설정
            val info = CameraInfo()
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info)
            val rotation = requireActivity().windowManager.defaultDisplay.rotation
            val degrees = when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
            val result = (info.orientation - degrees + 360) % 360
            camera?.setDisplayOrientation(result)

            // 프리뷰 시작
            camera?.setPreviewDisplay(binding.cameraPreviewMain.holder)
            camera?.startPreview()
        } catch (e: Exception) {
            Log.e("CamSfFragment", "Error initializing camera: ${e.message}")
        }
    }

    private fun setCameraZoom(zoomLevel: Float) {
        val params = camera?.parameters
        if (params?.isZoomSupported == true) {
            params.zoom = zoomLevel.toInt().coerceIn(0, params.maxZoom)
            camera?.parameters = params
            camera?.startPreview()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public fun takeCapture() {
        if (camera != null) {
            try {
                camera!!.takePicture(
                    { /* 셔터 소리 처리 */ },
                    null,
                    Camera.PictureCallback { data, camera ->
                        try {
                            // 이미지 데이터를 비트맵으로 변환
                            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

                            // 이미지 회전 처리
                            val matrix = Matrix()
                            matrix.postRotate(90f) // 시계 방향으로 90도 회전
                            bitmap = Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.width,
                                bitmap.height,
                                matrix,
                                true
                            )

                            // 회전된 비트맵을 다시 ByteArray로 변환
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            val rotatedData = stream.toByteArray()

                            // Bundle에 회전된 이미지 데이터 담기
                            val result = Bundle().apply {
                                putByteArray("photo_data", rotatedData)
                            }

                            // 결과 설정 및 네비게이션
                            parentFragmentManager.setFragmentResult("camera_result", result)

                            // MainFragment로 돌아가기
                            findNavController().navigateUp()


                        } catch (e: Exception) {
                            Log.e("CameraDebug", "이미지 처리 중 오류 발생: ${e.message}")
                            Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
            } catch (e: Exception) {
                Log.e("CameraDebug", "사진 촬영 중 오류 발생: ${e.message}")
                Toast.makeText(requireContext(), "카메라 촬영 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "카메라가 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    // 카메라 해제 함수
    private fun releaseCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseCamera() // 뷰가 파괴될 때 카메라 해제
        _binding = null
    }
}
