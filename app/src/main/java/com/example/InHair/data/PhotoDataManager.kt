package com.example.InHair.data

object PhotoDataManager {
    private val photoDataList: MutableList<PhotoData> = mutableListOf()
    private val listeners = mutableListOf<() -> Unit>()

    // 데이터를 리스트에 추가하는 함수
    fun addPhotoData(photoData: PhotoData) {
        if (photoDataList.any { it.photoPath == photoData.photoPath }) {
            // 동일한 경로의 사진이 이미 있는 경우 추가하지 않음
            return
        }
        photoDataList.add(photoData)
        notifyListeners()  // 데이터 추가 시 모든 리스너에게 알림
    }

    // 리스너 등록 함수
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    // 리스너들에게 변경 사항 알리기
    private fun notifyListeners() {
        for (listener in listeners) {
            listener()
        }
    }

    // 모든 데이터를 반환하는 함수
    fun getAllPhotoData(): List<PhotoData> {
        return photoDataList
    }
}