package faceservice.domain.face;

/**
 * 单帧关键点几何指标（由 YuNet 5 点计算，用于主动动作校验）。
 * yawRatio = (鼻尖x − 双眼中点x) / 瞳距，镜像图下：负=朝画面左转，正=朝右；
 * mouthRatio = 鼻尖到嘴心中点距离 / 瞳距，张嘴时增大（相对基线判定）。
 */
public record FrameMetric(boolean faceFound, double yawRatio, double mouthRatio) {
}
