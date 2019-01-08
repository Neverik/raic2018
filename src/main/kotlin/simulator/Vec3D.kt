package simulator

import kotlin.math.pow
import kotlin.math.sqrt


@Suppress("NOTHING_TO_INLINE")
data class Vec3D(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    inline operator fun plus(other: Vec3D) =
            Vec3D(x + other.x, y + other.y, z + other.z)

    inline operator fun minus(other: Vec3D) =
            Vec3D(x - other.x, y - other.y, z - other.z)

    inline operator fun times(other: Double) =
            Vec3D(x * other, y * other, z * other)

    inline operator fun div(other: Double) =
            Vec3D(x / other, y / other, z / other)

    inline infix fun dot(other: Vec3D) =
            x * other.x + y * other.y + z * other.z

    inline val length: Double
        get() = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

    inline val normalized: Vec3D
        get() = this / length

    inline fun clamp(max: Double): Vec3D {
        val len = length
        return if (len > max)
            Vec3D(x / len * max, y / len * max, z / len * max)
        else
            this
    }
}