package org.ton.intellij.tolk.coverage

import com.intellij.openapi.util.io.systemIndependentPath
import com.intellij.util.containers.PeekableIteratorWrapper
import java.io.File
import java.io.PrintWriter

class LcovCoverageReport {
    private val info: MutableMap<String, FileReport> = hashMapOf()
    val records: Set<Map.Entry<String, FileReport>> get() = info.entries

    fun mergeFileReport(basePath: String?, filePath: String, report: FileReport) {
        val file = File(filePath).let {
            if (it.isAbsolute || basePath == null) it else File(basePath, filePath)
        }
        val normalizedFilePath = file.systemIndependentPath
        val oldReport = info[normalizedFilePath]
        val result = if (oldReport == null) {
            FileReport(
                normalizeLineHitsList(report.lineHits),
                report.branchHits.toMutableMap(),
            )
        } else {
            FileReport(
                doMerge(oldReport.lineHits, report.lineHits),
                mergeBranchHits(oldReport.branchHits, report.branchHits),
            )
        }
        info[normalizedFilePath] = result
    }

    class FileReport(val lineHits: List<LineHits>, val branchHits: Map<Int, List<BranchHits>>)

    class LineHits(val lineNumber: Int, hits: Int) {
        var hits: Int
            private set

        init {
            this.hits = hits
        }

        fun addHits(hitCount: Int) {
            when {
                hits == -1 -> return
                hitCount == -1 -> hits = hitCount
                else -> hits += hitCount
            }
        }
    }

    class BranchHits(val blockIndex: Int, val trueHits: Int, val falseHits: Int)

    object Serialization {
        private const val SOURCE_FILE_PREFIX: String = "SF:"
        private const val LINE_HIT_PREFIX: String = "DA:"
        private const val BRANCH_DATA_PREFIX: String = "BRDA:"
        private const val END_OF_RECORD: String = "end_of_record"

        fun readLcov(lcovFile: File, localBaseDir: String? = null): LcovCoverageReport {
            val report = LcovCoverageReport()
            var currentFileName: String? = null
            var lineDataList: MutableList<LineHits>? = null
            // BRDA records: line -> blockIndex -> branchIndex -> hits
            var brdaRaw: MutableMap<Int, MutableMap<Int, MutableMap<Int, Int>>>? = null
            lcovFile.forEachLine { line ->
                when {
                    line.startsWith(SOURCE_FILE_PREFIX) -> {
                        currentFileName = line.substring(SOURCE_FILE_PREFIX.length)
                        lineDataList = mutableListOf()
                        brdaRaw = mutableMapOf()
                    }
                    line.startsWith(LINE_HIT_PREFIX) -> {
                        checkNotNull(lineDataList)
                        val values = line
                            .substring(LINE_HIT_PREFIX.length)
                            .split(",")
                            .dropLastWhile { it.isEmpty() }
                        check(values.size == 2)
                        val lineNum = values[0].toIntOrNull() ?: return@forEachLine
                        val hitCount = values[1].toIntOrNull() ?: -1
                        val lineHits = LineHits(lineNum, hitCount)
                        lineDataList?.add(lineHits)
                    }
                    line.startsWith(BRANCH_DATA_PREFIX) -> {
                        // BRDA:line,blockIndex,branchIndex,hits
                        val values = line
                            .substring(BRANCH_DATA_PREFIX.length)
                            .split(",")
                            .dropLastWhile { it.isEmpty() }
                        if (values.size == 4) {
                            val lineNum = values[0].toIntOrNull() ?: return@forEachLine
                            val blockIdx = values[1].toIntOrNull() ?: return@forEachLine
                            val branchIdx = values[2].toIntOrNull() ?: return@forEachLine
                            val hits = values[3].toIntOrNull() ?: 0
                            brdaRaw?.getOrPut(lineNum) { mutableMapOf() }
                                ?.getOrPut(blockIdx) { mutableMapOf() }
                                ?.set(branchIdx, hits)
                        }
                    }
                    END_OF_RECORD == line -> {
                        val branchHits = buildBranchHits(brdaRaw ?: emptyMap())
                        report.mergeFileReport(
                            localBaseDir,
                            checkNotNull(currentFileName),
                            FileReport(checkNotNull(lineDataList), branchHits),
                        )
                        currentFileName = null
                        lineDataList = null
                        brdaRaw = null
                    }
                }
            }
            check(lineDataList == null)
            return report
        }

