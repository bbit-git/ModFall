APK_RELEASE := app/build/outputs/apk/release/app-release-unsigned.apk
APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
DIST_DIR := dist
DIST_APK := $(DIST_DIR)/app-release-unsigned.apk
GRADLE_USER_HOME := .gradle
LIBOPENMPT_SCRIPT := scripts/build-libopenmpt.sh
PKG := com.bigbangit.blockdrop

.PHONY: build install libopenmpt libopenmpt-force

build:
	GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew assembleRelease
	mkdir -p $(DIST_DIR)
	cp $(APK_RELEASE) $(DIST_APK)

libopenmpt:
	bash $(LIBOPENMPT_SCRIPT)

libopenmpt-force:
	FORCE_REBUILD=1 bash $(LIBOPENMPT_SCRIPT)

install: build
	GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew assembleDebug
	adb install -r $(APK_DEBUG)

log:
	@adb logcat --pid=$$(adb shell pidof -s $(PKG))