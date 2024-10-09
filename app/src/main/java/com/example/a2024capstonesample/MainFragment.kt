package com.example.a2024capstonesample


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.hardware.Camera
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
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
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.a2024capstonesample.data.PhotoDataManager
import com.example.a2024capstonesample.data.PhotoData
import java.io.FileNotFoundException
import java.io.FileOutputStream


class MainFragment : Fragment() {

    private lateinit var curPhotoPath: String // 사진 경로 저장 변수
    private lateinit var ivProfile: ImageView // 프로필 이미지를 보여줄 ImageView
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

        // 카메라 결과 리스너 설정
        parentFragmentManager.setFragmentResultListener("camera_result", viewLifecycleOwner) { _, bundle ->
            val photoData = bundle.getByteArray("photo_data")
            photoData?.let {
                onPictureTaken(it, requireContext())
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                ivProfile.setImageBitmap(bitmap)
                savePhoto(bitmap)
            }
        }

        // 솔루션 버튼 클릭 이벤트 처리
        binding.btnSolution.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_SolutionFragment) // 솔루션 Fragment로 이동
        }

        // 상세 기록 버튼 클릭 이벤트 처리
        binding.btnDetails.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_DetailFragment) // 상세 기록 Fragment로 이동
        }
        // 카메라 버튼 클릭 이벤트 처리
        binding.btnCamera.setOnClickListener {
            Log.d("CameraDebug", "btnSurFace")
            val photoFile = createImageFile()   // 파일 생성 및 경로 설정
            findNavController().navigate(R.id.action_MainFragment_to_CamSfFragment) // 카메라 테스트 Fragment로 이동
        }

/*        // 기존 카메라 버튼 클릭 리스너 (카메라 객체 사용 x)
        binding.btnSurFace.setOnClickListener {
            Log.d("CameraDebug", "takeCapture") // 디버그 로그
            takeCapture() // 카메라 촬영 함수 호출
        }*/

        // 갤러리 버튼 클릭 리스너
        binding.btnCall.setOnClickListener { openGallery() } // 갤러리 열기 함수 호출

        // 갤러리 선택 결과를 처리하는 런처 설정
        galleryActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) { // 결과가 성공적인 경우
                result.data?.data?.let { uri ->
                    val bitmap = loadImageFromUri(uri) // 선택한 이미지 로드
                    bitmap?.let {
                        ivProfile.setImageBitmap(null)
                        ivProfile.setImageBitmap(it) // 이미지 뷰에 설정

                        // 갤러리에서 선택한 이미지의 절대 경로 가져오기
                        val filePath = getRealPathFromURI(uri)
                        if (filePath != null) {
                            curPhotoPath = filePath // 절대 경로를 curPhotoPath에 저장
                            Log.d("MainFragment", "갤러리에서 불러온 절대 경로: $curPhotoPath")
                        } else {
                            Log.e("MainFragment", "갤러리에서 선택한 이미지의 절대 경로를 가져오지 못했습니다.")
                        }

                        // 새로운 데이터를 추가하기 위해 현재 날짜와 사진 정보 생성
                        val dateTaken = getExifDate(uri)
                        val newPhotoData = PhotoData(
                            date = dateTaken,
                            photoPath = curPhotoPath,
                            measurement = 0f // 갤러리에서 불러온 경우에는 측정 수치를 0으로 설정
                        )
                        PhotoDataManager.addPhotoData(newPhotoData)

                        // 로그로 확인
                        Log.d("MainFragment", "갤러리에서 불러온 사진 데이터 저장 완료: $newPhotoData")

                        onPictureTaken(convertBitmapToByteArray(it), requireContext()) // 전처리를 위해 onPictureTaken에 전달
                    } // 이미지 뷰에 설정
                }
            }
        }
        // 권한 요청
        requestPermissions() // 권한 요청 함수 호출
    }

    // 사진의 EXIF 정보를 사용해 촬영 날짜를 추출하는 함수
    private fun getExifDate(uri: Uri): String {
        val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri.let {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                inputStream?.let { ExifInterface(it) }
            }
        } else {
            uri.let {
                val filePath = getRealPathFromURI(it)
                filePath?.let { ExifInterface(it) }
            }
        }

        val dateTaken = exifInterface?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateTaken?.let {
            try {
                dateFormat.format(SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it))
            } catch (e: Exception) {
                Log.e("MainFragment", "촬영 날짜를 파싱하는 중 오류 발생: ${e.message}")
                dateFormat.format(Date())
            }
        } ?: dateFormat.format(Date())
    }

    // URI에서 실제 경로를 가져오는 메서드 (Android 10 미만을 위한 메서드)
    @SuppressLint("Recycle")
    private fun getRealPathFromURI(uri: Uri): String? {
        var filePath: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
                if (index != -1) {
                    filePath = it.getString(index)
                }
            }
            it.close()
        }
        return filePath
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

