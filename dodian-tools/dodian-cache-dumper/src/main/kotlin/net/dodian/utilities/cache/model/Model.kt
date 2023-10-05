@file:Suppress("ArrayInDataClass")

package net.dodian.utilities.cache.model

/**
 * Model to house decoded data from a 3D model in the RuneScape cache.
 */
data class Model(
    var modelId: Int = -1,
    var version: Int = -1,
    var textureUCoordinates: Array<FloatArray> = arrayOf(),
    var textureVCoordinates: Array<FloatArray> = arrayOf(),
    var faceMaterial: ShortArray = shortArrayOf(),
    var faceTexture: ShortArray = shortArrayOf(),
    var textureMap: ShortArray = shortArrayOf(),
    var faceTextureMasks: ByteArray = byteArrayOf(),
    var vertexCount: Int = 0,
    var triangleCount: Int = 0,
    var verticesXCoordinate: IntArray = intArrayOf(),
    var verticesYCoordinate: IntArray = intArrayOf(),
    var verticesZCoordinate: IntArray = intArrayOf(),
    var faceIndicesA: IntArray = intArrayOf(),
    var faceIndicesB: IntArray = intArrayOf(),
    var faceIndicesC: IntArray = intArrayOf(),
    var triangleInfo: IntArray = intArrayOf(),
    var trianglePriorities: ByteArray = byteArrayOf(),
    var triangleAlpha: IntArray = intArrayOf(),
    var triangleColors: ShortArray = shortArrayOf(),
    var modelPriority: Byte = 0,
    var texturedFaces: Int = 0,
    var textureVertexA: ShortArray = shortArrayOf(),
    var textureVertexB: ShortArray = shortArrayOf(),
    var textureVertexC: ShortArray = shortArrayOf(),
    var vertexLabels: IntArray = intArrayOf(),
    var triangleLabels: IntArray = intArrayOf(),
    var particleVertices: IntArray = intArrayOf(),
    var vertexWeights: IntArray = intArrayOf(),
    var triangleSkin: IntArray = intArrayOf()
) {

    fun verticesToFloatArray(): FloatArray {
        val vertices = FloatArray(vertexCount * 3)

        for (vertex in 0 until vertexCount) {
            vertices[vertex * 3 + 0] = verticesXCoordinate[vertex].toFloat()
            vertices[vertex * 3 + 1] = verticesYCoordinate[vertex] * -1f
            vertices[vertex * 3 + 2] = verticesZCoordinate[vertex] * -1f
        }

        val scale = 15f
        for (vertex in vertices.indices) {
            vertices[vertex] /= scale
        }

        return vertices
    }

    fun indicesToIntArray(): IntArray {
        val indices = IntArray(triangleCount * 3)

        for (index in 0 until triangleCount) {
            indices[index * 3 + 0] = faceIndicesA[index]
            indices[index * 3 + 1] = faceIndicesB[index]
            indices[index * 3 + 2] = faceIndicesC[index]
        }

        return indices
    }

    fun texCoordsToFloatArray(): FloatArray {
        computeTextureUVCoordinates()

        val texCoords = FloatArray(triangleCount * 6)
        for (triangle in 0 until triangleCount) {
            val u = textureUCoordinates[triangle]
            val v = textureVCoordinates[triangle]
            texCoords[triangle * 6 + 0] = u[2]
            texCoords[triangle * 6 + 1] = v[2]
            texCoords[triangle * 6 + 2] = u[1]
            texCoords[triangle * 6 + 3] = v[1]
            texCoords[triangle * 6 + 4] = u[0]
            texCoords[triangle * 6 + 5] = v[0]
        }

        return texCoords
    }

    fun computeTextureUVCoordinates() {
        textureUCoordinates = Array(triangleCount) { FloatArray(3) }
        textureVCoordinates = Array(triangleCount) { FloatArray(3) }

        for (i in 0 until triangleCount) {

            var textureCoordinate = when (faceTexture.isEmpty()) {
                true -> -1
                false -> faceTexture[i].toInt()
            }

            val textureIdx = when (faceMaterial.isEmpty()) {
                true -> -1
                else -> faceMaterial[i].toInt() and 0xFFFF
            }

            if (textureIdx != -1) {
                val u = FloatArray(3)
                val v = FloatArray(3)

                if (textureCoordinate == -1) {
                    u[0] = 0.0f
                    v[0] = 1.0f

                    u[1] = 1.0f
                    v[1] = 1.0f

                    u[2] = 0.0f
                    v[2] = 0.0f
                } else {
                    textureCoordinate = textureCoordinate and 0xFF
                    val textureRenderType = if (textureMap.isEmpty()) 0 else textureMap[textureCoordinate]

                    @Suppress("DuplicatedCode")
                    if (textureRenderType == 0.toShort()) {
                        val faceVertexIdx1 = faceIndicesA[i]
                        val faceVertexIdx2 = faceIndicesB[i]
                        val faceVertexIdx3 = faceIndicesC[i]

                        val texturedFaceVertexIdx1 = textureVertexA[textureCoordinate].toInt()
                        val texturedFaceVertexIdx2 = textureVertexB[textureCoordinate].toInt()
                        val texturedFaceVertexIdx3 = textureVertexC[textureCoordinate].toInt()

                        val triangleX = verticesXCoordinate[texturedFaceVertexIdx1].toFloat()
                        val triangleY = verticesYCoordinate[texturedFaceVertexIdx2].toFloat()
                        val triangleZ = verticesZCoordinate[texturedFaceVertexIdx3].toFloat()

                        val tx2Tx1 = verticesXCoordinate[texturedFaceVertexIdx2].toFloat() - triangleX
                        val ty2Ty1 = verticesYCoordinate[texturedFaceVertexIdx2].toFloat() - triangleY
                        val tz2Tz1 = verticesZCoordinate[texturedFaceVertexIdx2].toFloat() - triangleZ

                        val tx3Tx1 = verticesXCoordinate[texturedFaceVertexIdx3].toFloat() - triangleX
                        val ty3Ty1 = verticesYCoordinate[texturedFaceVertexIdx3].toFloat() - triangleY
                        val tz3Tz1 = verticesZCoordinate[texturedFaceVertexIdx3].toFloat() - triangleZ

                        val vxT1 = verticesXCoordinate[faceVertexIdx1].toFloat() - triangleX
                        val vyT1 = verticesYCoordinate[faceVertexIdx1].toFloat() - triangleY
                        val vzT1 = verticesZCoordinate[faceVertexIdx1].toFloat() - triangleZ

                        val vx2Tx1 = verticesXCoordinate[faceVertexIdx2].toFloat() - triangleX
                        val vy2Ty1 = verticesYCoordinate[faceVertexIdx2].toFloat() - triangleY
                        val vz2Tz1 = verticesZCoordinate[faceVertexIdx2].toFloat() - triangleZ

                        val vx3Tx1 = verticesXCoordinate[faceVertexIdx3].toFloat() - triangleX
                        val vy3Ty1 = verticesYCoordinate[faceVertexIdx3].toFloat() - triangleY
                        val vz3Tz1 = verticesZCoordinate[faceVertexIdx3].toFloat() - triangleZ

                        val f897 = (ty2Ty1 * tz3Tz1 - tz2Tz1 * ty3Ty1)
                        val f898 = (tz2Tz1 * tx3Tx1 - tx2Tx1 * tz3Tz1)
                        val f899 = (tx2Tx1 * ty3Ty1 - ty2Ty1 * tx3Tx1)

                        var f900 = (ty3Ty1 * f899 - tz3Tz1 * f898)
                        var f901 = (tz3Tz1 * f897 - tx3Tx1 * f899)
                        var f902 = (tx3Tx1 * f898 - ty3Ty1 * f897)
                        var f903 = 1.0f / (f900 * tx2Tx1 + f901 * ty2Ty1 + f902 * tz2Tz1)

                        u[0] = (f900 * vxT1 + f901 * vyT1 + f902 * vzT1) * f903
                        u[1] = (f900 * vx2Tx1 + f901 * vy2Ty1 + f902 * vz2Tz1) * f903
                        u[2] = (f900 * vx3Tx1 + f901 * vy3Ty1 + f902 * vz3Tz1) * f903

                        f900 = ty2Ty1 * f899 - tz2Tz1 * f898
                        f901 = tz2Tz1 * f897 - tx2Tx1 * f899
                        f902 = tx2Tx1 * f898 - ty2Ty1 * f897
                        f903 = 1.0f / (f900 * tx3Tx1 + f901 * ty3Ty1 + f902 * tz3Tz1)

                        v[0] = (f900 * vxT1 + f901 * vyT1 + f902 * vzT1) * f903
                        v[1] = (f900 * vx2Tx1 + f901 * vy2Ty1 + f902 * vz2Tz1) * f903
                        v[2] = (f900 * vx3Tx1 + f901 * vy3Ty1 + f902 * vz3Tz1) * f903
                    }
                }

                textureUCoordinates[i] = u
                textureVCoordinates[i] = v
            }
        }
    }
}