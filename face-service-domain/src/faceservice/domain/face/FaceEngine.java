package faceservice.domain.face;

import java.util.List;

/**
 * 人脸引擎抽象（业务层唯一入口，禁止直接依赖 mica-ai/ONNX 类型）。
 * 当前唯一实现为 mica-ai-face（YuNet + SFace 128d + MiniFASNetV2）。
 */
public interface FaceEngine {

    /**
     * 检测图中全部人脸。
     */
    List<FaceDetection> detect(byte[] image);

    /**
     * 从多帧中选取最大人脸并提取 128d 特征；无脸抛 NO_FACE。
     */
    float[] extractLargest(List<byte[]> frames);

    /**
     * 多帧活体投票：任一攻击帧判 SPOOF；live 占比达阈值判 REAL，否则 FUZZY。
     */
    LivenessVote checkLiveness(List<byte[]> frames);

    /**
     * 逐帧关键点几何指标（主动动作校验用）。
     */
    List<FrameMetric> analyze(List<byte[]> frames);

    /**
     * 两个 128d 特征的余弦相似度。
     */
    double compare(float[] left, float[] right);

    /**
     * 引擎特征版本（写入档案与索引，跨版本禁止比对）。
     */
    String modelVersion();
}
