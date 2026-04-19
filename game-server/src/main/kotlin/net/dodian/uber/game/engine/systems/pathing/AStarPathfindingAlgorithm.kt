package net.dodian.uber.game.engine.systems.pathing

import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

class AStarPathfindingAlgorithm(
    private val collision: PathingCollision,
    private val heuristic: Heuristic = Heuristic.MANHATTAN,
) : PathfindingAlgorithm {
    override fun find(srcX: Int, srcY: Int, dstX: Int, dstY: Int, z: Int): ArrayDeque<Node> {
        if (srcX == dstX && srcY == dstY) {
            return ArrayDeque()
        }

        val minX = min(srcX, dstX) - SEARCH_MARGIN
        val maxX = max(srcX, dstX) + SEARCH_MARGIN
        val minY = min(srcY, dstY) - SEARCH_MARGIN
        val maxY = max(srcY, dstY) + SEARCH_MARGIN
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val total = width * height

        val workspace = WORKSPACE.get()
        workspace.prepare(total)
        val searchId = workspace.nextSearchId()

        val srcIndex = indexOf(srcX, srcY, minX, minY, width)
        val dstIndex = indexOf(dstX, dstY, minX, minY, width)
        workspace.gCost[srcIndex] = 0
        workspace.hCost[srcIndex] = heuristic.estimate(srcX, srcY, dstX, dstY)
        workspace.parent[srcIndex] = NO_PARENT
        workspace.visitedStamp[srcIndex] = searchId
        workspace.pushOpen(srcIndex, searchId)
        var expansions = 0

        while (workspace.openSize > 0) {
            if (expansions++ >= MAX_EXPANSIONS) {
                return ArrayDeque()
            }

            val currentIndex = workspace.popOpen(searchId)
            if (workspace.closedStamp[currentIndex] == searchId) {
                continue
            }
            workspace.closedStamp[currentIndex] = searchId
            if (currentIndex == dstIndex) {
                return buildPath(
                    endIndex = currentIndex,
                    srcIndex = srcIndex,
                    minX = minX,
                    minY = minY,
                    width = width,
                    z = z,
                    workspace = workspace,
                )
            }
            val currentX = minX + (currentIndex % width)
            val currentY = minY + (currentIndex / width)
            val currentCost = workspace.gCost[currentIndex]

            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) {
                        continue
                    }

                    val nextX = currentX + dx
                    val nextY = currentY + dy
                    if (nextX < minX || nextX > maxX || nextY < minY || nextY > maxY) {
                        continue
                    }
                    if (!collision.traversable(nextX, nextY, z, dx, dy)) {
                        continue
                    }

                    val nextCost = currentCost + if (dx == 0 || dy == 0) 10 else 14
                    val nextIndex = indexOf(nextX, nextY, minX, minY, width)
                    if (workspace.closedStamp[nextIndex] == searchId) {
                        continue
                    }

                    val isVisited = workspace.visitedStamp[nextIndex] == searchId
                    if (isVisited && nextCost >= workspace.gCost[nextIndex]) {
                        continue
                    }

                    workspace.gCost[nextIndex] = nextCost
                    workspace.hCost[nextIndex] = heuristic.estimate(nextX, nextY, dstX, dstY)
                    workspace.parent[nextIndex] = currentIndex
                    workspace.visitedStamp[nextIndex] = searchId
                    if (workspace.openStamp[nextIndex] == searchId) {
                        workspace.siftUp(workspace.heapPos[nextIndex], searchId)
                    } else {
                        workspace.pushOpen(nextIndex, searchId)
                    }
                }
            }
        }

        return ArrayDeque()
    }

    private fun buildPath(
        endIndex: Int,
        srcIndex: Int,
        minX: Int,
        minY: Int,
        width: Int,
        z: Int,
        workspace: Workspace,
    ): ArrayDeque<Node> {
        val indexPath = IntArray(MAX_EXPANSIONS)
        var count = 0
        var cursor = endIndex
        while (cursor != srcIndex && cursor != NO_PARENT && count < indexPath.size) {
            indexPath[count++] = cursor
            cursor = workspace.parent[cursor]
        }
        if (cursor != srcIndex) {
            return ArrayDeque()
        }

        val path = ArrayDeque<Node>(count)
        var parentNode: Node? = null
        for (i in (count - 1) downTo 0) {
            val index = indexPath[i]
            val x = minX + (index % width)
            val y = minY + (index / width)
            val node = Node(x, y, z, workspace.gCost[index], workspace.hCost[index], parentNode)
            path.add(node)
            parentNode = node
        }
        return path
    }

    private fun indexOf(x: Int, y: Int, minX: Int, minY: Int, width: Int): Int {
        return (y - minY) * width + (x - minX)
    }

    private class Workspace {
        var gCost = IntArray(0)
        var hCost = IntArray(0)
        var parent = IntArray(0)
        var visitedStamp = IntArray(0)
        var closedStamp = IntArray(0)
        var openStamp = IntArray(0)
        var heapPos = IntArray(0)
        var openHeap = IntArray(0)
        var tieBreak = IntArray(0)
        var openSize: Int = 0
        private var tieCounter = 0
        private var searchId = 0

        fun prepare(size: Int) {
            if (gCost.size >= size) {
                openSize = 0
                tieCounter = 0
                return
            }
            gCost = IntArray(size)
            hCost = IntArray(size)
            parent = IntArray(size) { NO_PARENT }
            visitedStamp = IntArray(size)
            closedStamp = IntArray(size)
            openStamp = IntArray(size)
            heapPos = IntArray(size) { -1 }
            openHeap = IntArray(size)
            tieBreak = IntArray(size)
            openSize = 0
            tieCounter = 0
        }

        fun nextSearchId(): Int {
            searchId++
            if (searchId == Int.MAX_VALUE) {
                visitedStamp.fill(0)
                closedStamp.fill(0)
                openStamp.fill(0)
                searchId = 1
            }
            return searchId
        }

        fun pushOpen(index: Int, stamp: Int) {
            openStamp[index] = stamp
            tieBreak[index] = ++tieCounter
            openHeap[openSize] = index
            heapPos[index] = openSize
            siftUp(openSize, stamp)
            openSize++
        }

        fun popOpen(stamp: Int): Int {
            val root = openHeap[0]
            openSize--
            if (openSize > 0) {
                val moved = openHeap[openSize]
                openHeap[0] = moved
                heapPos[moved] = 0
                siftDown(0, stamp)
            }
            heapPos[root] = -1
            openStamp[root] = 0
            return root
        }

        fun siftUp(start: Int, stamp: Int) {
            var index = start
            while (index > 0) {
                val parentIndex = (index - 1) ushr 1
                val childCell = openHeap[index]
                val parentCell = openHeap[parentIndex]
                if (!isHigherPriority(childCell, parentCell, stamp)) {
                    break
                }
                swap(index, parentIndex)
                index = parentIndex
            }
        }

        private fun siftDown(start: Int, stamp: Int) {
            var index = start
            while (true) {
                val left = (index shl 1) + 1
                if (left >= openSize) {
                    break
                }
                val right = left + 1
                var best = left
                if (right < openSize && isHigherPriority(openHeap[right], openHeap[left], stamp)) {
                    best = right
                }
                if (!isHigherPriority(openHeap[best], openHeap[index], stamp)) {
                    break
                }
                swap(index, best)
                index = best
            }
        }

        private fun swap(a: Int, b: Int) {
            val cellA = openHeap[a]
            val cellB = openHeap[b]
            openHeap[a] = cellB
            openHeap[b] = cellA
            heapPos[cellA] = b
            heapPos[cellB] = a
        }

        private fun isHigherPriority(left: Int, right: Int, stamp: Int): Boolean {
            val leftF = gCost[left] + hCost[left]
            val rightF = gCost[right] + hCost[right]
            if (leftF != rightF) {
                return leftF < rightF
            }
            val leftH = hCost[left]
            val rightH = hCost[right]
            if (leftH != rightH) {
                return leftH < rightH
            }
            val leftOpen = openStamp[left] == stamp
            val rightOpen = openStamp[right] == stamp
            if (leftOpen != rightOpen) {
                return leftOpen
            }
            return tieBreak[left] < tieBreak[right]
        }
    }

    private companion object {
        const val SEARCH_MARGIN = 24
        const val MAX_EXPANSIONS = 8192
        const val NO_PARENT = -1
        private val WORKSPACE: ThreadLocal<Workspace> = ThreadLocal.withInitial { Workspace() }
    }
}
