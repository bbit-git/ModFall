APK_RELEASE := app/build/outputs/apk/release/app-release-unsigned.apk
APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
DIST_DIR := dist
DIST_APK := $(DIST_DIR)/app-release-unsigned.apk

.PHONY: build install

build:
	./gradlew assembleRelease
	mkdir -p $(DIST_DIR)
	cp $(APK_RELEASE) $(DIST_APK)

install: build
	./gradlew assembleDebug
	adb install -r $(APK_DEBUG)
