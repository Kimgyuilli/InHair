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
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.a2024capstonesample.Room.AverageScore
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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.a2024capstonesample.Room.MyAppDatabase
import com.example.a2024capstonesample.Room.MyEntity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope



class MainFragment : Fragment() {

    private lateinit var curPhotoPath: String // 사진 경로 저장 변수
    private lateinit var galleryActivityResultLauncher: ActivityResultLauncher<Intent> // 갤러리 결과를 처리할 런처
    private lateinit var database: MyAppDatabase // Database 저장경로

    private var _binding: FragmentMainBinding? = null // View Binding을 위한 변수
    private val binding get() = _binding!! // Binding 초기화

    private var goodMeasure = 0.0    // 양호 수치 변수
    private var cautionMeasure = 0.0    // 주의 수치 변수

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false) // Fragment의 View를 생성
        database = MyAppDatabase.getDatabase(requireContext()) //데이터베이스 연결
        return binding.root // 생성된 View 반환
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 카메라 결과 리스너 설정
        parentFragmentManager.setFragmentResultListener("camera_result", viewLifecycleOwner) { _, bundle ->
            val photoData = bundle.getByteArray("photo_data")
            photoData?.let {
                // 먼저 bitmap을 빠르게 처리하여 UI에 표시
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)

                showProgressToast()    // 이미지 분석 중 메시지 표시

                // 백그라운드에서 무거운 처리 실행
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 이미지 처리 및 분석
                        val score = onPictureTaken(it, requireContext())
                        val result: Pair<String, String>? = savePhoto(bitmap)

                        // UI 업데이트는 메인 스레드에서 실행
                        withContext(Dispatchers.Main) {
                            hideProgressDialog()    // 이미지 분석 중 메시지 닫기

                            result?.let { (date, path) ->
                                Log.d("MainFragment", "저장된 날짜: $date")
                                Log.d("MainFragment", "저장된 경로: $path")
                                Log.d("MainFragment", "저장된 수치: $score")

                                // insertPhoto 호출
                                insertPhoto(date, path, score)

                                // 분석 결과를 AlertDialog로 표시
                                showAnalysisResult(score)
                            } ?: run {
                                Log.e("insertPhoto", "데이터 저장에 실패했습니다")
                                Toast.makeText(requireContext(), "데이터 저장에 실패했습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("MainFragment", "이미지 처리 중 오류 발생: ${e.message}")
                            Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
        // db테스트 버튼
        binding.btnDelete.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_RoomTest) // Roomtest Fragment로 이동
        }

        // 갤러리 버튼 클릭 리스너
        binding.btnCall.setOnClickListener { openGallery() } // 갤러리 열기 함수 호출

        // 갤러리 선택 결과를 처리하는 런처 설정
        galleryActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // 이미지 로드를 코루틴으로 처리
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // 비트맵 로드 (IO 스레드에서)
                            val bitmap = withContext(Dispatchers.IO) {
                                loadImageFromUri(uri)
                            }

                            bitmap?.let {
                                // 파일 경로 가져오기
                                val filePath = withContext(Dispatchers.IO) {
                                    getRealPathFromURI(uri)
                                }

                                // UI 스레드에서 curPhotoPath 업데이트
                                withContext(Dispatchers.Main) {
                                    if (filePath != null) {
                                        curPhotoPath = filePath
                                        Log.d("MainFragment", "갤러리에서 불러온 절대 경로: $curPhotoPath")
                                    } else {
                                        Log.e("MainFragment", "갤러리에서 선택한 이미지의 절대 경로를 가져오지 못했습니다.")
                                        Toast.makeText(requireContext(), "이미지 경로를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }
                                }

                                // 중복 확인 (IO 스레드에서)
                                val existingPhotoData = withContext(Dispatchers.IO) {
                                    PhotoDataManager.getAllPhotoData().find { it.photoPath == curPhotoPath }
                                }

                                if (existingPhotoData != null) {
                                    withContext(Dispatchers.Main) {
                                        Log.d("MainFragment", "이미 존재하는 사진입니다: $curPhotoPath")
                                        Toast.makeText(requireContext(), "이미 존재하는 사진입니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // 새로운 사진 처리
                                    val dateTaken = withContext(Dispatchers.IO) {
                                        getExifDate(uri)
                                    }

                                    // 임시 PhotoData 객체 생성 및 저장
                                    val newPhotoData = PhotoData(
                                        date = dateTaken,
                                        photoPath = curPhotoPath,
                                        goodMeasurement = 0.0,
                                        cautionMeasurement = 0.0
                                    )

                                    withContext(Dispatchers.IO) {
                                        PhotoDataManager.addPhotoData(newPhotoData)
                                        Log.d("MainFragment", "갤러리에서 불러온 사진 데이터 저장 완료: $newPhotoData")
                                    }

                                    // 프로그레스 표시
                                    withContext(Dispatchers.Main) {
                                        showProgressToast()
                                    }

                                    try {
                                        // 이미지 처리 및 분석
                                        val byteArray = withContext(Dispatchers.IO) {
                                            convertBitmapToByteArray(it)
                                        }

                                        val score = withContext(Dispatchers.IO) {
                                            onPictureTaken(byteArray, requireContext())
                                        }

                                        // 분석 결과 저장 및 UI 업데이트
                                        withContext(Dispatchers.Main) {
                                            hideProgressDialog()
                                            insertPhoto(dateTaken, curPhotoPath, score)
                                            showAnalysisResult(score)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            hideProgressDialog()
                                            Log.e("MainFragment", "이미지 처리 중 오류 발생: ${e.message}")
                                            Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                hideProgressDialog()
                                Log.e("MainFragment", "갤러리 이미지 처리 중 오류 발생: ${e.message}")
                                Toast.makeText(requireContext(), "이미지 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        loadAverageScores() // 평균 점수 로드
        // 권한 요청
        requestPermissions() // 권한 요청 함수 호출
    }
    //db에 데이터 삽입
    private fun insertPhoto(date: String, imagePath: String, score: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            // score를 "%6.2f%%" 형식으로 변환
            val formattedScore = String.format("%.2f%%", score)
            // MyEntity 생성 시 필요한 데이터들을 인자로 받아서 insert
            database.myDao().insert(MyEntity(date = date, imagePath = imagePath, score = score, formattedScore = formattedScore))
        }
    }

    // 데이터베이스에서 평균 점수를 로드하는 함수
    private fun loadAverageScores() {
        // IO 스레드에서 데이터베이스 작업 수행
        CoroutineScope(Dispatchers.IO).launch {
            // 데이터베이스에서 날짜별 평균 점수 조회
            val averageScores = database.myDao().getAverageScoresByDate()
            // UI 업데이트는 메인 스레드에서 수행
            withContext(Dispatchers.Main) {
                setupChart(averageScores)
            }
        }
    }

    // 차트를 설정하고 데이터를 표시하는 함수
    private fun setupChart(averageScores: List<AverageScore>) {
        CoroutineScope(Dispatchers.IO).launch {
            // 차트에 표시할 데이터 포인트를 저장할 리스트
            val entries = arrayListOf<Entry>()

            // 데이터가 비어 있는 경우에도 빈 차트를 설정
            if (averageScores.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.lineChart.visibility = View.GONE
                    binding.emptyChartImage.visibility = View.VISIBLE
                }
                return@launch
            } else {
                withContext(Dispatchers.Main) {
                    binding.lineChart.visibility = View.VISIBLE
                    binding.emptyChartImage.visibility = View.GONE
                }
            }

            // 날짜 형식 지정을 위한 포맷터
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 각 데이터 포인트를 차트 엔트리로 변환
            averageScores.forEachIndexed { index, averageScore ->
                if (averageScore.averageScore >= 0) {
                    entries.add(Entry(index.toFloat(), averageScore.averageScore))
                }
            }

            // 차트의 데이터셋 설정
            val dataSet = LineDataSet(entries, "평균 점수").apply {
                color = Color.BLUE                // 선 색상
                lineWidth = 6f                    // 선 두께
                setCircleColor(Color.BLUE)         // 데이터 포인트 색상
                circleRadius = 6f    // 데이터 포인트 크기
                setDrawValues(false)                           // 데이터 값 표시 여부
            }

            // 차트에 표시할 전체 데이터 설정
            val lineData = LineData(dataSet)

            // UI 스레드에서 차트 업데이트
            withContext(Dispatchers.Main) {
                binding.lineChart.apply {
                    data = lineData

                    // X축 설정
                    xAxis.apply {
                        // X축의 값을 날짜 문자열로 변환하는 포맷터
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val position = value.toInt()
                                return if (position >= 0 && position < averageScores.size) {
                                    averageScores[position].date
                                } else ""
                            }
                        }
                        position = XAxis.XAxisPosition.BOTTOM  // X축 위치를 아래로 설정
                        granularity = 1f                      // 최소 간격 설정
                    }

                    // Y축 설정
                    axisLeft.apply {
                        setDrawGridLines(true)    // 그리드 라인 표시
                        axisMinimum = 0f          // Y축 최소값
                    }
                    axisRight.isEnabled = false   // 오른쪽 Y축 비활성화

                    description.isEnabled = false // 차트 설명 비활성화
                    legend.isEnabled = true       // 범례 활성화

                    // 차트 새로고침
                    invalidate()
                }
            }
        }
    }



    // 사진의 EXIF 정보를 사용해 촬영 날짜를 추출하는 함수
    private fun getExifDate(uri: Uri): String {
        // Android 10 이상에서는 InputStream을 사용하여 EXIF 정보 추출
        val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.let { ExifInterface(it) }
        } else {
            val filePath = getRealPathFromURI(uri)
            filePath?.let { ExifInterface(it) }
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
        } ?: run {
            Log.e("MainFragment", "EXIF 정보가 없어 기본 날짜 설정")
            dateFormat.format(Date())
        }
    }

    // URI에서 실제 경로를 가져오는 메서드
    @SuppressLint("Recycle")
    private fun getRealPathFromURI(uri: Uri): String? {
        // Android 10 이상에서는 _ID 필드로 접근
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var filePath: String? = null
            val cursor = requireContext().contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = it.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    filePath = contentUri.toString() // Android 10 이상에서는 URI로 저장
                }
            }
            return filePath
        } else {
            // Android 9 이하에서는 기존 방식으로 경로를 가져옴
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

    // 비트맵을 byteArray로 변환하는 메서드
    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }


    // 이미지 리사이징을 위한 유틸리티 메서드
    // maxSize를 기준으로 비트맵의 크기를 조정하되 비율은 유지
    // 비는 부분 평균 색 픽셀 추가
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

    // 프로그레스 다이얼로그 관련 변수와 메서드
    private var progressDialog: AlertDialog? = null

    private fun showProgressToast() {
        Toast.makeText(requireContext(), "이미지를 분석중입니다...", Toast.LENGTH_SHORT).show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
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

    // 분석 결과를 보여주는 함수
    private fun showAnalysisResult(score: Float) {
        val goodProbability = 100 * score
        val cautionProbability = 100 * (1 - score)

        val resultMessage = String.format(
            "두피 점수: %6.0f점",
            goodProbability,
        )

        Toast.makeText(requireContext(), resultMessage, Toast.LENGTH_LONG).show()
    }

    // onPictureTaken 메서드 수정
    private suspend fun onPictureTaken(data: ByteArray, context: Context): Float {
        return withContext(Dispatchers.IO) {
            try {
                val imageSize = 500

                // byte array를 bitmap으로 변환
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmapOriginal = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                Log.d("CameraApp", "비트맵으로 변환 완료")

                // 비트맵을 Uri로 변환
                val uri = saveBitmapToFile(context, bitmapOriginal)
                Log.d("Onpic", " URI로 변환 완료")

                // 비율을 유지하면서 크기를 조정
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
                val tfLiteInterpreter = getTfliteInterpreter("scalp_classification_model_J_20_500_GB_0.tflite")
                val prediction = Array(1) { FloatArray(2) }

                Log.d("CameraApp", "TensorFlow Lite 모델 실행 중...")
                tfLiteInterpreter!!.run(input_img, prediction)
                Log.d("CameraApp", "모델 예측 완료")

                if (!::curPhotoPath.isInitialized) {
                    Log.e("MainFragment", "curPhotoPath가 초기화되지 않았습니다.")
                    return@withContext 0f
                }

                goodMeasure = (100 * prediction[0][0]).toDouble()
                cautionMeasure = 100 * (1.0 - prediction[0][0])

                prediction[0][0]
            } catch (e: Exception) {
                Log.e("CameraApp", "이미지 처리 중 오류 발생: ${e.message}")
                0f
            }
        }
    }

    // 비트맵을 파일로 저장하고 URI 반환하는 메서드
    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "temp_image.png") // 임시 파일 경로
        Log.d("saveBitmapToFile", "URI1")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.d("saveBitmapToFile", "URI2")
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


    // 사진 저장 후 날짜와 경로를 반환하는 메서드
    private fun savePhoto(bitmap: Bitmap): Pair<String, String>? {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpeg"
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

            // 저장된 이미지의 절대 경로 가져오기
            val absolutePath = getRealPathFromURI(it) ?: it.toString()
            curPhotoPath = absolutePath // 절대 경로로 설정

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newPhotoData = PhotoData(date = currentDate, photoPath = curPhotoPath, goodMeasurement = goodMeasure, cautionMeasurement = cautionMeasure)
            PhotoDataManager.addPhotoData(newPhotoData)
            Log.d("MainFragment", "사진 데이터 저장 완료: $newPhotoData")
            Log.d("MainFragment", "curPhotoPath 업데이트 완료: $curPhotoPath")
            Log.d("MainFragment", "양호 수치 업데이트 완료: $goodMeasure")
            Log.d("MainFragment", "주의 수치 업데이트 완료: $cautionMeasure")

            return Pair(currentDate, curPhotoPath)
        }
        return null // 실패 시 null 반환
    }
}