package com.example.dam_front.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.example.dam_front.ui.theme.DAM_frontTheme
import com.example.dam_front.ui.theme.*
import com.example.dam_front.R

@Composable
fun TargetIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2
            
            // Draw concentric circles
            drawCircle(
                color = tint,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = tint,
                radius = radius * 0.75f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = tint,
                radius = radius * 0.5f,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = tint,
                radius = radius * 0.25f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun CardIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val cardWidth = 12.dp.toPx()
            val cardHeight = 10.dp.toPx()
            
            // Rectangle centered
            val left = centerX - cardWidth / 2
            val top = centerY - cardHeight / 2
            val right = centerX + cardWidth / 2
            val bottom = centerY + cardHeight / 2
            
            val path = Path().apply {
                moveTo(left, top)
                lineTo(right, top)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            drawPath(path, color = tint, style = Stroke(width = 2.dp.toPx()))
            
            // Horizontal line in the middle
            drawLine(
                color = tint,
                start = Offset(left, centerY),
                end = Offset(right, centerY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun TrophyIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.EmojiEvents,
        contentDescription = "Trophy",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun PeopleIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.People,
        contentDescription = "People",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun NotificationIcon(modifier: Modifier = Modifier, tint: Color = Color.White, hasNotification: Boolean = true) {
    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            modifier = Modifier.size(24.dp),
            tint = tint
        )
        if (hasNotification) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(x = 12.dp, y = (-2).dp)
                    .align(Alignment.TopEnd)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFFFF5722))
                }
            }
        }
    }
}

@Composable
fun SettingsIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun HomeIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Home,
        contentDescription = "Home",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun ProgressIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.TrendingUp,
        contentDescription = "Progress",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun LogoutIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.ExitToApp,
        contentDescription = "Logout",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun MenuIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Menu",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun ListIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.List,
        contentDescription = "List",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun CalendarIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.DateRange,
        contentDescription = "Calendar",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.AccessTime,
        contentDescription = "Clock",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun CurrencyIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.AttachMoney,
        contentDescription = "Currency",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun ChevronRightIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "Chevron Right",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun AddIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Add",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun ArrowBackIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.ArrowBack,
        contentDescription = "Back",
        modifier = modifier.size(24.dp),
        tint = tint
    )
}

@Composable
fun TimerIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Timer,
        contentDescription = "Timer",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun PersonIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Person",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun DescriptionIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Description,
        contentDescription = "Description",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun EditIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun DeleteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun SearchIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun SortIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Sort,
        contentDescription = "Sort",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun ArrowDropDownIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = "Arrow Drop Down",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun CheckIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Check",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun EmailIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Email,
        contentDescription = "Email",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}

@Composable
fun PhoneIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.Phone,
        contentDescription = "Phone",
        modifier = modifier.size(20.dp),
        tint = tint
    )
}


@Preview
@Composable
fun TargetIconPreview() {
    DAM_frontTheme {
        TargetIcon(tint = Color.Black)
    }
}

@Preview
@Composable
fun CardIconPreview() {
    DAM_frontTheme {
        CardIcon(tint = Color.Black)
    }
}

@Preview
@Composable
fun TrophyIconPreview() {
    DAM_frontTheme {
        TrophyIcon(tint = Color.Black)
    }
}

@Preview
@Composable
fun PeopleIconPreview() {
    DAM_frontTheme {
        PeopleIcon(tint = Color.Black)
    }
}

@Preview
@Composable
fun NotificationIconPreview() {
    DAM_frontTheme {
        Row {
            NotificationIcon(tint = Color.Black, hasNotification = true)
            Spacer(modifier = Modifier.width(16.dp))
            NotificationIcon(tint = Color.Black, hasNotification = false)
        }
    }
}

@Composable
fun SportyKidsLogo(
    modifier: Modifier = Modifier,
    logoSize: androidx.compose.ui.unit.Dp = 120.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Image (using logo2.png with background)
        Image(
            painter = painterResource(id = R.drawable.logo2),
            contentDescription = "Sporty Kids Logo",
            modifier = Modifier.size(logoSize),
            contentScale = ContentScale.Fit
        )
    }
}

@Preview
@Composable
fun SettingsIconPreview() {
    DAM_frontTheme {
        SettingsIcon(tint = Color.Black)
    }
}

@Preview
@Composable
fun SportyKidsLogoPreview() {
    DAM_frontTheme {
        SportyKidsLogo()
    }
}

