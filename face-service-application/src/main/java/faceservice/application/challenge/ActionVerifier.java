package faceservice.application.challenge;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.domain.face.FaceEngine;
import faceservice.domain.face.FrameMetric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主动动作校验（大厂式：张嘴/转头），全部基于服务端 YuNet 5 点几何，不信任客户端判定。
 * 两个入口：
 * - {@link #checkAction}：单动作预检（页面分段引导用，不消费挑战、不写库）；
 * - {@link #verify}：最终整单校验（enroll/verify 提交时，逐段 + 全帧口径）。
 * 几何口径：yawRatio = (鼻尖x − 双眼中点x)/瞳距（镜像图：负=用户左转）；张嘴 = 鼻唇距/瞳距 相对基线增幅。
 */
@Component
public class ActionVerifier {

    private final FaceEngine engine;
    private final double yawThreshold;
    private final double straightYaw;
    private final double mouthOpenRatio;
    private final double passRatio;
    public ActionVerifier(FaceEngine engine,
                          @Value("${faceservice.face.yaw-threshold:0.20}") double yawThreshold,
                          @Value("${faceservice.face.straight-yaw:0.15}") double straightYaw,
                          @Value("${faceservice.face.mouth-open-ratio:1.08}") double mouthOpenRatio,
                          @Value("${faceservice.face.action-pass-ratio:0.5}") double passRatio) {
        this.engine = engine;
        this.yawThreshold = yawThreshold;
        this.straightYaw = straightYaw;
        this.mouthOpenRatio = mouthOpenRatio;
        this.passRatio = passRatio;
    }

    private static String fmt(double v) {
        return java.math.BigDecimal.valueOf(v).setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 单动作预检：frames 为当前动作的近期帧；baselineFrames 为正视段帧（张嘴判定基线，可空）。
     */
    public ActionCheck checkAction(String action, List<byte[]> frames, List<byte[]> baselineFrames) {
        if (frames == null || frames.isEmpty()) {
            return new ActionCheck(false, "无帧");
        }
        List<FrameMetric> metrics = engine.analyze(frames);
        long withFace = metrics.stream().filter(FrameMetric::faceFound).count();
        if (withFace == 0) {
            return new ActionCheck(false, "NO_FACE");
        }
        Double baseline = baselineFor(baselineFrames);
        long passed = metrics.stream().filter(m -> satisfies(action, m, baseline)).count();
        boolean satisfied = passed >= Math.max(1, (long) Math.ceil(metrics.size() * passRatio));
        return new ActionCheck(satisfied, stats(metrics));
    }

    /**
     * 最终整单校验：帧数必须与动作序列精确匹配，逐段判定。
     */
    public void verify(List<String> actions, int framesPerAction, List<byte[]> frames) {
        if (actions == null || actions.isEmpty() || framesPerAction <= 0) {
            return; // 旧挑战（无动作序列）跳过
        }
        if (frames == null || frames.size() != actions.size() * framesPerAction) {
            throw new FaceServiceException(ErrorCode.AUTH_INVALID, "帧数与动作序列不匹配");
        }
        List<FrameMetric> metrics = engine.analyze(frames);
        Double mouthBaseline = null;
        for (int a = 0; a < actions.size(); a++) {
            String action = actions.get(a);
            int from = a * framesPerAction;
            List<FrameMetric> phase = metrics.subList(from, from + framesPerAction);
            long withFace = phase.stream().filter(FrameMetric::faceFound).count();
            if (withFace == 0) {
                throw new FaceServiceException(ErrorCode.NO_FACE);
            }
            if ("straight".equals(action)) {
                mouthBaseline = phase.stream().filter(FrameMetric::faceFound)
                        .mapToDouble(FrameMetric::mouthRatio).min().orElse(0);
            }
            final Double baselineForPhase = mouthBaseline;
            long passed = phase.stream().filter(m -> satisfies(action, m, baselineForPhase)).count();
            if (passed < Math.max(1, (long) Math.ceil(framesPerAction * passRatio))) {
                var ys = phase.stream().filter(FrameMetric::faceFound).mapToDouble(FrameMetric::yawRatio).summaryStatistics();
                var ms = phase.stream().filter(FrameMetric::faceFound).mapToDouble(FrameMetric::mouthRatio).summaryStatistics();
                throw new FaceServiceException(ErrorCode.ACTION_MISMATCH,
                        "动作未完成：" + action + "（yaw " + fmt(ys.getMin()) + "~" + fmt(ys.getMax())
                                + "，mouth " + fmt(ms.getMin()) + "~" + fmt(ms.getMax()) + "）");
            }
        }
    }

    private boolean satisfies(String action, FrameMetric m, Double mouthBaseline) {
        if (!m.faceFound()) {
            return false;
        }
        return switch (action) {
            case "straight" -> Math.abs(m.yawRatio()) <= straightYaw;
            case "turn_left" -> m.yawRatio() <= -yawThreshold;
            case "turn_right" -> m.yawRatio() >= yawThreshold;
            case "mouth_open" -> mouthBaseline != null && m.mouthRatio() >= mouthBaseline * mouthOpenRatio;
            default -> true;
        };
    }

    private Double baselineFor(List<byte[]> baselineFrames) {
        if (baselineFrames == null || baselineFrames.isEmpty()) {
            return null;
        }
        return engine.analyze(baselineFrames).stream().filter(FrameMetric::faceFound)
                .mapToDouble(FrameMetric::mouthRatio).min().orElse(0);
    }

    private String stats(List<FrameMetric> metrics) {
        var ys = metrics.stream().filter(FrameMetric::faceFound).mapToDouble(FrameMetric::yawRatio).summaryStatistics();
        var ms = metrics.stream().filter(FrameMetric::faceFound).mapToDouble(FrameMetric::mouthRatio).summaryStatistics();
        return "yaw " + fmt(ys.getMin()) + "~" + fmt(ys.getMax()) + ", mouth " + fmt(ms.getMin()) + "~" + fmt(ms.getMax());
    }

    public record ActionCheck(boolean satisfied, String detail) {
    }
}
