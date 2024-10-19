package com.example.watch_schoolmeal.presentation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Bundle
import android.util.Log
import android.view.SubMenu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {

    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val day = dateFormat.format(Date())

    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    calendar.time = Date()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val nextDay = dateFormat.format(calendar.time)

    var today by remember { mutableStateOf(arrayOf("")) }
    var tmr by remember { mutableStateOf(arrayOf("")) }
    var todayB by remember { mutableStateOf("") }
    var todayL by remember { mutableStateOf("") }
    var todayD by remember { mutableStateOf("") }
    var tmrB by remember { mutableStateOf("") }
    var tmrL by remember { mutableStateOf("") }
    var tmrD by remember { mutableStateOf("") }

    var mode by remember { mutableIntStateOf(when (currentHour) {
        1, 2, 3, 4, 5, 6, 7, 8 -> 1
        9, 10, 11, 12, 13 -> 2
        14, 15, 16, 17 -> 3
        else -> 4
    }) }

    fun modeToTitle(mode: Int): String {
        if (mode == 1) return "오늘의 아침"
        else if (mode == 2) return "오늘의 점심"
        else if (mode == 3) return "오늘의 저녁"
        else if (mode == 4) return "내일의 아침"
        else if (mode == 5) return "내일의 점심"
        else if (mode == 6) return "내일의 저녁"
        else return "일시적 오류가 발생했어요."
    }

    fun modeToContent(mode: Int): String {
        if (mode == 1) return todayB
        else if (mode == 2) return todayL
        else if (mode == 3) return todayD
        else if (mode == 4) return tmrB
        else if (mode == 5) return tmrL
        else if (mode == 6) return tmrD
        else return "개발자에게 문의하세요."
    }

    LaunchedEffect(day) {
        today = GetSchoolMeal(day)
        tmr = GetSchoolMeal(nextDay)
        todayB = today[0]
        todayL = today[1]
        todayD = today[2]
        tmrB = tmr[0]
        tmrL = tmr[1]
        tmrD = tmr[2]
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .onRotaryScrollEvent { event ->
                val delta = event.verticalScrollPixels
                if (delta > 0) {
                    if (mode < 3) mode += 3
                } else {
                    if (mode > 3) mode -= 3
                }
                true
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode != 1) {Text(modifier = Modifier.clickable { mode -= 1 }, text = " ←", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 30.sp)}
            else {Text(text = " ←", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 30.sp)}

            Spacer(modifier = Modifier.weight(1f))

            Column (
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    text = modeToTitle(mode)
                )

                Text(
                    modifier = Modifier,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFF79F1C),
                    fontSize = 10.sp,
                    text = if (mode <= 3) {day.substring(0, 4) + "." + day.substring(4, 6) + "." + day.substring(6, 8)} else {nextDay.substring(0, 4) + "." + nextDay.substring(4, 6) + "." + nextDay.substring(6, 8)}
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    modifier = Modifier
                        .verticalScroll(scrollState),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 12.sp,
                    text = modeToContent(mode)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (mode != 6) {Text(modifier = Modifier.clickable { mode += 1 }, text = "→ ", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 30.sp)}
            else {Text(text = "→ ", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 30.sp)}
        }
    }

}

suspend fun GetSchoolMeal(day: String): Array<String> {
    return withContext(Dispatchers.IO) {
        try {
            // Get the school meal data from the API
            var breakfast = ""
            var lunch = ""
            var dinner = ""
            val studentMealData = Jsoup.connect("https://food.podac.poapper.com/v2/menus/period/$day/$day")
                .ignoreContentType(true)
                .get()
                .text()

            val mealNames = mutableMapOf<String, String>()
            val schoolMealJSON = JSONObject(studentMealData).getJSONObject(day)

            val mealTypes = listOf("BREAKFAST_A", "BREAKFAST_B", "LUNCH", "DINNER", "STAFF", "INTERNATIONAL")

            for (mealType in mealTypes) {
                // Skip if the meal type is not in the JSON
                if (!schoolMealJSON.has(mealType)) {
                    mealNames[mealType] = "없음"
                    continue
                }
                val foods = schoolMealJSON.getJSONObject(mealType).getJSONArray("foods")
                val mealNameBuilder = StringBuilder()

                for (i in 0 until foods.length()) {
                    val food = foods.getJSONObject(i)
                    mealNameBuilder.append(food.getString("name_kor"))
                    if (i < foods.length() - 1) {
                        mealNameBuilder.append("\n")
                    }
                }

                mealNames[mealType] = mealNameBuilder.toString()
            }

            breakfast = mealNames["BREAKFAST_A"] + "\n\n[간편식]\n" + mealNames["BREAKFAST_B"]
            lunch = mealNames["LUNCH"].toString()
            dinner = mealNames["DINNER"].toString()

            // Get the RIST meal data from the API
            val RISTMenuData = Jsoup.connect("https://puls2.pulmuone.com/src/sql/menu/today_sql.php")
                .data("requestId", "search_schMenu")
                .data("requestParam",
                        "{" +
                            "\"srchOperCd\":\"O000002\"," +
                            "\"srchAssignCd\":\"S000591\"," +
                            "\"srchCurDay\":\"$day\"" +
                        "}"
                )
                .ignoreContentType(true)
                .post()
                .text()
            val RISTMenuJSON = JSONObject(RISTMenuData)
            Log.d("SchoolMeal", RISTMenuJSON.toString())

            if(RISTMenuJSON.getString("closeGb") != "1") {
                val RISTMenus = RISTMenuJSON.getJSONArray("data")
                val RISTTimeCode = arrayOf("010", "020", "030") // Breakfast, Lunch, Dinner
                val RISTMealNames = mutableMapOf<String, String>()

                for (i in 0 until RISTMenus.length()) {
                    val RISTMenuInfo = RISTMenus.getJSONArray(i)
                    val mealType = arrayOf("breakfast", "lunch", "dinner")[
                        RISTTimeCode.indexOf(RISTMenuInfo[0].toString())
                    ]

                    val MenuName = RISTMenuInfo[6].toString()
                    val MainMenu = RISTMenuInfo[1].toString()
                    var SubMenu = RISTMenuInfo[5].toString()

                    if ("샐러드" in MenuName.toString()) SubMenu = ""

                    val MenuString = ("$MainMenu $SubMenu")
                        .trim()
                        .replace("  ", " ")
                        .replace(" ", "\n")
                        .replace("＆", "\n")
                        .trim()

                    if (RISTMealNames.containsKey(mealType)) {
                        RISTMealNames[mealType] += "\n[RIST $MenuName]\n$MenuString\n"
                    } else {
                        RISTMealNames[mealType] = "\n[RIST $MenuName]\n$MenuString\n"
                    }
                }

                if (RISTMealNames.containsKey("breakfast")) {
                    breakfast += "\n" + RISTMealNames["breakfast"].toString()
                }
                if (RISTMealNames.containsKey("lunch")) {
                    lunch += "\n" + RISTMealNames["lunch"].toString()
                }
                if (RISTMealNames.containsKey("dinner")) {
                    dinner += "\n" + RISTMealNames["dinner"].toString()
                }
            }
            arrayOf(breakfast, lunch, dinner)
        } catch (e: Exception) {
            Log.e("SchoolMeal", "Error: $e")
            arrayOf("", "", "")
        }
    }
}
