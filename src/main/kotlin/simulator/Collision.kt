package simulator

import model.Arena
import kotlin.math.*


// Very evil, places collision on stack
typealias Collision = DoubleArray

inline val Collision.distance get() = this[3]
inline val Collision.normal get() = this.sliceArray(0..2)

@Suppress("FunctionName")
fun Collision(distance: Double, normal: Vec3D) =
        doubleArrayOf(normal[0], normal[1], normal[2], distance)

fun minCollision(a: Collision, b: Collision) =
        if(a.distance > b.distance) b else a

object ArenaCollision {
    fun danToArena(point: Vec3D, arena: Arena): Collision {
        val negateX = point.x < 0
        val negateZ = point.z < 0

        if (negateX)
            point.x = -point.x
        if (negateZ)
            point.z = -point.z

        val result = danToArenaQuarter(point, arena)

        if (negateX)
            result.normal.x = -result.normal.x
        if (negateZ)
            result.normal.z = -result.normal.z

        if (negateX)
            point.x = -point.x
        if (negateZ)
            point.z = -point.z

        return result
    }

    private fun danToPlane(point: Vec3D, pointOnPlane: Vec3D, planeNormal: Vec3D) =
            Collision(
                    distance = (point - pointOnPlane) dot planeNormal,
                    normal = planeNormal)

    private fun danToSphereInner(point: Vec3D, sphereCenter: Vec3D, sphereRadius: Double) =
            Collision(
                    distance = sphereRadius - (point - sphereCenter).length,
                    normal = (sphereCenter - point).normalized)

    private fun danToSphereOuter(point: Vec3D, sphereCenter: Vec3D, sphereRadius: Double) =
            Collision(
                    distance = (point - sphereCenter).length - sphereRadius,
                    normal = (point - sphereCenter).normalized)

