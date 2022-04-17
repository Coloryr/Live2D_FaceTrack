#pragma once

System::IntPtr LoadFileDO(System::String^ filePath, Csm::csmSizeInt* outSize);
void LoadDone(System::String^ file);
void TopUpdate();

extern char* MotionGroupIdle; // アイドリング
extern char* MotionGroupTapBody; // 悶をタップしたとき

extern char* HitAreaNameHead;
extern char* HitAreaNameBody;