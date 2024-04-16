package com.sopt.smeem.presentation.mypage.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sopt.smeem.R
import com.sopt.smeem.domain.model.mypage.MySmeem
import com.sopt.smeem.presentation.home.calendar.ui.theme.Typography
import com.sopt.smeem.presentation.home.calendar.ui.theme.gray100
import com.sopt.smeem.util.VerticalSpacer

@Composable
fun MySmeem(
    modifier: Modifier = Modifier,
    mySmeem: MySmeem
) {
    SmeemContents(
        modifier = modifier.padding(vertical = 18.dp),
        title = stringResource(R.string.my_smeem)
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors().copy(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, gray100)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MySmeemContent(
                    count = mySmeem.visitDays,
                    title = "방문일"
                )
                MySmeemContent(
                    count = mySmeem.diaryCount,
                    title = "총 일기"
                )
                MySmeemContent(
                    count = mySmeem.diaryComboCount,
                    title = "연속일기"
                )
                MySmeemContent(
                    count = mySmeem.badgeCount,
                    title = "배지"
                )
            }

        }
    }
}

@Composable
fun MySmeemContent(
    count: Int,
    title: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = count.toString(), style = Typography.titleMedium)
        VerticalSpacer(height = 6.dp)
        Text(text = title, style = Typography.labelLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun MySmeemContentPreview() {
    MySmeemContent(
        count = 23,
        title = "방문일"
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 800)
@Composable
fun MySmeemPreview() {
    MySmeem(
        mySmeem = MySmeem(
            visitDays = 23,
            diaryCount = 19,
            diaryComboCount = 3,
            badgeCount = 10
        )
    )
}
