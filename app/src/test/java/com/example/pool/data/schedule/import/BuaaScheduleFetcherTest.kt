package com.example.pool.data.schedule.import

import org.junit.Assert.assertTrue
import org.junit.Test

class BuaaScheduleFetcherTest {
    @Test
    fun apiCandidates_includesHomeappPathsForBothSchemes() {
        val urls = BuaaScheduleFetcher.apiCandidates("http://byxt.buaa.edu.cn/jwapp/sys/byrhmhsy/index.do")
        assertTrue(urls.any { it.contains("homeapp") && it.startsWith("http://") })
        assertTrue(urls.any { it.contains("homeapp") && it.startsWith("https://") })
    }

    @Test
    fun validateResponse_rejectsHtml() {
        val error = runCatching {
            BuaaScheduleFetcher.validateResponse("<html>login</html>", week = 1)
        }.exceptionOrNull()
        assertTrue(error?.message?.contains("JSON") == true)
    }

    @Test
    fun validateResponse_acceptsSuccessJson() {
        BuaaScheduleFetcher.validateResponse("""{"code":"0","datas":{}}""", week = 1)
    }
}
