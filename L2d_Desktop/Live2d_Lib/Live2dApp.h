#pragma once

System::IntPtr LoadFileDO(System::String^ filePath, Csm::csmSizeInt* outSize);
void LoadDone(System::String^ file);
void TopUpdate();

extern char* MotionGroupIdle; // �����ɥ��
extern char* MotionGroupTapBody; // ��򥿥åפ����Ȥ�

extern char* HitAreaNameHead;
extern char* HitAreaNameBody;