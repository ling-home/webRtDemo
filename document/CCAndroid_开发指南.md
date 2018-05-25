[TOC] 
## 1.概述
提供猎豹基础SDK，SDK端支持YUV视频格式，yuv流输入，yuv流输出；多人并发拉rtmp流，连麦拉rtc流。

### 1.1 功能特性
|            功能                            | 描述      | 
| :--------------------------------------- | :------- |
| 自定义视频流	 | 支持自定义视频作为推流的视频来源 |
| 推流  |支持推流到服务器|		
| 拉流   |支持从服务器订阅流|
|获取流状态|支持获取流的状态(发报数、收报数、丢包数、延时)|
|获取房间列表|支持获取直播中停止等状态的直播列表|
|支持https协议|	支持接口https请求|

### 1.2 阅读对象
本文档为技术文档，需要阅读者：
* 具备基本的Android开发能力
* 准备接入CC视频的猎豹SDK相关功能
* CC猎豹SDK，为了使用硬件媒体编解码器，建议API级别19以上。

## 2.开发准备

### 2.1 开发环境
* Android Studio : Android 开发IDE
* Android SDK : Android 官方SDK

### 2.2 混淆配置
ccclassroom.jar已经混淆过，如果需要对应用进行混淆，需要在混淆的配置文件增加如下代码，以防止SDK的二次混淆：
```
-keep public class com.bokecc.sdk.mobile.**{*;}
-keep public class tv.**{*;}
-keep public interface com.bokecc.sdk.mobile.**{*;}
-keep public class org.webrtc.**{*;}
```

## 3.快速集成

注：快速集成主要提供的是推流和拉流的功能(核心功能)。

首先，需要下载最新版本的SDK，下载地址为：

### 3.1 导入jar
|            名称                            | 描述      |
| :--------------------------------------- | :------- | 
| ccclassroom.jar	 | CC猎豹核心jar包	 | 

### 3.2 导入so
|            名称                            | 描述      |
| :--------------------------------------- | :------- | 
|libjingle_peerconnection.so|CC连麦依赖native库|

### 3.3 配置依赖库

修改 build.gradle，打开您的工程目录下的 build.gradle，确保已经添加了如下依赖：
```gradle
compile('io.socket:socket.io-client:0.8.3') {
        exclude group: 'org.json', module: 'json'
    }
compile 'com.squareup.okhttp3:okhttp:3.8.1'
compile files('libs/ccclassroom.jar')
 // Java CV
 compile group: 'org.bytedeco', name: 'javacv', version: '1.3.2'
 compile group: 'org.bytedeco.javacpp-presets', name: 'opencv', version: '3.1.0-1.3', classifier: 'android-arm'
 compile group: 'org.bytedeco.javacpp-presets', name: 'opencv', version: '3.1.0-1.3', classifier: 'android-x86'
 compile group: 'org.bytedeco.javacpp-presets', name: 'ffmpeg', version: '3.2.1-1.3', classifier: 'android-arm'
 compile group: 'org.bytedeco.javacpp-presets', name: 'ffmpeg', version: '3.2.1-1.3', classifier: 'android-x86'
```
### 3.4 创建SDK实例和视频数据输入代理

创建SDK实例：

```java
 mAtlasClient = new CCAtlasClient(this, Config.PUBLIC_KEY);
 mAtlasClient.addAtlasObserver(mClientObserver);
```
系统代理回调：

```java
 private CCAtlasClient.AtlasClientObserver mClientObserver = new CCAtlasClient.AtlasClientObserver() {
        @Override
        public void onServerDisconnected() {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        @Override
        public void onStreamAdded(CCStream stream) {
            if (stream.isRemoteIsLocal()) { // 不订阅自己的本地流
                return;
            }

            Log.e(TAG, "onStreamAdded: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL && stream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
                // 订阅
                mStream = stream;
                mSubableRemoteStreams.add(mStream);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addVideoView(mStream);
                    }
                });
            }
        }
        @Override
        public void onStreamRemoved(final CCStream stream) {
            Log.e(TAG, "onStreamRemoved: [ " + stream.getStreamId() + " ]");
            if (stream.getStreamType() != CCStream.REMOTE_MIX && stream.getStreamType() != CCStream.REMOTE_SCREEN &&
                    stream.getStreamType() != CCStream.LOCAL && stream.getStreamType() != CCStream.REMOTE_CUSTOMIZED) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      removeStreamView(stream);
                    }
                }, 2000);
            }

        }

        @Override
        public void onStreamError(String streamid, String errorMsg) {

        }
    };
```
添加流视图方法：

```java
 private synchronized void addVideoView(CCStream mStream) {
        if (mStream.getStreamType() != CCStream.REMOTE_MIX && mStream.getStreamType() != CCStream.REMOTE_SCREEN &&
                mStream.getStreamType() != CCStream.LOCAL) {
//            showProgress();
            final CCSurfaceRenderer mRemoteMixRenderer = new CCSurfaceRenderer(this);
            mRemoteMixRenderer.init(mAtlasClient.getEglBase().getEglBaseContext(), null);
            mRemoteMixRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            final VideoStreamView videoRemoteStreamView = new VideoStreamView();
            videoRemoteStreamView.setRenderer(mRemoteMixRenderer);
            videoRemoteStreamView.setStream(mStream);
            videoRemoteStreamView.setUserId(mStream.getUserid());
            mAtlasClient.subscribe(mStream, new CCAtlasCallBack<CCStream>() {
                @Override
                public void onSuccess(final CCStream ccStream) {
//                    dismissProgress();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int position;
                            position = mVideoStreamViews.size();
                            mVideoStreamViews.add(position,videoRemoteStreamView);
                            try {
                                videoRemoteStreamView.getStream().attach(videoRemoteStreamView.getRenderer());
                                mCurFragment.notifyItemChanged(videoRemoteStreamView, mVideoStreamViews.size() - 1, true);
                            } catch (StreamException e) {
                                showToast(e.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int errCode, String errMsg) {
//                    dismissProgress();
                }
            });
        }
```
移除流视图方法：