        private fun buildBranchHits(raw: Map<Int, Map<Int, Map<Int, Int>>>): Map<Int, List<BranchHits>> {
            val result = mutableMapOf<Int, MutableList<BranchHits>>()
            for ((lineNum, blocks) in raw) {
                for ((blockIdx, branches) in blocks) {
                    val trueHits = branches[0] ?: 0
                    val falseHits = branches[1] ?: 0
                    result.getOrPut(lineNum) { mutableListOf() }
                        .add(BranchHits(blockIdx, trueHits, falseHits))
                }
            }
            return result
        }

        fun writeLcov(report: LcovCoverageReport, outputFile: File) {
            PrintWriter(outputFile).use { out ->
                for ((filePath, fileReport) in report.info) {
                    out.print(SOURCE_FILE_PREFIX)
                    out.println(filePath)
                    val lineHits = fileReport.lineHits.sortedBy { it.lineNumber }
                    for (lineHit in lineHits) {
                        out.print(LINE_HIT_PREFIX)
                        out.print(lineHit.lineNumber)
                        out.print(',')
                        out.println(lineHit.hits)
                    }
                    out.println("LF:${lineHits.size}")
                    out.println("LH:${lineHits.count { it.hits > 0 }}")

                    val branchHits = fileReport.branchHits.toSortedMap()
                    if (branchHits.isNotEmpty()) {
                        var branchesFound = 0
                        var branchesHit = 0
                        for ((lineNumber, branches) in branchHits) {
                            for (branch in branches.sortedBy { it.blockIndex }) {
                                out.println("$BRANCH_DATA_PREFIX$lineNumber,${branch.blockIndex},0,${branch.trueHits}")
                                out.println("$BRANCH_DATA_PREFIX$lineNumber,${branch.blockIndex},1,${branch.falseHits}")
                                branchesFound += 2
                                branchesHit += if (branch.trueHits > 0) 1 else 0
                                branchesHit += if (branch.falseHits > 0) 1 else 0
                            }
                        }
                        out.println("BRF:$branchesFound")
                        out.println("BRH:$branchesHit")
                    }
                    out.println(END_OF_RECORD)
                }
            }
        }
    }

    companion object {

        private fun normalizeLineHitsList(lineHits: List<LineHits>): List<LineHits> =
            lineHits.sortedBy { it.lineNumber }.distinctBy { it.lineNumber }

        private fun doMerge(list1: List<LineHits>, list2: List<LineHits>): List<LineHits> = buildList {
            val iter1 = PeekableIteratorWrapper(list1.iterator())
            val iter2 = PeekableIteratorWrapper(list2.iterator())
            while (iter1.hasNext() && iter2.hasNext()) {
                val head1 = iter1.peek()
                val head2 = iter2.peek()
                val next = when {
                    head1.lineNumber < head2.lineNumber ->
                        iter1.next()
                    head1.lineNumber > head2.lineNumber ->
                        iter2.next()
                    else -> {
                        head1.addHits(head2.hits)
                        iter1.next()
                        iter2.next()
                        head1
                    }
                }
                add(next)
            }
            iter1.forEachRemaining(::add)
            iter2.forEachRemaining(::add)
        }

        private fun mergeBranchHits(
            a: Map<Int, List<BranchHits>>,
            b: Map<Int, List<BranchHits>>,
        ): Map<Int, List<BranchHits>> {
            val result = a.toMutableMap()
            for ((line, branches) in b) {
                val existing = result[line]
                if (existing == null) {
                    result[line] = branches
                } else {
                    val merged = existing.associateBy { it.blockIndex }.toMutableMap()
                    for (bh in branches) {
                        val old = merged[bh.blockIndex]
                        if (old == null) {
                            merged[bh.blockIndex] = bh
                        } else {
                            merged[bh.blockIndex] = BranchHits(
                                bh.blockIndex,
                                old.trueHits + bh.trueHits,
                                old.falseHits + bh.falseHits,
                            )
                        }
                    }
                    result[line] = merged.values.toList()
                }
            }
            return result
        }
    }
}
