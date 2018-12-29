package simulator

import kotlin.math.hypot


// Very evil, places the vector on stack
typealias Vec3D = DoubleArray


@Suppress("FunctionName")
fun Vec3D(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3D =
        doubleArrayOf(x, y, z)

inline var Vec3D.x: Double
    get() = this[0]
    set(value) {this[0] = value}

inline var Vec3D.y: Double
    get() = this[1]
    set(value) {this[1] = value}

inline var Vec3D.z: Double
    get() = this[2]
    set(value) {this[2] = value}

operator fun Vec3D.plus(other: Vec3D) =
        Vec3D(x + other.x, y + other.y, z + other.z)

operator fun Vec3D.minus(other: Vec3D) =
        Vec3D(x - other.x, y - other.y, z - other.z)

operator fun Vec3D.times(other: Double) =
        Vec3D(x * other, y * other, z * other)

operator fun Vec3D.div(other: Double) =
        Vec3D(x / other, y / other, z / other)

infix fun Vec3D.dot(other: Vec3D) =
        x * other.x + y * other.y + z * other.z

inline val Vec3D.length: Double
    get() = hypot(hypot(x, y), z)

inline val Vec3D.normalized: Vec3D
    get() = this / length

fun Vec3D.clamp(max: Double): Vec3D =
        if (length > max)
            normalized * max
        else
            this

/*
data class Vec3D(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    operator fun plus(other: Vec3D) =
            Vec3D(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3D) =
            Vec3D(x - other.x, y - other.y, z - other.z)

    operator fun times(other: Double) =
            Vec3D(x * other, y * other, z * other)

    operator fun div(other: Double) =
            Vec3D(x / other, y / other, z / other)

    operator fun rem(other: Vec3D) =
            x * other.x + y * other.y + z * other.z

    val length: Double
        get() = hypot(hypot(x, y), z)

    val normalized: Vec3D
        get() = this / length

    fun clamp(max: Double): Vec3D =
            if (length > max)
                normalized * max
            else
                this
}
*/