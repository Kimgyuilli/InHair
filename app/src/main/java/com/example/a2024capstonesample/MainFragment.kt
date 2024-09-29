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
    private lateinit var curPhotoPath: String // 사진 경로
    private lateinit var ivProfile: ImageView // 이미지 뷰
    private val galleryRequestCode = 2 // 갤러리 요청 코드
    private lateinit var galleryActivityResultLauncher: ActivityResultLauncher<Intent>

    private var _binding: FragmentMainBinding? = null
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

        // 이미지 뷰 초기화
        ivProfile = binding.chart // XML에서 id로 접근하여 연동

        // 솔루션 버튼 클릭 이벤트 처리
        binding.btnSolution.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SolutionFragment)
        }

        // 상세 기록 버튼 클릭 이벤트 처리
        binding.btnDetails.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_DetailFragment)
        }

        // 카메라 버튼 클릭 리스너
        binding.btnCamera.setOnClickListener {
            Log.d("CameraDebug", "takeCapture")
            takeCapture()
        }

        // 갤러리 버튼 클릭 리스너
        binding.btnCall.setOnClickListener { openGallery() }

        // 갤러리 선택 결과를 처리하는 런처 설정
        galleryActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val bitmap = loadImageFromUri(uri)
                    bitmap?.let { ivProfile.setImageBitmap(it) }
                }
            }
        }

        // 권한 요청
        requestPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 권한 요청
    private fun requestPermissions() {
        val sharedPreferences = requireActivity().getSharedPreferences("app_preferences", Activity.MODE_PRIVATE)
        val hasShownPermissionMessage = sharedPreferences.getBoolean("hasShownPermissionMessage", false)

        // 권한이 이미 허용되었고 메시지를 보여주지 않은 경우에만 메시지 표시
        if (!hasShownPermissionMessage) {
            val permissionListener = object : PermissionListener {
                override fun onPermissionGranted() {
                    // 권한이 허용된 경우
                    Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()

                    // 메시지를 보여주었다고 저장
                    sharedPreferences.edit().putBoolean("hasShownPermissionMessage", true).apply()
                }

                override fun onPermissionDenied(deniedPermissions: List<String?>) {
                    // 권한이 거부된 경우
                    Toast.makeText(requireContext(), "Permission Denied: $deniedPermissions", Toast.LENGTH_SHORT).show()
                }
            }

            TedPermission.create()
                .setPermissionListener(permissionListener)
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check()
        }
    }

    // 카메라 촬영 결과 처리
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = getCapturedImage()
                bitmap?.let {
                    ivProfile.setImageBitmap(it) // 촬영된 이미지를 ImageView에 표시
                    savePhoto(it) // 사진 저장
                }
            }
        }

    // 이미지 처리 함수
    private fun getCapturedImage(): Bitmap? {
        val file = File(curPhotoPath)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28 이상
                val source = ImageDecoder.createSource(requireActivity().contentResolver, Uri.fromFile(file))
                val bitmap = ImageDecoder.decodeBitmap(source)
                // 비트맵 크기 조정
                Bitmap.createScaledBitmap(bitmap, 500, 500, true)
            } else { // API 28 미만
                val uri = Uri.fromFile(file)
                requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    // 비트맵 크기 조정
                    Bitmap.createScaledBitmap(bitmap, 500, 500, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "사진을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // 카메라 실행
    @SuppressLint("QueryPermissionsNeeded")
    private fun takeCapture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val photoFile: File? = createImageFile()
            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startForResult.launch(takePictureIntent)
            }
        } catch (ex: IOException) {
            Log.e("CameraDebug", "Error creating image file", ex)
            Toast.makeText(requireContext(), "카메라를 실행할 수 없습니다: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 갤러리 열기
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryActivityResultLauncher.launch(intent)
    }

    // 이미지 파일 생성
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timestamp}_",
            ".jpg",
            storageDir
        ).apply {
            curPhotoPath = absolutePath
        }
    }

    // 갤러리에서 선택한 이미지 로드
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "이미지를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // 사진 저장
    private fun savePhoto(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpeg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri: Uri? = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let {
            requireActivity().contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                requireActivity().contentResolver.update(it, contentValues, null, null)
            }
            Toast.makeText(requireContext(), "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
