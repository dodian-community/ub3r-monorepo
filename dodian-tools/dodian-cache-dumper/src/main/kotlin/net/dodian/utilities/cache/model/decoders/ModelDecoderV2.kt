package net.dodian.utilities.cache.model.decoders

import net.dodian.utilities.cache.extensions.readShortSmart
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.model.Model
import kotlin.experimental.and

private var headerData = byteArrayOf()
private var headerVertices = -1
private var headerFaces = -1
private var headerTextureFaces = -1

fun modelDecoderV2(model: Model, data: ByteBuf): Model = with(model) {
    val first = data.duplicate()
    val second = data.duplicate()
    val third = data.duplicate()
    val fourth = data.duplicate()
    val fifth = data.duplicate()
    val sixth = data.duplicate()
    val seventh = data.duplicate()

    first.readerIndex(data.array().size - 23)
    vertexCount = first.readUnsignedShort()
    triangleCount = first.readUnsignedShort()
    texturedFaces = first.readUnsignedByte().toInt()

    headerData = data.array()
    headerVertices = vertexCount
    headerFaces = triangleCount
    headerTextureFaces = texturedFaces

    val flag = first.readUnsignedByte().toInt()
    val hasFaceTypes = (flag and 0x1) == 1
    val hasParticleEffects = (flag and 0x2) == 2
    val hasBillboards = (flag and 0x4) == 4
    val hasVersion = (flag and 0x8) == 8

    if (hasVersion) {
        first.readerIndex(first.readerIndex() - 7)
        version = first.readUnsignedByte().toInt()
        first.readerIndex(first.readerIndex() + 6)
    }

    val modelPriorityOpcode = first.readUnsignedByte().toInt()
    val modelAlphaOpcode = first.readUnsignedByte().toInt()
    val modelMuscleOpcode = first.readUnsignedByte().toInt()
    val modelTextureOpcode = first.readUnsignedByte().toInt()
    val modelBonesOpcode = first.readUnsignedByte().toInt()

    val modelVertexX = first.readUnsignedShort()
    val modelVertexY = first.readUnsignedShort()
    val modelVertexZ = first.readUnsignedShort()
    val modelVertexPoints = first.readUnsignedShort()
    val modelTextureIndices = first.readUnsignedShort()

    var textureIdSimple = 0
    var textureIdComplex = 0
    var textureIdCube = 0

    if (texturedFaces > 0) {
        textureMap = ShortArray(texturedFaces)
        first.readerIndex(0)
        for (face in 0 until texturedFaces) {
            val opcode = first.readByte().toShort().also { textureMap[face] = it }
            textureMap[face] = opcode

            if (opcode == 0.toShort()) textureIdSimple++
            if (opcode in 1..3) textureIdComplex++
            if (opcode == 2.toShort()) textureIdCube++
        }
    }

    var readPosition = texturedFaces

    val modelVertexOffset = readPosition
    readPosition += vertexCount

    val modelRenderTypeOffset = readPosition
    if (flag == 1) readPosition += triangleCount

    val modelFaceOffset = readPosition
    readPosition += triangleCount

    val modelFacePrioritiesOffset = readPosition
    if (modelPriorityOpcode == 255) readPosition += triangleCount

    val modelMuscleOffset = readPosition
    if (modelMuscleOpcode == 1) readPosition += triangleCount

    val modelBonesOffset = readPosition
    if (modelBonesOpcode == 1) readPosition += vertexCount

    val modelAlphaOffset = readPosition
    if (modelAlphaOpcode == 1) readPosition += triangleCount

    val modelPointsOffset = readPosition
    readPosition += modelVertexPoints

    val modelTextureId = readPosition
    if (modelTextureOpcode == 1) readPosition += triangleCount * 2

    val modelTextureCoordinateOffset = readPosition
    readPosition += modelTextureIndices

    val modelColorOffset = readPosition
    readPosition += triangleCount * 2

    val modelVertexXOffset = readPosition
    readPosition += modelVertexX

    val modelVertexYOffset = readPosition
    readPosition += modelVertexY

    val modelVertexZOffset = readPosition
    readPosition += modelVertexZ

    val modelSimpleTextureOffset = readPosition
    readPosition += textureIdSimple * 6

    val modelComplexTextureOffset = readPosition
    readPosition += textureIdComplex * 6

    val modelTextureScaleOffset = readPosition
    readPosition += textureIdComplex * 6

    val modelTextureRotationOffset = readPosition
    readPosition += textureIdComplex * 2

    val modelTextureDirectionOffset = readPosition
    readPosition += textureIdComplex

    val modelTextureTranslateOffset = readPosition
    readPosition += textureIdComplex * 2 + textureIdCube * 2

    particleVertices = IntArray(vertexCount)
    verticesXCoordinate = IntArray(vertexCount)
    verticesYCoordinate = IntArray(vertexCount)
    verticesZCoordinate = IntArray(vertexCount)
    faceIndicesA = IntArray(triangleCount)
    faceIndicesB = IntArray(triangleCount)
    faceIndicesC = IntArray(triangleCount)

    if (modelBonesOpcode == 1)
        vertexWeights = IntArray(vertexCount)

    if (flag == 1)
        triangleInfo = IntArray(triangleCount)

    if (modelPriorityOpcode == 255)
        trianglePriorities = ByteArray(triangleCount)
    else modelPriority = modelPriorityOpcode.toByte()

    if (modelAlphaOpcode == 1)
        triangleAlpha = IntArray(triangleCount)

    if (modelMuscleOpcode == 1)
        triangleSkin = IntArray(triangleCount)

    if (modelTextureOpcode == 1)
        faceMaterial = ShortArray(triangleCount)

    if (modelTextureOpcode == 1 && texturedFaces > 0)
        faceTexture = ShortArray(triangleCount)

    triangleColors = ShortArray(triangleCount)
    if (texturedFaces > 0) {
        textureVertexA = ShortArray(texturedFaces)
        textureVertexB = ShortArray(texturedFaces)
        textureVertexC = ShortArray(texturedFaces)
    }

    first.readerIndex(modelVertexOffset)
    second.readerIndex(modelVertexXOffset)
    third.readerIndex(modelVertexYOffset)
    fourth.readerIndex(modelVertexZOffset)
    fifth.readerIndex(modelBonesOffset)

    var startX = 0
    var startY = 0
    var startZ = 0

    for (point in 0 until vertexCount) {
        val positionMask = first.readUnsignedByte()

        var x = 0
        if ((positionMask and 1) != 0.toShort()) x = second.readShortSmart().toInt()

        var y = 0
        if ((positionMask and 2) != 0.toShort()) y = third.readShortSmart().toInt()

        var z = 0
        if ((positionMask and 4) != 0.toShort()) z = fourth.readShortSmart().toInt()

        verticesXCoordinate[point] = startX + x
        verticesYCoordinate[point] = startY + y
        verticesZCoordinate[point] = startZ + z

        startX = verticesXCoordinate[point]
        startY = verticesYCoordinate[point]
        startZ = verticesZCoordinate[point]

        if (vertexWeights.isNotEmpty()) vertexWeights[point] = fifth.readUnsignedByte().toInt()
    }

    first.readerIndex(modelColorOffset)
    second.readerIndex(modelRenderTypeOffset)
    third.readerIndex(modelFacePrioritiesOffset)
    fourth.readerIndex(modelAlphaOffset)
    fifth.readerIndex(modelMuscleOffset)
    sixth.readerIndex(modelTextureId)
    seventh.readerIndex(modelTextureCoordinateOffset)

    for (face in 0 until triangleCount) {
        triangleColors[face] = (first.readUnsignedShort() and 0xFFFF).toShort()

        if (flag == 1) triangleInfo[face] = second.readByte().toInt()

        if (modelPriorityOpcode == 255) trianglePriorities[face] = third.readByte()

        if (modelAlphaOpcode == 1) {
            triangleAlpha[face] = fourth.readByte().toInt()
            if (triangleAlpha[face] < 0)
                triangleAlpha[face] = (256 + triangleAlpha[face])
        }

        if (modelMuscleOpcode == 1)
            triangleSkin[face] = fifth.readUnsignedByte().toInt()

        if (modelTextureOpcode == 1) {
            faceMaterial[face] = (sixth.readUnsignedShort() - 1).toShort()
            if (faceMaterial[face] >= 0) {
                if (triangleInfo.isNotEmpty()) {
                    if (triangleInfo[face] < 2
                        && triangleColors[face] != 127.toShort()
                        && triangleColors[face] != (-27075).toShort()
                        && triangleColors[face] != 8128.toShort()
                        && triangleColors[face] != 7510.toShort()
                    ) {
                        faceMaterial[face] = -1
                    }
                }
            }

            if (faceMaterial[face] != (-1).toShort() && faceMaterial[face] >= 0 && faceMaterial[face] <= 85)
                triangleColors[face] = 127
        }

        if (faceTexture.isNotEmpty() && faceMaterial[face] != (-1).toShort())
            faceTexture[face] = (seventh.readUnsignedByte().toInt() - 1).toShort()
    }

    first.readerIndex(modelPointsOffset)
    second.readerIndex(modelFaceOffset)

    var a = 0
    var b = 0
    var c = 0
    var lastCoordinate = 0
    for (face in 0 until triangleCount) {
        val opcode = second.readUnsignedByte().toInt()
        if (opcode == 1) {
            a = first.readShortSmart().toInt() + lastCoordinate
            lastCoordinate = a
            b = first.readShortSmart().toInt() + lastCoordinate
            lastCoordinate = b
            c = first.readShortSmart().toInt() + lastCoordinate
            lastCoordinate = c

            faceIndicesA[face] = a
            faceIndicesB[face] = b
            faceIndicesC[face] = c
        }

        if (opcode == 2) {
            b = c
            c = first.readShortSmart() + lastCoordinate
            lastCoordinate = c
            faceIndicesA[face] = a
            faceIndicesB[face] = b
            faceIndicesC[face] = c
        }

        if (opcode == 3) {
            a = c
            c = first.readShortSmart() + lastCoordinate
            lastCoordinate = c
            faceIndicesA[face] = a
            faceIndicesB[face] = b
            faceIndicesC[face] = c
        }

        if (opcode == 4) {
            val l14 = a
            a = b
            b = l14
            c = first.readShortSmart() + lastCoordinate
            lastCoordinate = c
            faceIndicesA[face] = a
            faceIndicesB[face] = b
            faceIndicesC[face] = c
        }
    }

    first.readerIndex(modelSimpleTextureOffset)
    second.readerIndex(modelComplexTextureOffset)
    third.readerIndex(modelTextureScaleOffset)
    fourth.readerIndex(modelTextureRotationOffset)
    fifth.readerIndex(modelTextureDirectionOffset)
    sixth.readerIndex(modelTextureTranslateOffset)

    for (face in 0 until texturedFaces) {
        val opcode = (textureMap[face] and 0xff).toInt()

        if (opcode == 0) {
            textureVertexA[face] = first.readUnsignedShort().toShort()
            textureVertexB[face] = first.readUnsignedShort().toShort()
            textureVertexC[face] = first.readUnsignedShort().toShort()
        }

        if (opcode in 1..3) {
            textureVertexA[face] = second.readUnsignedShort().toShort()
            textureVertexB[face] = second.readUnsignedShort().toShort()
            textureVertexC[face] = second.readUnsignedShort().toShort()
        }
    }

    convertTexturesToOldFormat()

    return this
}

fun Model.convertTexturesToOldFormat() {
    if (faceMaterial.isEmpty() || faceTexture.isEmpty()) return
    if (faceMaterial.any { it > 117 }) return

    if (triangleInfo.isEmpty()) triangleInfo = IntArray(triangleCount)

    for (i in 0 until triangleCount) {
        if (faceMaterial[i] != (-1).toShort() && faceTexture[i] >= 0) {
            val mask = 2 + (faceTexture[i] * 4)
            triangleInfo[i] = mask
            triangleColors[i] = faceMaterial[i]
        } else {
            triangleInfo[i] = 0
        }
    }
}