/*// 카메라 촬영 결과 처리(takeCapture())
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) { // 결과가 성공적인 경우
                val file = File(curPhotoPath) // 촬영한 이미지 파일
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath) // 파일에서 비트맵 가져오기
                    bitmap?.let {
                        ivProfile.setImageBitmap(null)
                        ivProfile.setImageBitmap(it) // 이미지 뷰에 설정
                        savePhoto(it) // 사진 저장
                        onPictureTaken(convertBitmapToByteArray(it)) // 전처리를 위해 onPictureTaken에 전달
                    }
                }
            }
        }*/
    // 비트맵을 byteArray로 변환하는 메서드
    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }



    // 이미지 리사이징을 위한 유틸리티 메서드
    // maxSize를 기준으로 비트맵의 크기를 조정하되 비율은 유지
    fun resizeBitmap(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        var resizeBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

        try {
            // 1번: 첫 번째 비트맵을 읽어와서 옵션에서 너비와 높이 가져오기
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
            var width = options.outWidth
            var height = options.outHeight
            var sampleSize = 1

            // 2번: 비율에 맞게 샘플 사이즈 계산
            while (width / 2 >= targetSize && height / 2 >= targetSize) {
                width /= 2
                height /= 2
                sampleSize *= 2
            }

            options.inSampleSize = sampleSize

            // 3번: 최종 비트맵 리사이징
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
            resizeBitmap = bitmap

            // 500x500 크기의 새로운 비트맵 생성
            val finalBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)
            // 평균 색상 계산
            val averageColor = if (resizeBitmap != null) {
                calculateAverageColor(resizeBitmap)
            } else {
                Color.GRAY // 기본 색상 또는 다른 적절한 값을 설정
            }

            // 빈 공간을 평균 색상으로 채우기
            canvas.drawColor(averageColor)

            // 중앙에 리사이즈한 비트맵을 그리기
            val newWidth = resizeBitmap?.width ?: 0
            val newHeight = resizeBitmap?.height ?: 0
            val left = (targetSize - newWidth) / 2
            val top = (targetSize - newHeight) / 2

            resizeBitmap?.let {
                canvas.drawBitmap(it, left.toFloat(), top.toFloat(), null)
            }

            return finalBitmap

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

    // 평균 색상 계산 메서드
    fun calculateAverageColor(bitmap: Bitmap): Int {
        var red = 0
        var green = 0
        var blue = 0
        var pixelCount = 0

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                red += (color shr 16) and 0xff
                green += (color shr 8) and 0xff
                blue += color and 0xff
                pixelCount++
            }
        }

        // 평균 색상 계산
        red /= pixelCount
        green /= pixelCount
        blue /= pixelCount

        return (0xff shl 24) or (red shl 16) or (green shl 8) or blue
    }

    // 이미지 픽셀 데이터를 TensorFlow Lite 입력 형식으로 변환
    // RGB 값을 0~1 사이의 float으로 정규화하여 ByteBuffer에 저장
    private fun getInputImage_2(pixels: IntArray, cx: Int, cy: Int): ByteBuffer {
        // 크기에 맞게 ByteBuffer를 생성
        val input_img = ByteBuffer.allocateDirect(cx * cy * 3 * 4)
        input_img.order(ByteOrder.nativeOrder())

        // 픽셀 데이터를 각각 R, G, B로 분해하여 정규화 후 ByteBuffer에 넣음
        for (i in 0 until cx * cy) {
            val pixel = pixels[i] // ARGB 형식의 픽셀 값

            // RGB 각 채널을 추출하여 정규화 후 저장
            input_img.putFloat(((pixel shr 16) and 0xff) / 255f) // Red
            input_img.putFloat(((pixel shr 8) and 0xff) / 255f) // Green
            input_img.putFloat(((pixel shr 0) and 0xff) / 255f) // Blue
        }

        return input_img
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

    // onPictureTaken 메서드: 여기서 전처리 수행
    private fun onPictureTaken(data: ByteArray, context: Context) {

        // 이미지 크기 설정
        val imageSize = 500

        // byte array를 bitmap으로 변환
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmapOriginal = BitmapFactory.decodeByteArray(data, 0, data.size, options)
        Log.d("CameraApp", "비트맵으로 변환 완료")

        // 비트맵을 Uri로 변환 (임시로 저장된 이미지 파일을 사용하는 방식)
        val uri = saveBitmapToFile(context, bitmapOriginal)

        // 비율을 유지하면서 크기를 조정하고 빈 공간을 특정 색으로 채우기
        val bitmapForTensorFlow = resizeBitmap(context, uri, imageSize)
        Log.d("CameraApp", "비트맵 리사이즈 및 빈 공간 채우기 완료")

        // 비트맵 이미지를 TensorFlow Lite로 입력하기 위해 처리
        val pixels = IntArray(imageSize * imageSize)
        bitmapForTensorFlow?.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize)
        Log.d("CameraApp", "비트맵 픽셀 데이터 가져오기 완료")

        // TensorFlow Lite 입력 형식으로 변환
        val input_img: ByteBuffer = getInputImage_2(pixels, imageSize, imageSize)
        Log.d("CameraApp", "입력 이미지 데이터 생성 완료")

        // TFLite 인터프리터 실행
        val tfLiteInterpreter = getTfliteInterpreter("scalp_classification_model_J_20_500_GB_0.tflite") // 최신 모델
        val prediction = Array(1) { FloatArray(2) } // 최신 모델 쓸 때

        Log.d("CameraApp", "TensorFlow Lite 모델 실행 중...")
        tfLiteInterpreter!!.run(input_img, prediction)
        Log.d("CameraApp", "모델 예측 완료")

        // curPhotoPath 초기화 확인
        if (!::curPhotoPath.isInitialized) {
            Log.e("MainFragment", "curPhotoPath가 초기화되지 않았습니다.")
            return
        }

        // 사진 경로에 맞는 데이터를 찾아 수치 업데이트
        val matchingPhotoData = PhotoDataManager.getAllPhotoData().find {
            it.photoPath == curPhotoPath || it.photoPath == Uri.fromFile(File(curPhotoPath)).toString()
        }
        if (matchingPhotoData != null) {
            matchingPhotoData.measurement = 100 * prediction[0][0] // 양호 확률 업데이트
            Log.d("MainFragment", "사진 데이터 수치 업데이트 완료: ${matchingPhotoData.measurement}")
        } else {
            Log.e("MainFragment", "사진 데이터 업데이트 실패: 경로를 찾을 수 없습니다. curPhotoPath: $curPhotoPath")
        }

        // 예측 결과 출력
        val resultMessage = String.format("두피 건강 상태:\n 양호 확률: %6.2f%%\n주의 확률: %6.2f%%", 100 * prediction[0][0], 100 * prediction[0][1])

        // 예측 결과를 AlertDialog로 표시
        AlertDialog.Builder(requireContext())
            .setTitle("두피 건강 예측 결과")
            .setMessage(resultMessage)
            .setPositiveButton("확인", null)
            .show()

        Log.d("CameraApp", "예측 결과 표시 완료")
    }

    // 비트맵을 파일로 저장하고 URI 반환하는 메서드
    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "temp_image.png") // 임시 파일 경로
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(file) // URI 반환
    }

    private fun getTfliteInterpreter(modelPath: String): Interpreter? {
        try {
            Log.d("CameraApp", "TensorFlow Lite 모델 불러오기: $modelPath")

            // requireContext()로 컨텍스트 가져오기
            val fileDescriptor: AssetFileDescriptor = requireContext().assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            Log.d("CameraApp", "TensorFlow Lite 모델 로드 완료")
            return Interpreter(model)
        } catch (e: IOException) {
            Log.e("CameraApp", "모델 파일을 불러오는 중 오류 발생: " + e.message)
            return null
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

            // 촬영 날짜 추출 및 저장
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // PhotoDataManager에 데이터 추가
            val newPhotoData = PhotoData(
                date = currentDate,
                photoPath = it.toString(), // 저장된 이미지의 URI 경로
                measurement = 0f // 기본 측정값 (필요 시 이후 업데이트 가능)
            )
            PhotoDataManager.addPhotoData(newPhotoData)
            Log.d("MainFragment", "사진 데이터 저장 완료: $newPhotoData") // 데이터 저장 로그

            // 저장된 이미지의 URI를 curPhotoPath에 업데이트
            curPhotoPath = it.toString()
            Log.d("MainFragment", "curPhotoPath 업데이트 완료: $curPhotoPath")
        }
    }
}