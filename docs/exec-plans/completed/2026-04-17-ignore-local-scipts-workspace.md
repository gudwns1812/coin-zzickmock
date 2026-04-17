# 로컬 scipts 작업 디렉터리 무시 규칙 추가

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
작업 범위는 `.gitignore` 한 파일이며, 저장소에 올리면 안 되는 로컬 실험 디렉터리 제외 규칙만 추가한다.

## 목적 / 큰 그림

현재 로컬 `scipts/` 디렉터리에는 스크립트, 가상환경, IDE 파일, `.env` 같은 개인 작업 산출물이 섞여 있다.
이번 작업이 끝나면 이 디렉터리 전체가 Git 추적 대상에서 제외되어, 이후 PR 분리 과정에서 로컬 민감 정보나 불필요한 실험 파일이 함께 올라갈 위험을 줄여야 한다.

## 진행 현황

- [x] (2026-04-17 11:30+09:00) 로컬 `scipts/` 디렉터리 성격 확인
- [x] (2026-04-17 11:30+09:00) `.gitignore`에 `scipts/*` 규칙 추가

## 결과 및 회고

- 저장소 루트 `.gitignore`가 `scipts/` 아래 로컬 파일을 무시하게 됐다.
- 분리 PR를 만드는 동안 로컬 `.env`, IDE 메타데이터, 가상환경 폴더를 따로 거르지 않아도 되는 기본 안전장치가 생겼다.

## 검증과 수용 기준

- `.gitignore`에 `scipts/*` 규칙이 존재한다.
- 이후 `git status`에서 `scipts/` 내부 파일이 추적 대상으로 새로 올라오지 않는다.
