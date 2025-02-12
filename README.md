# 👨‍🦲 탈모 진단, 솔루션 제공 서비스 앱 InHair

![logo](images/logo.png)
 

- 배포 URL : https://drive.google.com/file/d/1QHplI3DMvmGXK2upUt-GtkSU0T345zXv/view?usp=sharing

<br>

## 프로젝트 소개

- InHair는 개인이 손쉽게 탈모 상태를 진단받고 간단한 솔루션을 제공받을 수 있는 AI기반 앱입니다.
- 개인의 정수리 사진을 등록함으로써 탈모 점수와 피드백을 확인할 수 있습니다.
- 탈모 점수의 변화와 과거 나의 모습과 현재 나의 모습을 비교할 수 있습니다.

<br>

## 팀원 구성

<div align="center">

| **주찬양** | **조경환** | **김규일** |

</div>

<br>

## 1. 개발 환경

- App: Kotlin, room, XML
- AI model: Tensorflow, Python
- 버전 및 이슈관리 : Github, Github Issues, Github Project
- 협업 툴 : Discord
- 서비스 배포 환경 : Local
- 디자인 : [Figma](https://www.figma.com/file/fAisC2pEKzxTOzet9CfqML/README(oh-my-code)?node-id=39%3A1814)
<br>

## 2. 채택한 개발 기술

### React, styled-component

- Kotlin 
          - 안드로이드 네이티브 앱 내에 있는 카메라 API 사용 및 개발 경험의 다양성을 고려한 언어 선택

<br>

## 3. 역할 분담

### 🍊김규일

- **기능**
    - 페이지 : 메인, 카메라
    - 상세 기능: 페이지 연결, 추세 그래프, 카메라 구현 및 권한, 사진 전처리 및 저장
- **DB**
    - Android room을 이용한 로컬 DB 구현

<br>
    
### 👻조경환

- **UI**
    - 페이지 : 메인, 솔루션, 기록 페이지
- **기능**
    - 솔루션 페이지, 기록 페이지 기능 구현

<br>

### 😎주찬양

- **AI모델**
- 정수리를 통해 탈모 진행률을 진단하는 AI프로그램 설계, 학습

<br>


## 5. 개발 기간 및 작업 관리

### 개발 기간

- 전체 개발 기간 : 2024-09-25 ~ 2024-11-12

<br>

### 작업 관리

- discord를 통한 주간 회의 1회와 대면 회의 1회 총 주 2회로 진행

<br>


## 7. 페이지별 기능

### [초기화면]
- 서비스 접속 초기화면으로 splash 화면이 잠시 나온 뒤 다음 페이지가 나타납니다.
- 그간의 기록을 보여주는 그래프
- 촬영, 갤러리 호출, 솔루션, 기록페이지로 이동하는 버튼

| 초기화면 |
|----------|
![main2](images/main2.png)

<br>

![main](images/main.png)

<br>


| 기록 |
|----------|
![detail](images/detail.png)

<br>

![detail2](images/detail2.png)
<br>

| 솔루션 |
|----------|
![solution](images/solution.png)

<br>


| 카메라 |
|----------|
![camera](images/camera.png)

<br>


