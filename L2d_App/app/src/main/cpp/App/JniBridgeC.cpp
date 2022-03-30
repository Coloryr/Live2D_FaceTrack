/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#include <jni.h>
#include "JniBridgeC.hpp"
#include "LAppDelegate.hpp"
#include "LAppPal.hpp"
#include "LAppView.hpp"
#include "LAppLive2DManager.hpp"
#include "LAppModel.hpp"
#include <CubismFramework.hpp>
#include <string.h>

using namespace Csm;

static JavaVM *g_JVM; // JavaVM is valid for all threads, so just save it globally
static jclass g_JniBridgeJavaClass;
static jmethodID g_LoadFileMethodId;
static jmethodID g_OnLoadModelMethodId;
static jmethodID g_OnUpdateMethodId;

#define class_name(F) Java_com_coloryr_facetrack_live2d_JniBridgeJava_native##F
#define class_local "com/coloryr/facetrack/live2d/JniBridgeJava"

JNIEnv *GetEnv()
{
    JNIEnv *env = NULL;
    g_JVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

// The VM calls JNI_OnLoad when the native library is loaded
jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    g_JVM = vm;

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass(class_local);
    g_JniBridgeJavaClass = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
    g_LoadFileMethodId = env->GetStaticMethodID(g_JniBridgeJavaClass, "LoadFile", "(Ljava/lang/String;)[B");
    g_OnLoadModelMethodId = env->GetStaticMethodID(g_JniBridgeJavaClass, "onLoadModel", "(Ljava/lang/String;)V");
    g_OnUpdateMethodId = env->GetStaticMethodID(g_JniBridgeJavaClass, "onUpdate", "()V");

    return JNI_VERSION_1_6;
}

void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env = GetEnv();
    env->DeleteGlobalRef(g_JniBridgeJavaClass);
}

char *JniBridgeC::LoadFileAsBytesFromJava(const char *filePath, unsigned int *outSize)
{
    JNIEnv *env = GetEnv();

    // ファイルロード
    jbyteArray obj = (jbyteArray)env->CallStaticObjectMethod(g_JniBridgeJavaClass, g_LoadFileMethodId, env->NewStringUTF(filePath));
    *outSize = static_cast<unsigned int>(env->GetArrayLength(obj));

    char *buffer = new char[*outSize];
    env->GetByteArrayRegion(obj, 0, *outSize, reinterpret_cast<jbyte *>(buffer));

    return buffer;
}

void JniBridgeC::OnLoadModel(char *name)
{
    JNIEnv *env = GetEnv();
    jstring str_arg = env->NewStringUTF(name);
    env->CallStaticVoidMethod(g_JniBridgeJavaClass, g_OnLoadModelMethodId, str_arg);
    env->DeleteLocalRef(str_arg);
}

void JniBridgeC::OnUpdate()
{
    JNIEnv *env = GetEnv();
    env->CallStaticVoidMethod(g_JniBridgeJavaClass, g_OnUpdateMethodId, NULL);
}

