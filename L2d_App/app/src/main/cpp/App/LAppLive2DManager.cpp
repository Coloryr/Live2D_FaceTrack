/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#include "LAppLive2DManager.hpp"
#include <string>
#include <GLES2/gl2.h>
#include <Rendering/CubismRenderer.hpp>
#include "LAppPal.hpp"
#include "LAppDefine.hpp"
#include "LAppDelegate.hpp"
#include "LAppModel.hpp"
#include "LAppView.hpp"
#include "JniBridgeC.hpp"

using namespace Csm;
using namespace LAppDefine;
using namespace std;

namespace
{
    LAppLive2DManager *s_instance = NULL;

    void FinishedMotion(ACubismMotion *self)
    {
        LAppPal::PrintLog("Motion Finished: %x", self);
    }
}

LAppLive2DManager *LAppLive2DManager::GetInstance()
{
    if (s_instance == NULL)
    {
        s_instance = new LAppLive2DManager();
    }

    return s_instance;
}

void LAppLive2DManager::ReleaseInstance()
{
    if (s_instance != NULL)
    {
        delete s_instance;
    }

    s_instance = NULL;
}

LAppLive2DManager::LAppLive2DManager()
    : _viewMatrix(NULL),
      x(0.0f), y(0.0f), scale(1.0f),
      path(NULL), name(NULL),
      _model(NULL), loading(false)
{
    _viewMatrix = new CubismMatrix44();
}

LAppLive2DManager::~LAppLive2DManager()
{
    ReleaseAllModel();
}

void LAppLive2DManager::ReleaseAllModel()
{
    if (this->_model)
    {
        delete this->_model;
    }
}

LAppModel *LAppLive2DManager::GetModel() const
{
    return this->_model;
}

void LAppLive2DManager::OnDrag(csmFloat32 x, csmFloat32 y) const
{
    this->_model->SetDragging(x, y);
}

void LAppLive2DManager::OnTap(csmFloat32 x, csmFloat32 y)
{
    if (DebugLogEnable)
    {
        LAppPal::PrintLog("[APP]tap point: {x:%.2f y:%.2f}", x, y);
    }

    if (this->_model->HitTest(HitAreaNameHead, x, y))
    {
        if (DebugLogEnable)
        {
            LAppPal::PrintLog("[APP]hit area: [%s]", HitAreaNameHead);
        }
        this->_model->SetRandomExpression();
    }
    else if (this->_model->HitTest(HitAreaNameBody, x, y))
    {
        if (DebugLogEnable)
        {
            LAppPal::PrintLog("[APP]hit area: [%s]", HitAreaNameBody);
        }
        this->_model->StartRandomMotion(MotionGroupTapBody, PriorityNormal, FinishedMotion);
    }
}

void LAppLive2DManager::OnUpdate() const
{
    if (this->_model == NULL)
        return;

    int width = LAppDelegate::GetInstance()->GetWindowWidth();
    int height = LAppDelegate::GetInstance()->GetWindowHeight();

    CubismMatrix44 projection;

    projection.Translate(x, y);

    if (this->_model->GetModel()->GetCanvasWidth() > 1.0f && width < height)
    {
        // 横に長いモデルを縦長ウィンドウに表示する際モデルの横サイズでscaleを算出する
        this->_model->GetModelMatrix()->SetWidth(2.0f);
        projection.Scale(scale, static_cast<float>(width) / static_cast<float>(height) * scale);
    }
    else
    {
        projection.Scale(static_cast<float>(height) / static_cast<float>(width) * scale, scale);
    }

    // 必要があればここで乗算
    if (_viewMatrix != NULL)
    {
        projection.MultiplyByMatrix(_viewMatrix);
    }

    // モデル1体描画前コール
    LAppDelegate::GetInstance()->GetView()->PreModelDraw(*this->_model);

    this->_model->Update();
    this->_model->Draw(projection); ///< 参照渡しなのでprojectionは変質する

    // モデル1体描画後コール
    LAppDelegate::GetInstance()->GetView()->PostModelDraw(*this->_model);
}

void LAppLive2DManager::LoadModel(Csm::csmString path, Csm::csmString name)
{
    if (this->path)
    {
        delete[] this->path;
    }

    if (this->name)
    {
        delete[] this->name;
    }

    // ModelDir[]に保持したディレクトリ名から
    // model3.jsonのパスを決定する.
    // ディレクトリ名とmodel3.jsonの名前を一致させておくこと.
    csmString modelPath = path + "/";
    csmString modelJsonName = name + ".model3.json";

    if (DebugLogEnable)
    {
        LAppPal::PrintLog("[APP]model load: %s%s", modelPath.GetRawString(), modelJsonName.GetRawString());
    }

    int length = modelPath.GetLength() + 1;
    this->path = new char[length];
    strncpy(this->path, modelPath.GetRawString(), length);

    length = modelJsonName.GetLength() + 1;
    this->name = new char[length];
    strncpy(this->name, modelJsonName.GetRawString(), length);

    this->loading = true;
}

void LAppLive2DManager::InitModel()
{
    ReleaseAllModel();

    this->_model = new LAppModel();

    JniBridgeC::OnLoadModel(this->name);

    this->_model->LoadAssets((const char *)this->path, (const char *)this->name);

    /*
     * モデル半透明表示を行うサンプルを提示する。
     * ここでUSE_RENDER_TARGET、USE_MODEL_RENDER_TARGETが定義されている場合
     * 別のレンダリングターゲットにモデルを描画し、描画結果をテクスチャとして別のスプライトに張り付ける。
     */
    {
#if defined(USE_RENDER_TARGET)
        // LAppViewの持つターゲットに描画を行う場合、こちらを選択
        LAppView::SelectTarget useRenderTarget = LAppView::SelectTarget_ViewFrameBuffer;
#elif defined(USE_MODEL_RENDER_TARGET)
        // 各LAppModelの持つターゲットに描画を行う場合、こちらを選択
        LAppView::SelectTarget useRenderTarget = LAppView::SelectTarget_ModelFrameBuffer;
#else
        // デフォルトのメインフレームバッファへレンダリングする(通常)
        LAppView::SelectTarget useRenderTarget = LAppView::SelectTarget_None;
#endif

        LAppDelegate::GetInstance()->GetView()->SwitchRenderingTarget(useRenderTarget);

        // 別レンダリング先を選択した際の背景クリア色
        float clearColor[3] = {1.0f, 1.0f, 1.0f};
        LAppDelegate::GetInstance()->GetView()->SetRenderTargetClearColor(clearColor[0], clearColor[1], clearColor[2]);
    }

    this->loading = false;
}

void LAppLive2DManager::SetViewMatrix(CubismMatrix44 *m)
{
    for (int i = 0; i < 16; i++)
    {
        _viewMatrix->GetArray()[i] = m->GetArray()[i];
    }
}
