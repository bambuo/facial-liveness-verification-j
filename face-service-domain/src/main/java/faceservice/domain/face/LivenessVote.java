package faceservice.domain.face;

/**
 * 多帧活体投票结果：最低活体分 + live 帧占比。
 */
public record LivenessVote(LivenessStatus status, float minScore, float liveRatio) {
}
