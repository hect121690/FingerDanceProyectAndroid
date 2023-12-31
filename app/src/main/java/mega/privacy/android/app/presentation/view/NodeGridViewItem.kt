package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.photos.albums.view.MiddleEllipsisText
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Grid view item for file/folder info
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NodeGridViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem,
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
) {
    if (nodeUIItem.node is FolderNode) {
        ConstraintLayout(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (nodeUIItem.isInvisible) 0f else 1f)
                .border(
                    width = 1.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_012_white_012,
                    shape = RoundedCornerShape(5.dp)
                )
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem) },
                    onLongClick = { onLongClick(nodeUIItem) }
                )
                .padding(start = 16.dp, end = 8.dp),
        ) {
            val (menuImage, txtTitle, thumbImage) = createRefs()
            Image(
                painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                contentDescription = "3 dots",
                modifier = Modifier
                    .clickable { onMenuClick.invoke(nodeUIItem) }
                    .constrainAs(menuImage) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
            )
            Image(
                painter = if (nodeUIItem.isSelected) painterResource(id = R.drawable.ic_select_folder) else getPainter(
                    nodeUIItem = nodeUIItem.node
                ),
                contentDescription = "Folder",
                modifier = Modifier
                    .height(24.dp)
                    .width(24.dp)
                    .constrainAs(thumbImage) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
            )
            MiddleEllipsisText(
                text = nodeUIItem.name,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .constrainAs(txtTitle) {
                        end.linkTo(menuImage.start)
                        start.linkTo(thumbImage.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    },
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary
            )

        }
    } else if (nodeUIItem.node is FileNode) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_012_white_012,
                    shape = RoundedCornerShape(5.dp)
                )
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem) },
                    onLongClick = { onLongClick(nodeUIItem) }
                )
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                Image(
                    painter = rememberAsyncImagePainter(model = nodeUIItem.node.thumbnailPath),
                    contentDescription = "File",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .height(172.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                )
                if (nodeUIItem.isSelected) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_select_folder),
                        contentDescription = "checked",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            ConstraintLayout(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 16.dp, top = 16.dp)
                    .fillMaxWidth()
            ) {
                val (menuImage, txtTitle) = createRefs()
                Image(
                    painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                    contentDescription = "3 dots",
                    modifier = Modifier
                        .clickable { onMenuClick.invoke(nodeUIItem) }
                        .constrainAs(menuImage) {
                            end.linkTo(parent.end)
                        }
                )
                MiddleEllipsisText(
                    text = nodeUIItem.name,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .constrainAs(txtTitle) {
                            end.linkTo(menuImage.start)
                            start.linkTo(parent.start)
                        },
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary
                )
            }
        }
    }
}