    private fun danToArenaQuarter(point: Vec3D, arena: Arena): Collision {
        // Ground
        var dan = danToPlane(point, Vec3D(0.0, 0.0, 0.0), Vec3D(0.0, 1.0, 0.0))

        // Ceiling
        dan = minCollision(dan, danToPlane(point, Vec3D(0.0, arena.height, 0.0), Vec3D(0.0, -1.0, 0.0)))

        // Side x
        dan = minCollision(dan, danToPlane(point, Vec3D(arena.width / 2, 0.0, 0.0), Vec3D(-1.0, 0.0, 0.0)))

        // Side z (goal)
        dan = minCollision(dan, danToPlane(
                point,
                Vec3D(0.0, 0.0, (arena.depth / 2) + arena.goal_depth),
                Vec3D(0.0, 0.0, -1.0)))

        // Side z
        val vvec = Vec3D(point.x, point.y, 0.0) -
                Vec3D(
                        (arena.goal_width / 2) - arena.goal_top_radius,
                        arena.goal_height - arena.goal_top_radius,
                        0.0)
        if (point.x >= (arena.goal_width / 2) + arena.goal_side_radius
                || point.y >= arena.goal_height + arena.goal_side_radius
                || (vvec.x > 0 && vvec.y > 0 && vvec.length >= arena.goal_top_radius + arena.goal_side_radius))
            dan = minCollision(dan, danToPlane(
                    point,
                    Vec3D(
                            0.0,
                            0.0,
                            arena.depth / 2),
                    Vec3D(
                            0.0,
                            0.0,
                            -1.0)))

        // Side x & ceiling (goal)
        if (point.z >= (arena.depth / 2) + arena.goal_side_radius) {
            // x
            dan = minCollision(dan, danToPlane(
                    point,
                    Vec3D(arena.goal_width / 2, 0.0, 0.0),
                    Vec3D(-1.0, 0.0, 0.0)))
            // y
            dan = minCollision(dan, danToPlane(
                    point,
                    Vec3D(0.0, arena.goal_height, 0.0),
                    Vec3D(0.0, -1.0, 0.0)))
        }

        // Goal back corners
        if (point.z > (arena.depth / 2) + arena.goal_depth - arena.bottom_radius)
            dan = minCollision(dan, danToSphereInner(
                    point,
                    Vec3D(
                            clamp(
                                    point.x,
                                    arena.bottom_radius - (arena.goal_width / 2),
                                    (arena.goal_width / 2) - arena.bottom_radius
                            ),
                            clamp(
                                    point.y,
                                    arena.bottom_radius,
                                    arena.goal_height - arena.goal_top_radius),
                            (arena.depth / 2) + arena.goal_depth - arena.bottom_radius),
                    arena.bottom_radius))

        // Corner
        if (point.x > (arena.width / 2) - arena.corner_radius && point.z > (arena.depth / 2) - arena.corner_radius)
            dan = minCollision(dan, danToSphereInner(
                    point,
                    Vec3D(
                            (arena.width / 2) - arena.corner_radius,
                            point.y,
                            (arena.depth / 2) - arena.corner_radius
                    ),
                    arena.corner_radius))

        // Goal outer corner
        if (point.z < (arena.depth / 2) + arena.goal_side_radius) {
            // Side x
            if (point.x < (arena.goal_width / 2) + arena.goal_side_radius)
                dan = minCollision(dan, danToSphereOuter(
                        point,
                        Vec3D(
                                (arena.goal_width / 2) + arena.goal_side_radius,
                                point.y,
                                (arena.depth / 2) + arena.goal_side_radius
                        ),
                        arena.goal_side_radius))
            // Ceiling
            if (point.y < arena.goal_height + arena.goal_side_radius)
                dan = minCollision(dan, danToSphereOuter(
                        point,
                        Vec3D(
                                point.x,
                                arena.goal_height + arena.goal_side_radius,
                                (arena.depth / 2) + arena.goal_side_radius
                        ),
                        arena.goal_side_radius))
            // Top corner
            varZ o = Vec3D(
                    (arena.goal_width / 2) - arena.goal_top_radius,
                    arena.goal_height - arena.goal_top_radius,
                    0.0
            )
            val vec = Vec3D(point.x, point.y, 0.0) - o
            if (vec.x > 0 && vec.y > 0)
                o += vec.normalized * (arena.goal_top_radius + arena.goal_side_radius)
            dan = minCollision(dan, danToSphereOuter(
                    point,
                    Vec3D(o.x, o.y, (arena.depth / 2) + arena.goal_side_radius),
                    arena.goal_side_radius))
        }

        // Goal inside top corners
        if (point.z > (arena.depth / 2) + arena.goal_side_radius
                && point.y > arena.goal_height - arena.goal_top_radius) {
            // Side x
            if (point.x > (arena.goal_width / 2) - arena.goal_top_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                (arena.goal_width / 2) - arena.goal_top_radius,
                                arena.goal_height - arena.goal_top_radius,
                                point.z
                        ),
                        arena.goal_top_radius))
            // Side z
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.goal_height - arena.goal_top_radius,
                                (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius
                        ),
                        arena.goal_top_radius))
        }

        // Bottom corners
        if (point.y < arena.bottom_radius) {
            // Side x
            if (point.x > (arena.width / 2) - arena.bottom_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                (arena.width / 2) - arena.bottom_radius,
                                arena.bottom_radius,
                                point.z
                        ),
                        arena.bottom_radius))
            // Side z
            if (point.z > (arena.depth / 2) - arena.bottom_radius
                    && point.x >= (arena.goal_width / 2) + arena.goal_side_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.bottom_radius,
                                (arena.depth / 2) - arena.bottom_radius
                        ),
                        arena.bottom_radius))
            // Side z (goal)
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.bottom_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.bottom_radius,
                                (arena.depth / 2) + arena.goal_depth - arena.bottom_radius
                        ),
                        arena.bottom_radius))
            // Goal outer corner
            var o = Vec3D(
                    (arena.goal_width / 2) + arena.goal_side_radius,
                    (arena.depth / 2) + arena.goal_side_radius,
                    0.0
            )
            val v = Vec3D(point.x, point.z, 0.0) - o
            if (v.x < 0 && v.y < 0
                    && v.length < arena.goal_side_radius + arena.bottom_radius) {
                o += v.normalized * (arena.goal_side_radius + arena.bottom_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(o.x, arena.bottom_radius, o.y),
                        arena.bottom_radius))
            }
            // Side x (goal)
            if (point.z >= (arena.depth / 2) + arena.goal_side_radius
                    && point.x > (arena.goal_width / 2) - arena.bottom_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                (arena.goal_width / 2) - arena.bottom_radius,
                                arena.bottom_radius,
                                point.z
                        ),
                        arena.bottom_radius))
            // Corner
            if (point.x > (arena.width / 2) - arena.corner_radius
                    && point.z > (arena.depth / 2) - arena.corner_radius) {
                val cornerO = Vec3D(
                        (arena.width / 2) - arena.corner_radius,
                        (arena.depth / 2) - arena.corner_radius,
                        0.0
                )
                var n = Vec3D(point.x, point.z, 0.0) - cornerO
                val dist = n.length
                if (dist > arena.corner_radius - arena.bottom_radius) {
                    n /= dist
                    val o2 = cornerO + n * (arena.corner_radius - arena.bottom_radius)
                    dan = minCollision(dan, danToSphereInner(
                            point,
                            Vec3D(o2.x, arena.bottom_radius, o2.y),
                            arena.bottom_radius))
                }
            }
        }

        // Ceiling corners
        if (point.y > arena.height - arena.top_radius) {
            // Side x
            if (point.x > (arena.width / 2) - arena.top_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                (arena.width / 2) - arena.top_radius,
                                arena.height - arena.top_radius,
                                point.z
                        ),
                        arena.top_radius))
            // Side z
            if (point.z > (arena.depth / 2) - arena.top_radius)
                dan = minCollision(dan, danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.height - arena.top_radius,
                                (arena.depth / 2) - arena.top_radius
                        ),
                        arena.top_radius))

            // Corner
            if (point.x > (arena.width / 2) - arena.corner_radius
                    && point.z > (arena.depth / 2) - arena.corner_radius) {
                val cornerO = Vec3D(
                        (arena.width / 2) - arena.corner_radius,
                        (arena.depth / 2) - arena.corner_radius,
                        0.0
                )
                val dv = Vec3D(point.x, point.z, 0.0) - cornerO
                if (dv.length > arena.corner_radius - arena.top_radius) {
                    val n = dv.normalized
                    val o2 = cornerO + n * (arena.corner_radius - arena.top_radius)
                    dan = minCollision(dan, danToSphereInner(
                            point,
                            Vec3D(o2.x, arena.height - arena.top_radius, o2.y),
                            arena.top_radius))
                }
            }
        }

        return dan
    }
}


fun clamp(it: Double, mn: Double, mx: Double) =
        max(mn, min(mx, it))
