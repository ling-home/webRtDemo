package ccsskt.bokecc.base.example.view;

import com.bokecc.sskt.base.CCStream;
import com.bokecc.sskt.base.renderer.CCSurfaceRenderer;

/**
 * 作者 ${王德惠}.<br/>
 */
public class VideoStreamView {

    private CCStream mStream;
    private CCSurfaceRenderer mRenderer;
    private String mUserId;


    public CCStream getStream() {
        return mStream;
    }

    public void setStream(CCStream stream) {
        mStream = stream;
    }

    public CCSurfaceRenderer getRenderer() {
        return mRenderer;
    }

    public void setRenderer(CCSurfaceRenderer renderer) {
        mRenderer = renderer;
    }
    /**
     * 获取用户id
     * @return 用户id
     */
    public String getUserId() {
        return mUserId;
    }

    /**
     * 设置用户id
     * @param userId 用户id
     */
    public void setUserId(String userId) {
        mUserId = userId;
    }

}