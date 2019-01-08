package simulator

import model.Arena
import java.security.cert.CollectionCertStoreParameters
import kotlin.math.*


class Collision(var normal: Vec3D, var distance: Double) {
    fun minOf(other: Collision) {
        if (distance > other.distance) {
            normal = other.normal
            distance = other.distance
        }
    }
}


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
        val dan = danToPlane(point, Vec3D(), Vec3D(0.0, 1.0))

        ceiling(point, dan, arena)
        sideX(point, dan, arena)
        sideZGoal(point, dan, arena)
        sideZ(point, dan, arena)
        sideXAndCeilingGoal(point, dan, arena)
        goalBackCorners(point, dan, arena)
        corner(point, dan, arena)
        goalOuterCorner(point, dan, arena)
        goalInsideTopCorners(point, dan, arena)
        bottomCorners(point, dan, arena)
        ceilingCorners(point, dan, arena)

        return dan
    }

    private fun ceiling(point: Vec3D, dan: Collision, arena: Arena) {
        dan.minOf(danToPlane(point, Vec3D(0.0, arena.height), Vec3D(0.0, -1.0)))
    }

    private fun sideX(point: Vec3D, dan: Collision, arena: Arena) {
        dan.minOf(danToPlane(point, Vec3D(arena.width / 2), Vec3D(-1.0)))
    }

    private fun sideZGoal(point: Vec3D, dan: Collision, arena: Arena) {
        // Side z (goal)
        dan.minOf(danToPlane(
                point,
                Vec3D(0.0, 0.0, (arena.depth / 2) + arena.goal_depth),
                Vec3D(0.0, 0.0, -1.0)))
    }

    private fun sideZ(point: Vec3D, dan: Collision, arena: Arena) {
        // Side z
        val v = Vec3D(point.x, point.y) -
                Vec3D(
                        (arena.goal_width / 2) - arena.goal_top_radius,
                        arena.goal_height - arena.goal_top_radius)
        if (point.x >= (arena.goal_width / 2) + arena.goal_side_radius
                || point.y >= arena.goal_height + arena.goal_side_radius
                || (
                        v.x > 0
                                && v.y > 0
                                && v.length >= arena.goal_top_radius + arena.goal_side_radius))
            dan.minOf(danToPlane(
                    point,
                    Vec3D(
                            0.0,
                            0.0,
                            arena.depth / 2),
                    Vec3D(
                            0.0,
                            0.0,
                            -1.0)))
    }

    private fun sideXAndCeilingGoal(point: Vec3D, dan: Collision, arena: Arena) {
        // Side x & ceiling (goal)
        if (point.z >= arena.depth / 2 + arena.goal_side_radius) {
            // x
            dan.minOf(danToPlane(
                    point,
                    Vec3D(arena.goal_width / 2),
                    Vec3D(-1.0)))
            // y
            dan.minOf(danToPlane(
                    point,
                    Vec3D(0.0, arena.goal_height),
                    Vec3D(0.0, -1.0)))
        }
    }

    private fun goalBackCorners(point: Vec3D, dan: Collision, arena: Arena) {
        // Goal back corners
        if (point.z > arena.depth / 2 + arena.goal_depth - arena.bottom_radius)
            dan.minOf(danToSphereInner(
                    point,
                    Vec3D(
                            clamp(
                                    point.x,
                                    arena.bottom_radius - arena.goal_width / 2,
                                    arena.goal_width / 2 - arena.bottom_radius
                            ),
                            clamp(
                                    point.y,
                                    arena.bottom_radius,
                                    arena.goal_height - arena.goal_top_radius),
                            arena.depth / 2 + arena.goal_depth - arena.bottom_radius),
                    arena.bottom_radius))
    }

    private fun corner(point: Vec3D, dan: Collision, arena: Arena) {
        // Corner
        if (point.x > arena.width / 2 - arena.corner_radius
                && point.z > arena.depth / 2 - arena.corner_radius)
            dan.minOf(danToSphereInner(
                    point,
                    Vec3D(
                            arena.width / 2 - arena.corner_radius,
                            point.y,
                            arena.depth / 2 - arena.corner_radius
                    ),
                    arena.corner_radius))
    }

    private fun goalOuterCorner(point: Vec3D, dan: Collision, arena: Arena) {
        // Goal outer corner
        if (point.z < arena.depth / 2 + arena.goal_side_radius) {
            // Side x
            if (point.x < arena.goal_width / 2 + arena.goal_side_radius)
                dan.minOf(danToSphereOuter(
                        point,
                        Vec3D(
                                arena.goal_width / 2 + arena.goal_side_radius,
                                point.y,
                                arena.depth / 2 + arena.goal_side_radius
                        ),
                        arena.goal_side_radius))
            // Ceiling
            if (point.y < arena.goal_height + arena.goal_side_radius)
                dan.minOf(danToSphereOuter(
                        point,
                        Vec3D(
                                point.x,
                                arena.goal_height + arena.goal_side_radius,
                                arena.depth / 2 + arena.goal_side_radius
                        ),
                        arena.goal_side_radius))
            // Top corner
            var o = Vec3D(
                    arena.goal_width / 2 - arena.goal_top_radius,
                    arena.goal_height - arena.goal_top_radius
            )
            val vec = point.copy(z = 0.0) - o
            if (vec.x > 0 && vec.y > 0) {
                o += vec.normalized * (arena.goal_top_radius + arena.goal_side_radius)
                dan.minOf(danToSphereOuter(
                        point,
                        Vec3D(o.x, o.y, (arena.depth / 2) + arena.goal_side_radius),
                        arena.goal_side_radius))
            }
        }
    }

    private fun goalInsideTopCorners(point: Vec3D, dan: Collision, arena: Arena) {
        // Goal inside top corners
        if (point.z > arena.depth / 2 + arena.goal_side_radius
                && point.y > arena.goal_height - arena.goal_top_radius) {
            // Side x
            if (point.x > (arena.goal_width / 2) - arena.goal_top_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(
                                (arena.goal_width / 2) - arena.goal_top_radius,
                                arena.goal_height - arena.goal_top_radius,
                                point.z
                        ),
                        arena.goal_top_radius))
            // Side z
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.goal_height - arena.goal_top_radius,
                                (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius
                        ),
                        arena.goal_top_radius))
        }
    }

    private fun bottomCorners(point: Vec3D, dan: Collision, arena: Arena) {
        // Bottom corners
        if (point.y < arena.bottom_radius) {
            // Side x
            if (point.x > arena.width / 2 - arena.bottom_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(
                                arena.width / 2 - arena.bottom_radius,
                                arena.bottom_radius,
                                point.z
                        ),
                        arena.bottom_radius))
            // Side z
            if (point.z > (arena.depth / 2) - arena.bottom_radius
                    && point.x >= (arena.goal_width / 2) + arena.goal_side_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(
                                point.x,
                                arena.bottom_radius,
                                arena.depth / 2 - arena.bottom_radius
                        ),
                        arena.bottom_radius))
            // Side z (goal)
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.bottom_radius)
                dan.minOf(danToSphereInner(
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
                    (arena.depth / 2) + arena.goal_side_radius
            )
            val v = Vec3D(point.x, point.z) - o
            if (v.x < 0 && v.y < 0
                    && v.length < arena.goal_side_radius + arena.bottom_radius) {
                o += v.normalized * (arena.goal_side_radius + arena.bottom_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(o.x, arena.bottom_radius, o.y),
                        arena.bottom_radius))
            }
            // Side x (goal)
            if (point.z >= (arena.depth / 2) + arena.goal_side_radius
                    && point.x > (arena.goal_width / 2) - arena.bottom_radius)
                dan.minOf(danToSphereInner(
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
                        (arena.depth / 2) - arena.corner_radius
                )
                var n = Vec3D(point.x, point.z) - cornerO
                val dist = n.length
                if (dist > arena.corner_radius - arena.bottom_radius) {
                    n /= dist
                    val o2 = cornerO + n * (arena.corner_radius - arena.bottom_radius)
                    dan.minOf(danToSphereInner(
                            point,
                            Vec3D(o2.x, arena.bottom_radius, o2.y),
                            arena.bottom_radius))
                }
            }
        }
    }

    private fun ceilingCorners(point: Vec3D, dan: Collision, arena: Arena) {
        // Ceiling corners
        if (point.y > arena.height - arena.top_radius) {
            // Side x
            if (point.x > (arena.width / 2) - arena.top_radius)
                dan.minOf(danToSphereInner(
                        point,
                        Vec3D(
                                (arena.width / 2) - arena.top_radius,
                                arena.height - arena.top_radius,
                                point.z
                        ),
                        arena.top_radius))
            // Side z
            if (point.z > (arena.depth / 2) - arena.top_radius)
                dan.minOf(danToSphereInner(
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
                        (arena.depth / 2) - arena.corner_radius
                )
                val dv = Vec3D(point.x, point.z) - cornerO
                if (dv.length > arena.corner_radius - arena.top_radius) {
                    val n = dv.normalized
                    val o2 = cornerO + n * (arena.corner_radius - arena.top_radius)
                    dan.minOf(danToSphereInner(
                            point,
                            Vec3D(o2.x, arena.height - arena.top_radius, o2.y),
                            arena.top_radius))
                }
            }
        }
    }
}


fun clamp(it: Double, mn: Double, mx: Double) =
        max(mn, min(mx, it))
