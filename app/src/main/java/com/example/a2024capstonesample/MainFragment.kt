package com.example.a2024capstonesample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.a2024capstonesample.databinding.FragmentMainBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainFragment : Fragment() {
    private lateinit var curPhotoPath: String // 사진 경로 저장 변수
    private lateinit var ivProfile: ImageView // 프로필 이미지를 보여줄 ImageView
    private val galleryRequestCode = 2 // 갤러리 요청 코드
    private lateinit var galleryActivityResultLauncher: ActivityResultLauncher<Intent> // 갤러리 결과를 처리할 런처

    private var _binding: FragmentMainBinding? = null // View Binding을 위한 변수
    private val binding get() = _binding!! // Binding 초기화

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false) // Fragment의 View를 생성
        return binding.root // 생성된 View 반환
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이미지 뷰 초기화
        ivProfile = binding.chart // XML에서 ImageView 연결

        // 솔루션 버튼 클릭 이벤트 처리
        binding.btnSolution.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SolutionFragment) // 솔루션 Fragment로 이동
        }

        // 상세 기록 버튼 클릭 이벤트 처리
        binding.btnDetails.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_DetailFragment) // 상세 기록 Fragment로 이동
        }

        // 카메라 버튼 클릭 리스너
        binding.btnCamera.setOnClickListener {
            Log.d("CameraDebug", "takeCapture") // 디버그 로그
            takeCapture() // 카메라 촬영 함수 호출
        }

        // 갤러리 버튼 클릭 리스너
        binding.btnCall.setOnClickListener { openGallery() } // 갤러리 열기 함수 호출

        // 갤러리 선택 결과를 처리하는 런처 설정
        galleryActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) { // 결과가 성공적인 경우
                result.data?.data?.let { uri ->
                    val bitmap = loadImageFromUri(uri) // 선택한 이미지 로드
                    bitmap?.let { ivProfile.setImageBitmap(it) } // 이미지 뷰에 설정
                }
            }
        }

        // 권한 요청
        requestPermissions() // 권한 요청 함수 호출
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Binding 해제
    }

    // 권한 요청 함수
    private fun requestPermissions() {
        val sharedPreferences = requireActivity().getSharedPreferences("app_preferences", Activity.MODE_PRIVATE)
        val hasShownPermissionMessage = sharedPreferences.getBoolean("hasShownPermissionMessage", false)

        // Android 10 이상에서는 WRITE_EXTERNAL_STORAGE 권한이 필요하지 않음
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.CAMERA) // 카메라 권한만 요청
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE) // 카메라 및 저장소 권한 요청
        }

        if (!hasShownPermissionMessage) {
            val permissionListener = object : PermissionListener {
                override fun onPermissionGranted() {
                    Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()

                    // 메시지를 보여주었다고 저장
                    sharedPreferences.edit().putBoolean("hasShownPermissionMessage", true).apply()
                }

                override fun onPermissionDenied(deniedPermissions: List<String?>) {
                    Toast.makeText(requireContext(), "Permission Denied: $deniedPermissions", Toast.LENGTH_SHORT).show()
                }
            }

            TedPermission.create()
                .setPermissionListener(permissionListener)
                .setDeniedMessage("권한을 거부하시면 서비스를 이용하실 수 없습니다.\n\n권한을 허가해주세요. [설정] > [권한]")
                .setPermissions(*permissions) // 요청할 권한 설정
                .check()
        }
    }

    // 카메라 촬영 결과 처리
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) { // 결과가 성공적인 경우
                val bitmap = getCapturedImage() // 촬영한 이미지 가져오기
                bitmap?.let {
                    ivProfile.setImageBitmap(it) // 이미지 뷰에 설정
                    savePhoto(it) // 사진 저장
                }
            }
        }

    // 이미지 처리 함수
    private fun getCapturedImage(): Bitmap? {
        val file = File(curPhotoPath) // 촬영된 이미지 파일
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28 이상
                val source = ImageDecoder.createSource(requireActivity().contentResolver, Uri.fromFile(file))
                val bitmap = ImageDecoder.decodeBitmap(source) // 비트맵 디코드
                // 비트맵 크기 조정
                Bitmap.createScaledBitmap(bitmap, 500, 500, true) // 500x500 크기로 조정
            } else { // API 28 미만
                val uri = Uri.fromFile(file)
                requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream) // 비트맵 디코드
                    // 비트맵 크기 조정
                    Bitmap.createScaledBitmap(bitmap, 500, 500, true) // 500x500 크기로 조정
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "사진을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show() // 오류 메시지 표시
            null
        }
    }

    // 카메라 실행
    @SuppressLint("QueryPermissionsNeeded")
    private fun takeCapture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE) // 카메라 인텐트 생성
        try {
            val photoFile: File? = createImageFile() // 이미지 파일 생성
            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI) // 촬영된 이미지의 URI 설정
                startForResult.launch(takePictureIntent) // 카메라 인텐트 실행
            }
        } catch (ex: IOException) {
            Log.e("CameraDebug", "Error creating image file", ex)
            Toast.makeText(requireContext(), "카메라를 실행할 수 없습니다: ${ex.message}", Toast.LENGTH_SHORT).show() // 오류 메시지 표시
        }
    }

    // 갤러리 열기
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) // 갤러리 인텐트 생성
        galleryActivityResultLauncher.launch(intent) // 갤러리 실행
    }

    // 이미지 파일 생성
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) // 현재 시간으로 파일명 생성
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) // 외부 저장소의 사진 디렉터리 경로
        return File.createTempFile(
            "JPEG_${timestamp}_", // 파일 이름
            ".jpg", // 확장자
            storageDir // 저장 위치
        ).apply {
            curPhotoPath = absolutePath // 현재 사진 경로 저장
        }
    }

    // 갤러리에서 선택한 이미지 로드
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri) // API 28 미만
            } else {
                val source = ImageDecoder.createSource(requireActivity().contentResolver, uri) // API 28 이상
                ImageDecoder.decodeBitmap(source) // 비트맵 디코드
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "이미지를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show() // 오류 메시지 표시
            null
        }
    }

    // 사진 저장
    private fun savePhoto(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpeg" // 현재 시간을 파일명으로 설정
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename) // 파일 이름 설정
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") // MIME 타입 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // API 29 이상에서 저장 경로 설정
                put(MediaStore.MediaColumns.IS_PENDING, 1) // 저장 중임을 표시
            }
        }

        // 이미지 URI에 삽입하여 갤러리에 저장
        val imageUri: Uri? = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let {
            requireActivity().contentResolver.openOutputStream(it)?.use { out -> // 출력 스트림 생성
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // 비트맵을 JPEG 형식으로 압축
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear() // ContentValues 초기화
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0) // 저장 완료 표시
                requireActivity().contentResolver.update(it, contentValues, null, null) // 갤러리에 업데이트
            }
            Toast.makeText(requireContext(), "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show() // 저장 완료 메시지 표시
        }
    }
}
