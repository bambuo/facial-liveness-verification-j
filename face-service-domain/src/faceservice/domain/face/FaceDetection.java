package faceservice.domain.face;

/**
 * 人脸检测结果（值对象，隔离底层引擎类型）。
 */
public record FaceDetection(float x1, float y1, float x2, float y2, float score) {

    public float area() {
        return Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
    }
}