```java
 private synchronized void removeStreamView(CCStream stream) {
        int position = -1;
        VideoStreamView tempView = null;
        for (int i = 0; i < mVideoStreamViews.size(); i++) {
            VideoStreamView streamView = mVideoStreamViews.get(i);
            if (streamView.getStream().getStreamId().equals(stream.getStreamId())) {
                tempView = streamView;
                position = i;
                break;
            }
        }
        if (tempView == null)
            return;
//        mStreamViewMap.remove(stream);
        mVideoStreamViews.remove(tempView);
        mSubableRemoteStreams.remove(stream);
        mCurFragment.notifyItemChanged(tempView, position, false);
        stream.detach();
        mAtlasClient.unsubcribe(stream, null);
    }
```
创建本地流：

```java
 private void createLocalStream() {
        LocalStreamConfig config = new LocalStreamConfig.LocalStreamConfigBuilder().build();
        isFront = config.cameraType == LocalStreamConfig.CAMERA_FRONT;
        try {
            mLocalStream = mAtlasClient.createLocalStream(config);
        } catch (StreamException e) {
            Log.e("111", e.getLocalizedMessage());
            showToast(e.getMessage());
        }
    }
```
### 3.5 直播端创建直播间

下面是创建直播间的操作，创建直播成功以后开始推流：
```java
mAtlasClient.createLive(name, publisher_name, max_publishers,new CCAtlasCallBack<CCBaseBean>() {
            @Override
            public void onSuccess(CCBaseBean ccBaseBean) {
                dismissProgress();
                showToast("join room success");
                mStartClss.setVisibility(View.GONE);
                mStopClass.setVisibility(View.VISIBLE);
                StopStatus = false;
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
            }
        });
```
### 3.6 直播端结束直播

下面是结束直播，结束成功以后停止推流：
```java
mAtlasClient.stopLive(new CCAtlasCallBack<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dismissProgress();
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
            }
        });
```
### 3.7 观看端观看RTMP流

根据其返回的token加入到当前atlas推流房间内， 并通过rtmp地址获取视频流：
```java
mAtlasClient.joinLive(live_id, live_name, new CCAtlasCallBack<CCStream>() {
            @Override
            public void onSuccess(CCStream ccStream) {
                dismissProgress();
                try {
                    ccStream.attach(localCustomizedStreamRenderer);
                } catch (StreamException ignored) {
                }
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
            }
        });
```

### 3.8 观看端连麦

观看端通过atlas推流本地流到服务器，停止rtmp流：
```java
  mAtlasClient.publish(live_id, live_name, new CCAtlasCallBack<CCBaseBean>() {
            @Override
            public void onSuccess(CCBaseBean ccBaseBean) {
                dismissProgress();
                showToast("publish Success");
                if (mLocalStream != null) {
                    mAtlasClient.leaveStopYUV(false);
                    isVideo = true;
                }
                mlianmai_normal.setVisibility(View.GONE);
                mlianmai_pressed.setVisibility(View.VISIBLE);
                mlianmai_normal.setEnabled(true);
                isLive = true;
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
                Log.i(TAG, "onFailure: ");
            }
        });
```
### 3.9 观看端下麦

观看端停止向atlas推流，并切换本地输出流为rtmp流：
```java
mAtlasClient.unpublish(new CCAtlasCallBack<CCStream>() {

            @Override
            public void onSuccess(CCStream ccStream) {
                dismissProgress();
                showToast("unpublish Success");
                try {
                    ccStream.attach(mLocalRenderer);
                    lianmaiStop = ccStream;
                } catch (StreamException e) {
                    e.printStackTrace();
                }
                isLive = false;
                mlianmai_normal.setVisibility(View.VISIBLE);
                mlianmai_pressed.setVisibility(View.GONE);
                mlianmai_pressed.setEnabled(true);
                isVideoSurface = true;
                status = true;
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
                showToast(errMsg);
            }
        });
```
## 4.功能使用

### 4.1 获取流状态
```java
getConnectionStats(stream, new CCAtlasCallBack<ConnectionStatsWrapper>() {
              @Override
              public void onSuccess(ConnectionStatsWrapper connectionStatsWrapper) 
             {
              }

            @Override
            public void onFailure(int errCode, String errMsg) {

           }
        });
```
### 4.2 获取直播间的列表
```java
 mAtlasClient.liveList(1, new CCAtlasCallBack<LiveListSet>() {
            @Override
            public void onSuccess(final LiveListSet mLiveListSet) {
                dismissProgress();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListAdapter.bindDatas(mLiveListSet.getLiveListSet());
                        mListAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(int errCode, String errMsg) {
                dismissProgress();
            }
        });
```
## 5.API查询
Doc目录打开index.html文件

## 6.Q&A
### 6.1 网络库兼容的问题

建议一：
```gradle
compile 'com.squareup.okhttp3:okhttp:3.8.1'
compile('io.socket:socket.io-client:0.8.3') {
	exclude group: 'org.json', module: 'json'
}
```

建议二：
```gradle
compile 'com.squareup.okhttp3:okhttp:3.3.1'
compile('io.socket:socket.io-client:0.7.0') {
	exclude group: 'org.json', module: 'json'
}
```
最好还是使用最高的版本使用建议一，由于需要和服务端兼容，目前支持的io.sokcet版本最高是0.8.3。