Csm::csmString ToString(JNIEnv *env, jbyteArray data)
{
    char *chars = NULL;
    jbyte *bytes;
    int chars_len;

    bytes = env->GetByteArrayElements(data, 0);
    chars_len = env->GetArrayLength(data);
    chars = new char[chars_len + 1];
    memset(chars, 0, chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;

    Csm::csmString res = Csm::csmString(chars);

    env->ReleaseByteArrayElements(data, bytes, 0);
    delete[] chars;

    return res;
}

extern "C"
{
    JNIEXPORT jfloatArray JNICALL
    class_name(GetPartValues)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            const csmFloat32 *list = model->GetPartValues();
            if (list == NULL)
                return NULL;

            csmInt32 size = model->GetPartCount();

            jfloatArray ret = (jfloatArray)env->NewFloatArray(size);

            env->SetFloatArrayRegion(ret, 0, size, list);

            return ret;
        }

        return NULL;
    }

    JNIEXPORT jobjectArray JNICALL
    class_name(GetPartIds)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            Csm::csmVector<Live2D::Cubism::Framework::CubismIdHandle> list = model->GetPartIds();
            jobjectArray ret = (jobjectArray)env->NewObjectArray(list.GetSize(), env->FindClass("java/lang/String"), env->NewStringUTF(""));

            for (int i = 0; i < list.GetSize(); i++)
            {
                env->SetObjectArrayElement(ret, i, env->NewStringUTF(list[i]->GetString().GetRawString()));
            }

            return ret;
        }

        return NULL;
    }

    JNIEXPORT void JNICALL
    class_name(SetParamValue)(JNIEnv *env, jclass type, jbyteArray id, jfloat value)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            Csm::csmString res = ToString(env, id);

            model->SetParamValue(res, value);
        }
    }

    JNIEXPORT jfloatArray JNICALL
    class_name(GetParamMaximumValues)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            const csmFloat32 *list = model->GetParameterMaximumValues();
            if (list == NULL)
                return NULL;

            csmInt32 size = model->GetParameterCount();

            jfloatArray ret = (jfloatArray)env->NewFloatArray(size);

            env->SetFloatArrayRegion(ret, 0, size, list);

            return ret;
        }

        return NULL;
    }

    JNIEXPORT jfloatArray JNICALL
    class_name(GetParamMinimumValues)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            const csmFloat32 *list = model->GetParameterMinimumValues();
            if (list == NULL)
                return NULL;

            csmInt32 size = model->GetParameterCount();

            jfloatArray ret = (jfloatArray)env->NewFloatArray(size);

            env->SetFloatArrayRegion(ret, 0, size, list);

            return ret;
        }

        return NULL;
    }

    JNIEXPORT jfloatArray JNICALL
    class_name(GetParamDefaultValues)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            const csmFloat32 *list = model->GetParameterDefaultValues();
            if (list == NULL)
                return NULL;

            csmInt32 size = model->GetParameterCount();

            jfloatArray ret = (jfloatArray)env->NewFloatArray(size);

            env->SetFloatArrayRegion(ret, 0, size, list);

            return ret;
        }

        return NULL;
    }

    JNIEXPORT jfloatArray JNICALL
    class_name(GetParamValues)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            const csmFloat32 *list = model->GetParameterValues();
            if (list == NULL)
                return NULL;

            csmInt32 size = model->GetParameterCount();

            jfloatArray ret = (jfloatArray)env->NewFloatArray(size);

            env->SetFloatArrayRegion(ret, 0, size, list);

            return ret;
        }

        return NULL;
    }

    JNIEXPORT jobjectArray JNICALL
    class_name(GetParamIds)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            Csm::csmVector<Live2D::Cubism::Framework::CubismIdHandle> list = model->GetParamIds();
            jobjectArray ret = (jobjectArray)env->NewObjectArray(list.GetSize(), env->FindClass("java/lang/String"), env->NewStringUTF(""));

            for (int i = 0; i < list.GetSize(); i++)
            {
                env->SetObjectArrayElement(ret, i, env->NewStringUTF(list[i]->GetString().GetRawString()));
            }

            return ret;
        }

        return NULL;
    }

    JNIEXPORT void JNICALL
    class_name(EnableRandomMotion)(JNIEnv *env, jclass type, jboolean open)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();
        if (model)
        {
            model->SetEnableRandomMotion(open == JNI_TRUE);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetEyeBallX)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetEyeBallX(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetEyeBallY)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetEyeBallY(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetBodyAngleX)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetBodyAngleX(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetAngleX)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetAngleX(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetAngleY)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetAngleY(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetAngleZ)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetAngleZ(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(SetBreath)(JNIEnv *env, jclass type, jbyteArray id)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString res = ToString(env, id);

        if (model)
        {
            model->SetBreath(res);
        }
    }

    JNIEXPORT void JNICALL
    class_name(LoadModel)(JNIEnv *env, jclass type, jbyteArray path, jbyteArray name)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();

        char *chars = NULL;
        jbyte *bytes;
        int chars_len;

        bytes = env->GetByteArrayElements(path, 0);
        chars_len = env->GetArrayLength(path);
        chars = new char[chars_len + 1];
        memset(chars, 0, chars_len + 1);
        memcpy(chars, bytes, chars_len);
        chars[chars_len] = 0;

        Csm::csmString path1 = Csm::csmString(chars);

        env->ReleaseByteArrayElements(path, bytes, 0);
        delete[] chars;

        bytes = env->GetByteArrayElements(name, 0);
        chars_len = env->GetArrayLength(name);
        chars = new char[chars_len + 1];
        memset(chars, 0, chars_len + 1);
        memcpy(chars, bytes, chars_len);
        chars[chars_len] = 0;

        Csm::csmString name1 = Csm::csmString(chars);

        env->ReleaseByteArrayElements(name, bytes, 0);
        delete[] chars;

        l2d->LoadModel(path1, name1);
    }

    JNIEXPORT jbyteArray JNICALL
    class_name(GetExpression)(JNIEnv *env, jclass type, jint index)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString data1 = model->GetExpression(index);

        jbyteArray data = env->NewByteArray(data1.GetLength());
        env->SetByteArrayRegion(data, 0, data1.GetLength(), (jbyte *)data1.GetRawString());

        env->ReleaseByteArrayElements(data, env->GetByteArrayElements(data, JNI_FALSE), 0);

        return data;
    }

    JNIEXPORT jbyteArray JNICALL
    class_name(GetMotion)(JNIEnv *env, jclass type, jint index)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        Csm::csmString data1 = model->GetMotion(index);

        jbyteArray data = env->NewByteArray(data1.GetLength());
        env->SetByteArrayRegion(data, 0, data1.GetLength(), (jbyte *)data1.GetRawString());

        env->ReleaseByteArrayElements(data, env->GetByteArrayElements(data, JNI_FALSE), 0);

        return data;
    }

    JNIEXPORT jint JNICALL
    class_name(GetExpressionSize)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        return model->GetExpressionSize();
    }

    JNIEXPORT jint JNICALL
    class_name(GetMotionSize)(JNIEnv *env, jclass type)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        if (model == nullptr)
        {
        }

        return model->GetMotionSize();
    }

    JNIEXPORT void JNICALL
    class_name(StartMotion)(JNIEnv *env, jclass type, jbyteArray group, jint no, jint priority)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        char *chars = NULL;
        jbyte *bytes;
        bytes = env->GetByteArrayElements(group, 0);
        int chars_len = env->GetArrayLength(group);
        chars = new char[chars_len + 1];
        memset(chars, 0, chars_len + 1);
        memcpy(chars, bytes, chars_len);
        chars[chars_len] = 0;

        env->ReleaseByteArrayElements(group, bytes, 0);

        model->StartMotion(chars, no, priority);

        delete[] chars;
    }

    JNIEXPORT void JNICALL
    class_name(StartExpressions)(JNIEnv *env, jclass type, jbyteArray name)
    {
        LAppLive2DManager *l2d = LAppLive2DManager::GetInstance();
        LAppModel *model = l2d->GetModel();

        char *chars = NULL;
        jbyte *bytes;
        bytes = env->GetByteArrayElements(name, 0);
        int chars_len = env->GetArrayLength(name);
        chars = new char[chars_len + 1];
        memset(chars, 0, chars_len + 1);
        memcpy(chars, bytes, chars_len);
        chars[chars_len] = 0;

        env->ReleaseByteArrayElements(name, bytes, 0);

        model->SetExpression(chars);

        delete[] chars;
    }

    JNIEXPORT void JNICALL
    class_name(OnStart)(JNIEnv *env, jclass type)
    {
        LAppDelegate::GetInstance()->OnStart();
    }

    JNIEXPORT void JNICALL
    class_name(OnStop)(JNIEnv *env, jclass type)
    {
        LAppDelegate::GetInstance()->OnStop();
    }

    JNIEXPORT void JNICALL
    class_name(OnDestroy)(JNIEnv *env, jclass type)
    {
        LAppDelegate::GetInstance()->OnDestroy();
    }

    JNIEXPORT void JNICALL
    class_name(OnSurfaceCreated)(JNIEnv *env, jclass type)
    {
        LAppDelegate::GetInstance()->OnSurfaceCreate();
    }

    JNIEXPORT void JNICALL
    class_name(OnSurfaceChanged)(JNIEnv *env, jclass type, jint width, jint height)
    {
        LAppDelegate::GetInstance()->OnSurfaceChanged(width, height);
    }

    JNIEXPORT void JNICALL
    class_name(OnDrawFrame)(JNIEnv *env, jclass type)
    {
        LAppDelegate::GetInstance()->Run();
    }

    JNIEXPORT void JNICALL
    class_name(OnTouchesBegan)(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LAppDelegate::GetInstance()->OnTouchBegan(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    class_name(OnTouchesEnded)(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LAppDelegate::GetInstance()->OnTouchEnded(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    class_name(OnTouchesMoved)(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LAppDelegate::GetInstance()->OnTouchMoved(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    class_name(SetPos)(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LAppLive2DManager *manager = LAppLive2DManager::GetInstance();
        manager->x = pointX;
        manager->y = pointY;
    }

    JNIEXPORT void JNICALL
    class_name(SetScale)(JNIEnv *env, jclass type, jfloat scale)
    {
        LAppLive2DManager *manager = LAppLive2DManager::GetInstance();
        manager->scale = scale;
    }

    JNIEXPORT void JNICALL
    class_name(SetPosX)(JNIEnv *env, jclass type, jfloat data)
    {
        LAppLive2DManager *manager = LAppLive2DManager::GetInstance();
        manager->x = data;
    }

    JNIEXPORT void JNICALL
    class_name(SetPosY)(JNIEnv *env, jclass type, jfloat data)
    {
        LAppLive2DManager *manager = LAppLive2DManager::GetInstance();
        manager->y = data;
    }
}
