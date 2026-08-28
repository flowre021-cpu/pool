package com.example.pool.data.schedule.import

object BuaaScheduleImportConfig {
    const val SCHOOL_NAME = "北京航空航天大学"
    const val LOGIN_URL = "https://byxt.buaa.edu.cn/jwapp/sys/jwsybuaa/login/index.html"
    /** 北航本研课表 API（与当前 WebView 页面路径无关）。 */
    const val SCHEDULE_API_URL =
        "https://byxt.buaa.edu.cn/jwapp/sys/homeapp/api/home/student/getMyScheduleDetail.do"
    const val DEFAULT_MAX_WEEKS = 18
    const val FETCH_TIMEOUT_MS = 20_000
    const val FETCH_WATCHDOG_MS = 180_000L
}
