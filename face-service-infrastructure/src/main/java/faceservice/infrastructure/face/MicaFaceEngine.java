package faceservice.infrastructure.face;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.domain.face.*;
import net.dreamlu.mica.ai.face.alignment.FaceAligner;
import net.dreamlu.mica.ai.face.detection.FaceDetector;
import net.dreamlu.mica.ai.face.liveness.LivenessDetector;
import net.dreamlu.mica.ai.face.model.FaceBox;
import net.dreamlu.mica.ai.face.model.LivenessResult;
import net.dreamlu.mica.ai.face.recognition.FeatureExtractor;
import net.dreamlu.mica.ai.face.util.ImageUtils;
import org.opencv.core.Mat;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * mica-ai-face 引擎适配（YuNet 检测 + SFace 128d 特征 + MiniFASNetV2 活体）。
 * 业务层只依赖 {@link FaceEngine}，不感知 mica-ai/ONNX 类型。
 */
@Component
public class MicaFaceEngine implements FaceEngine {

    /**
     * live 帧占比达到该值判 REAL，否则 FUZZY（任一攻击帧直接 SPOOF）。
     */
    private static final double LIVE_RATIO_THRESHOLD = 0.5;

    private final FaceDetector detector;
    private final FaceAligner aligner;
    private final FeatureExtractor extractor;
    private final LivenessDetector livenessDetector;

    public MicaFaceEngine(FaceDetector detector, FaceAligner aligner,
                          FeatureExtractor extractor, LivenessDetector livenessDetector) {
        this.detector = detector;
        this.aligner = aligner;
        this.extractor = extractor;
        this.livenessDetector = livenessDetector;
    }

    /**
     * YuNet 5 点：双眼/鼻尖/双侧嘴角（顺序无关：取中点与距离）。
     */
    private static FrameMetric metricsOf(FaceBox box) {
        float[][] lm = box.getLandmarks();
        if (lm == null || lm.length < 5) {
            return new FrameMetric(true, 0, 0);
        }
        double eyeMidX = (lm[0][0] + lm[1][0]) / 2.0;
        double eyeDist = Math.hypot(lm[0][0] - lm[1][0], lm[0][1] - lm[1][1]);
        if (eyeDist < 1e-3) {
            return new FrameMetric(true, 0, 0);
        }
        double noseX = lm[2][0];
        double mouthMidX = (lm[3][0] + lm[4][0]) / 2.0;
        double mouthMidY = (lm[3][1] + lm[4][1]) / 2.0;
        double yawRatio = (noseX - eyeMidX) / eyeDist;
        double mouthRatio = Math.hypot(noseX - mouthMidX, lm[2][1] - mouthMidY) / eyeDist;
        return new FrameMetric(true, yawRatio, mouthRatio);
    }

    private static FaceBox largestFace(List<FaceBox> boxes) {
        return boxes.stream().max(Comparator.comparingDouble(MicaFaceEngine::areaOf)).orElse(null);
    }

    private static double areaOf(FaceBox box) {
        return (double) Math.max(0, box.getX2() - box.getX1()) * Math.max(0, box.getY2() - box.getY1());
    }

    private static FaceDetection toDetection(FaceBox box) {
        return new FaceDetection(box.getX1(), box.getY1(), box.getX2(), box.getY2(), box.getScore());
    }

    @Override
    public List<FaceDetection> detect(byte[] image) {
        Mat mat = ImageUtils.byteArrayToMat(image);
        try {
            return detector.detect(mat).stream()
                    .map(MicaFaceEngine::toDetection)
                    .toList();
        } finally {
            ImageUtils.releaseAll(mat);
        }
    }

    @Override
    public float[] extractLargest(List<byte[]> frames) {
        float[] best = null;
        float bestArea = -1;
        for (byte[] frame : frames) {
            Mat mat = ImageUtils.byteArrayToMat(frame);
            try {
                FaceBox box = largestFace(detector.detect(mat));
                if (box == null) {
                    continue;
                }
                float area = Math.max(0, box.getX2() - box.getX1()) * Math.max(0, box.getY2() - box.getY1());
                if (area <= bestArea) {
                    continue;
                }
                Mat aligned = aligner.align(mat, box);
                try {
                    best = extractor.extract(aligned);
                    bestArea = area;
                } finally {
                    ImageUtils.releaseAll(aligned);
                }
            } finally {
                ImageUtils.releaseAll(mat);
            }
        }
        if (best == null) {
            throw new FaceServiceException(ErrorCode.NO_FACE);
        }
        return best;
    }

    @Override
    public LivenessVote checkLiveness(List<byte[]> frames) {
        int live = 0;
        int attack = 0;
        float minScore = 1f;
        for (byte[] frame : frames) {
            Mat mat = ImageUtils.byteArrayToMat(frame);
            try {
                FaceBox box = largestFace(detector.detect(mat));
                if (box == null) {
                    continue;
                }
                LivenessResult result = livenessDetector.check(mat, box);
                minScore = Math.min(minScore, result.getLiveScore());
                if (!result.isLive()) {
                    attack++;
                } else {
                    live++;
                }
            } finally {
                ImageUtils.releaseAll(mat);
            }
        }
        int judged = live + attack;
        if (judged == 0) {
            throw new FaceServiceException(ErrorCode.NO_FACE);
        }
        float ratio = (float) live / judged;
        LivenessStatus status = attack > 0 ? LivenessStatus.SPOOF
                : ratio >= LIVE_RATIO_THRESHOLD ? LivenessStatus.REAL : LivenessStatus.FUZZY;
        return new LivenessVote(status, minScore, ratio);
    }

    @Override
    public List<FrameMetric> analyze(List<byte[]> frames) {
        List<FrameMetric> out = new java.util.ArrayList<>(frames.size());
        for (byte[] frame : frames) {
            Mat mat = ImageUtils.byteArrayToMat(frame);
            try {
                FaceBox box = largestFace(detector.detect(mat));
                out.add(box == null ? new FrameMetric(false, 0, 0) : metricsOf(box));
            } finally {
                ImageUtils.releaseAll(mat);
            }
        }
        return out;
    }

    @Override
    public double compare(float[] left, float[] right) {
        return FeatureExtractor.compare(left, right);
    }

    @Override
    public String modelVersion() {
        return "mica-sface-128d-1.0.2";
    }
}
