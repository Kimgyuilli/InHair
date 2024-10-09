package com.example.a2024capstonesample

// AndroidX 라이브러리 임포트
// 안드로이드 기본 컴포넌트 임포트
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.Size
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.a2024capstonesample.databinding.FragmentCamerasurfaceBinding
import java.io.ByteArrayOutputStream


class CamSfFragment : Fragment() {
    // 화면 구성요소들
    private var _binding: FragmentCamerasurfaceBinding? = null
    private val binding get() = _binding!!
    private var camera: Camera? = null // 카메라 객체

    private lateinit var photoURI: Uri // 사진 URI 저장 변수
    private lateinit var curPhotoPath: String // 사진 경로 저장 변수

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCamerasurfaceBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 카메라 테스트 버튼 클릭 이벤트 처리
        binding.buttonMainCapture.setOnClickListener {
            Log.d("CameraDebug", "btnSurFace")
            takeCapture()
        }

        // SurfaceHolder 설정
        val surfaceHolder = binding.cameraPreviewMain.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // 카메라 초기화
                try {
                    camera = Camera.open() // 카메라 열기
                    // 카메라 프리뷰 회전 설정
                    val info = CameraInfo()
                    Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info) // 후면 카메라의 정보를 가져옴
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
                    camera?.setPreviewDisplay(holder) // SurfaceView에 프리뷰 표시
                    camera?.startPreview() // 프리뷰 시작
                } catch (e: Exception) {
                    Log.e("CamSfFragment", "Error setting up camera: ${e.message}")
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // SurfaceView가 변경되었을 때의 처리
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // SurfaceView가 파괴되면 카메라 해제
                camera?.stopPreview()
                camera?.release()
                camera = null
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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


}
