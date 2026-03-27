package net.dodian.utilities

class ISAACCipher(
    ai: IntArray,
) {
    @JvmField var keyArrayIdx: Int = 0
    @JvmField var keySetArray: IntArray = IntArray(256)
    @JvmField var cryptArray: IntArray = IntArray(256)
    @JvmField var cryptVar1: Int = 0
    @JvmField var cryptVar2: Int = 0
    @JvmField var cryptVar3: Int = 0

    init {
        System.arraycopy(ai, 0, keySetArray, 0, ai.size)
        initializeKeySet()
    }

    fun getNextKey(): Int {
        if (keyArrayIdx-- == 0) {
            generateNextKeySet()
            keyArrayIdx = 255
        }
        return keySetArray[keyArrayIdx]
    }

    fun generateNextKeySet() {
        cryptVar2 += ++cryptVar3
        for (i in 0 until 256) {
            val j = cryptArray[i]
            when (i and 3) {
                0 -> cryptVar1 = cryptVar1 xor (cryptVar1 shl 13)
                1 -> cryptVar1 = cryptVar1 xor (cryptVar1 ushr 6)
                2 -> cryptVar1 = cryptVar1 xor (cryptVar1 shl 2)
                3 -> cryptVar1 = cryptVar1 xor (cryptVar1 ushr 16)
            }
            cryptVar1 += cryptArray[i + 128 and 0xFF]
            val k = cryptArray[(j and 0x3FC) shr 2] + cryptVar1 + cryptVar2
            cryptArray[i] = k
            cryptVar2 = cryptArray[(k shr 8 and 0x3FC) shr 2] + j
            keySetArray[i] = cryptVar2
        }
    }

    fun initializeKeySet() {
        var l = 0x9e3779b9.toInt()
        var i1 = 0x9e3779b9.toInt()
        var j1 = 0x9e3779b9.toInt()
        var k1 = 0x9e3779b9.toInt()
        var l1 = 0x9e3779b9.toInt()
        var i2 = 0x9e3779b9.toInt()
        var j2 = 0x9e3779b9.toInt()
        var k2 = 0x9e3779b9.toInt()

        for (i in 0 until 4) {
            l = l xor (i1 shl 11)
            k1 += l
            i1 += j1
            i1 = i1 xor (j1 ushr 2)
            l1 += i1
            j1 += k1
            j1 = j1 xor (k1 shl 8)
            i2 += j1
            k1 += l1
            k1 = k1 xor (l1 ushr 16)
            j2 += k1
            l1 += i2
            l1 = l1 xor (i2 shl 10)
            k2 += l1
            i2 += j2
            i2 = i2 xor (j2 ushr 4)
            l += i2
            j2 += k2
            j2 = j2 xor (k2 shl 8)
            i1 += j2
            k2 += l
            k2 = k2 xor (l ushr 9)
            j1 += k2
            l += i1
        }

        var j = 0
        while (j < 256) {
            l += keySetArray[j]
            i1 += keySetArray[j + 1]
            j1 += keySetArray[j + 2]
            k1 += keySetArray[j + 3]
            l1 += keySetArray[j + 4]
            i2 += keySetArray[j + 5]
            j2 += keySetArray[j + 6]
            k2 += keySetArray[j + 7]
            l = l xor (i1 shl 11)
            k1 += l
            i1 += j1
            i1 = i1 xor (j1 ushr 2)
            l1 += i1
            j1 += k1
            j1 = j1 xor (k1 shl 8)
            i2 += j1
            k1 += l1
            k1 = k1 xor (l1 ushr 16)
            j2 += k1
            l1 += i2
            l1 = l1 xor (i2 shl 10)
            k2 += l1
            i2 += j2
            i2 = i2 xor (j2 ushr 4)
            l += i2
            j2 += k2
            j2 = j2 xor (k2 shl 8)
            i1 += j2
            k2 += l
            k2 = k2 xor (l ushr 9)
            j1 += k2
            l += i1
            cryptArray[j] = l
            cryptArray[j + 1] = i1
            cryptArray[j + 2] = j1
            cryptArray[j + 3] = k1
            cryptArray[j + 4] = l1
            cryptArray[j + 5] = i2
            cryptArray[j + 6] = j2
            cryptArray[j + 7] = k2
            j += 8
        }

        var k = 0
        while (k < 256) {
            l += cryptArray[k]
            i1 += cryptArray[k + 1]
            j1 += cryptArray[k + 2]
            k1 += cryptArray[k + 3]
            l1 += cryptArray[k + 4]
            i2 += cryptArray[k + 5]
            j2 += cryptArray[k + 6]
            k2 += cryptArray[k + 7]
            l = l xor (i1 shl 11)
            k1 += l
            i1 += j1
            i1 = i1 xor (j1 ushr 2)
            l1 += i1
            j1 += k1
            j1 = j1 xor (k1 shl 8)
            i2 += j1
            k1 += l1
            k1 = k1 xor (l1 ushr 16)
            j2 += k1
            l1 += i2
            l1 = l1 xor (i2 shl 10)
            k2 += l1
            i2 += j2
            i2 = i2 xor (j2 ushr 4)
            l += i2
            j2 += k2
            j2 = j2 xor (k2 shl 8)
            i1 += j2
            k2 += l
            k2 = k2 xor (l ushr 9)
            j1 += k2
            l += i1
            cryptArray[k] = l
            cryptArray[k + 1] = i1
            cryptArray[k + 2] = j1
            cryptArray[k + 3] = k1
            cryptArray[k + 4] = l1
            cryptArray[k + 5] = i2
            cryptArray[k + 6] = j2
            cryptArray[k + 7] = k2
            k += 8
        }

        generateNextKeySet()
        keyArrayIdx = 256
    }
}
