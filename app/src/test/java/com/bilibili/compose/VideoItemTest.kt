package com.bilibili.compose

import com.bilibili.compose.data.model.BiliResponse
import com.bilibili.compose.data.model.RcmdReason
import com.bilibili.compose.data.model.RcmdReasonAdapter
import com.bilibili.compose.data.model.VideoItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VideoItemTest {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RcmdReason::class.java, RcmdReasonAdapter())
        .create()

    @Test
    fun testDeserializeRelatedVideoWithEmptyStringRcmdReason() {
        val json = """
            {
                "aid": 114968892542101,
                "bvid": "BV1zVt3znEc1",
                "cid": 33169936100,
                "title": "视频已失效",
                "pic": "http://i0.hdslb.com/bfs/archive/0e65b48611113f0ffd975943f669df2ec6c25e2a.jpg",
                "desc": "-",
                "duration": 17,
                "pubdate": 1754286300,
                "rcmd_reason": ""
            }
        """.trimIndent()

        val video = gson.fromJson(json, VideoItem::class.java)
        assertNotNull(video)
        assertEquals("BV1zVt3znEc1", video.bvid)
        assertNull(video.rcmdReason)
    }

    @Test
    fun testDeserializeVideoWithObjectRcmdReason() {
        val json = """
            {
                "aid": 12345,
                "bvid": "BV1test",
                "title": "热门视频",
                "rcmd_reason": {
                    "content": "6万点赞",
                    "corner_mark": 0
                }
            }
        """.trimIndent()

        val video = gson.fromJson(json, VideoItem::class.java)
        assertNotNull(video)
        assertEquals("6万点赞", video.rcmdReason?.content)
    }

    @Test
    fun testDeserializeVideoWithStringRcmdReason() {
        val json = """
            {
                "aid": 12345,
                "bvid": "BV1test",
                "title": "推荐视频",
                "rcmd_reason": "百万播放"
            }
        """.trimIndent()

        val video = gson.fromJson(json, VideoItem::class.java)
        assertNotNull(video)
        assertEquals("百万播放", video.rcmdReason?.content)
    }

    @Test
    fun testDeserializeRelatedVideoListResponse() {
        val json = """
            {
                "code": 0,
                "message": "0",
                "ttl": 1,
                "data": [
                    {
                        "aid": 114968892542101,
                        "bvid": "BV1zVt3znEc1",
                        "cid": 33169936100,
                        "title": "视频已失效",
                        "pic": "http://i0.hdslb.com/bfs/archive/0e65b48611113f0ffd975943f669df2ec6c25e2a.jpg",
                        "desc": "-",
                        "duration": 17,
                        "rcmd_reason": "",
                        "stat": {
                            "aid": 114968892542101,
                            "view": 659315,
                            "danmaku": 1696
                        },
                        "owner": {
                            "mid": 2026563810,
                            "name": "McIDE"
                        }
                    }
                ]
            }
        """.trimIndent()

        val type = object : TypeToken<BiliResponse<List<VideoItem>>>() {}.type
        val response: BiliResponse<List<VideoItem>> = gson.fromJson(json, type)
        assertNotNull(response)
        assertEquals(0, response.code)
        assertEquals(1, response.data?.size)
        assertEquals("BV1zVt3znEc1", response.data?.first()?.bvid)
    }
}
