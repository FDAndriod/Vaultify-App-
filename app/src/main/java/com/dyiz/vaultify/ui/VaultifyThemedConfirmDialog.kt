package com.dyiz.vaultify.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dyiz.vaultify.R

enum class VaultifyConfirmPrimaryStyle {
    /** Teal primary — e.g. unlock / positive bulk action */
    Positive,
    /** Red primary — destructive bulk action */
    Destructive
}

/**
 * Full-screen scrim + gradient card matching [com.dyiz.vaultify.MainActivity] exit dialog styling.
 */
@Composable
fun VaultifyThemedConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String,
    iconResId: Int,
    iconContentDescription: String,
    primaryStyle: VaultifyConfirmPrimaryStyle,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val accent = colorResource(R.color.splashhalftextColor)
    val destructive = colorResource(R.color.wronganswercolor)
    val ringColor = when (primaryStyle) {
        VaultifyConfirmPrimaryStyle.Positive -> accent
        VaultifyConfirmPrimaryStyle.Destructive -> destructive
    }
    val confirmContainerColor = when (primaryStyle) {
        VaultifyConfirmPrimaryStyle.Positive -> colorResource(R.color.yesbuttoncontainercolor)
        VaultifyConfirmPrimaryStyle.Destructive -> destructive
    }
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            colorResource(R.color.splashscreenColor1),
            colorResource(R.color.splashscreenColor2)
        )
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
                    .background(cardGradient, RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(ringColor.copy(alpha = 0.16f), CircleShape)
                            .border(1.dp, ringColor.copy(alpha = 0.38f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = iconContentDescription,
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily(Font(R.font.roboto_medium)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = colorResource(R.color.splashsubtextcolor),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, colorResource(R.color.nobuttonbordercolor)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dismissButtonText,
                                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = confirmContainerColor,
                                contentColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmButtonText,
                                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
